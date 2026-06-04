package com.example.appopencounter.domain.usecase

import com.example.appopencounter.domain.repository.AppInfoResolver
import com.example.appopencounter.domain.repository.UsageRepository
import com.example.appopencounter.domain.repository.UsageSettingsRepository
import com.example.appopencounter.usage.UsageAccessChecker
import com.example.appopencounter.usage.UsageEventAggregator
import com.example.appopencounter.usage.UsageEventsSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CollectUsageUseCase(
    private val usageSettingsRepository: UsageSettingsRepository,
    private val usageAccessChecker: UsageAccessChecker,
    private val usageEventsSource: UsageEventsSource,
    private val usageEventAggregator: UsageEventAggregator,
    private val usageRepository: UsageRepository,
    private val appInfoResolver: AppInfoResolver,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend operator fun invoke(): CollectUsageResult {
        val endTimeMillis = clock.millis()
        val lastCollectTimeMillis = usageSettingsRepository.getLastCollectTimeMillis()
        val startTimeMillis = lastCollectTimeMillis ?: startOfDayMillis(endTimeMillis)

        return invoke(
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
        )
    }

    suspend operator fun invoke(
        startTimeMillis: Long,
        endTimeMillis: Long,
    ): CollectUsageResult {
        if (!usageSettingsRepository.isUsageCollectionEnabled()) {
            return CollectUsageResult.CollectionDisabled
        }
        if (!usageAccessChecker.hasUsageAccess()) {
            return CollectUsageResult.PermissionDenied
        }
        if (endTimeMillis <= startTimeMillis) {
            return CollectUsageResult.Collected(appOpenCount = 0)
        }

        return try {
            val rawEvents = usageEventsSource.collectUsageEvents(
                fromMillis = startTimeMillis,
                toMillis = endTimeMillis,
            )
            val dailyUsage = usageEventAggregator.aggregateDailyUsage(
                events = rawEvents,
                rangeEndTimeMillis = endTimeMillis,
                updatedAt = clock.millis(),
            )

            dailyUsage
                .map { usage -> usage.packageName }
                .distinct()
                .forEach { packageName ->
                    appInfoResolver.resolve(packageName)?.let { appInfo ->
                        usageRepository.upsertAppInfo(appInfo)
                    }
                }

            dailyUsage.forEach { usage ->
                usageRepository.upsertDailyUsage(usage)
            }
            usageSettingsRepository.setLastCollectTimeMillis(endTimeMillis)

            CollectUsageResult.Collected(
                appOpenCount = dailyUsage.sumOf { usage -> usage.openCount },
            )
        } catch (_: Exception) {
            CollectUsageResult.Failed
        }
    }

    private fun startOfDayMillis(timestampMillis: Long): Long {
        return dateFor(timestampMillis)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun datesBetween(
        startTimeMillis: Long,
        endTimeMillis: Long,
    ): List<String> {
        val startDate = dateFor(startTimeMillis)
        val endDate = dateFor(endTimeMillis)
        val dates = mutableListOf<String>()
        var cursor: LocalDate = startDate

        while (!cursor.isAfter(endDate)) {
            dates += cursor.toString()
            cursor = cursor.plusDays(1)
        }

        return dates
    }

    private fun dateFor(timestampMillis: Long): LocalDate {
        return Instant
            .ofEpochMilli(timestampMillis)
            .atZone(zoneId)
            .toLocalDate()
    }
}

sealed interface CollectUsageResult {
    data class Collected(val appOpenCount: Int) : CollectUsageResult

    data object CollectionDisabled : CollectUsageResult

    data object PermissionDenied : CollectUsageResult

    data object Failed : CollectUsageResult
}
