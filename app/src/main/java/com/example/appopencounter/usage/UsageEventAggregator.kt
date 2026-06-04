package com.example.appopencounter.usage

import com.example.appopencounter.domain.model.DailyUsageUpsert
import java.time.Instant
import java.time.ZoneId
import kotlin.math.min

class UsageEventAggregator(
    private val currentPackageName: String,
    private val minimumForegroundDurationMs: Long = MINIMUM_FOREGROUND_DURATION_MS,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun aggregate(
        events: List<UsageEventRecord>,
        rangeEndTimeMillis: Long,
    ): List<AppOpenEvent> {
        val activeSessions = linkedMapOf<String, Long>()
        val appOpenEvents = mutableListOf<AppOpenEvent>()

        events
            .sortedBy { event -> event.timestamp }
            .forEach { event ->
                when (event.kind) {
                    UsageEventKind.FOREGROUND -> {
                        closeOtherActiveSessions(
                            activeSessions = activeSessions,
                            foregroundPackageName = event.packageName,
                            backgroundTime = event.timestamp,
                            appOpenEvents = appOpenEvents,
                        )

                        if (
                            event.packageName != currentPackageName &&
                            activeSessions[event.packageName] == null
                        ) {
                            activeSessions[event.packageName] = event.timestamp
                        }
                    }

                    UsageEventKind.BACKGROUND -> {
                        val foregroundTime = activeSessions.remove(event.packageName)
                        if (foregroundTime != null) {
                            appOpenEvents += buildAppOpenEvents(
                                packageName = event.packageName,
                                foregroundTime = foregroundTime,
                                backgroundTime = event.timestamp,
                                rangeEndTimeMillis = rangeEndTimeMillis,
                            )
                        }
                    }
                }
            }

        activeSessions.forEach { (packageName, foregroundTime) ->
            appOpenEvents += buildAppOpenEvents(
                packageName = packageName,
                foregroundTime = foregroundTime,
                backgroundTime = null,
                rangeEndTimeMillis = rangeEndTimeMillis,
            )
        }

        return appOpenEvents
    }

    fun aggregateDailyUsage(
        events: List<UsageEventRecord>,
        rangeEndTimeMillis: Long,
        updatedAt: Long,
    ): List<DailyUsageUpsert> {
        return aggregate(
            events = events,
            rangeEndTimeMillis = rangeEndTimeMillis,
        )
            .groupBy { event -> event.packageName to event.date }
            .map { (key, eventsForDay) ->
                DailyUsageUpsert(
                    packageName = key.first,
                    date = key.second,
                    openCount = eventsForDay.size,
                    firstOpenTime = eventsForDay.minOfOrNull { event -> event.foregroundTime },
                    lastOpenTime = eventsForDay.maxOfOrNull { event -> event.foregroundTime },
                    updatedAt = updatedAt,
                )
            }
    }

    private fun closeOtherActiveSessions(
        activeSessions: MutableMap<String, Long>,
        foregroundPackageName: String,
        backgroundTime: Long,
        appOpenEvents: MutableList<AppOpenEvent>,
    ) {
        activeSessions
            .filterKeys { packageName -> packageName != foregroundPackageName }
            .forEach { (packageName, foregroundTime) ->
                activeSessions.remove(packageName)
                appOpenEvents += buildAppOpenEvents(
                    packageName = packageName,
                    foregroundTime = foregroundTime,
                    backgroundTime = backgroundTime,
                    rangeEndTimeMillis = backgroundTime,
                )
            }
    }

    private fun buildAppOpenEvents(
        packageName: String,
        foregroundTime: Long,
        backgroundTime: Long?,
        rangeEndTimeMillis: Long,
    ): List<AppOpenEvent> {
        if (packageName == currentPackageName) {
            return emptyList()
        }

        val sessionEndTime = backgroundTime ?: rangeEndTimeMillis
        if (sessionEndTime <= foregroundTime) {
            return emptyList()
        }

        val events = mutableListOf<AppOpenEvent>()
        var segmentStart = foregroundTime

        while (segmentStart < sessionEndTime) {
            val segmentDate = dateFor(segmentStart)
            val nextDayStart = segmentDate
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
            val segmentEnd = min(sessionEndTime, nextDayStart)
            val durationMs = segmentEnd - segmentStart

            if (durationMs > minimumForegroundDurationMs) {
                events += AppOpenEvent(
                    packageName = packageName,
                    foregroundTime = segmentStart,
                    backgroundTime = if (segmentEnd == sessionEndTime) backgroundTime else segmentEnd,
                    durationMs = durationMs,
                    date = segmentDate.toString(),
                )
            }

            segmentStart = segmentEnd
        }

        return events
    }

    private fun dateFor(timestampMillis: Long) = Instant
        .ofEpochMilli(timestampMillis)
        .atZone(zoneId)
        .toLocalDate()

    companion object {
        const val MINIMUM_FOREGROUND_DURATION_MS = 2_000L
    }
}
