package com.example.appopencounter.domain.usecase

import com.example.appopencounter.domain.model.AppInfo
import com.example.appopencounter.domain.model.DailyAppUsage
import com.example.appopencounter.domain.model.DailyUsageUpsert
import com.example.appopencounter.domain.repository.AppInfoResolver
import com.example.appopencounter.domain.repository.UsageRepository
import com.example.appopencounter.domain.repository.UsageSettingsRepository
import com.example.appopencounter.usage.UsageAccessChecker
import com.example.appopencounter.usage.UsageEventAggregator
import com.example.appopencounter.usage.UsageEventKind
import com.example.appopencounter.usage.UsageEventRecord
import com.example.appopencounter.usage.UsageEventsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class CollectUsageUseCaseTest {
    private val zoneId = ZoneId.of("UTC")
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-06-04T12:00:00Z"),
        zoneId,
    )

    @Test
    fun invoke_doesNotCollectOrWriteWhenCollectionSwitchIsDisabled() = runBlocking {
        val repository = FakeUsageRepository()
        val settingsRepository = FakeUsageSettingsRepository(enabled = false)
        val source = FakeUsageEventsSource(
            events = listOf(
                UsageEventRecord("com.example.mail", 0L, UsageEventKind.FOREGROUND),
            ),
        )
        val useCase = createUseCase(
            settingsRepository = settingsRepository,
            accessChecker = FakeUsageAccessChecker(hasAccess = true),
            eventsSource = source,
            repository = repository,
        )

        val result = useCase(0L, 5_000L)

        assertEquals(CollectUsageResult.CollectionDisabled, result)
        assertEquals(0, source.collectCallCount)
        assertTrue(repository.upserts.isEmpty())
        assertNull(settingsRepository.lastCollectTime)
    }

    @Test
    fun invoke_doesNotCollectOrWriteWhenUsageAccessIsDenied() = runBlocking {
        val repository = FakeUsageRepository()
        val settingsRepository = FakeUsageSettingsRepository(enabled = true)
        val source = FakeUsageEventsSource(
            events = listOf(
                UsageEventRecord("com.example.mail", 0L, UsageEventKind.FOREGROUND),
            ),
        )
        val useCase = createUseCase(
            settingsRepository = settingsRepository,
            accessChecker = FakeUsageAccessChecker(hasAccess = false),
            eventsSource = source,
            repository = repository,
        )

        val result = useCase(0L, 5_000L)

        assertEquals(CollectUsageResult.PermissionDenied, result)
        assertEquals(0, source.collectCallCount)
        assertTrue(repository.upserts.isEmpty())
        assertNull(settingsRepository.lastCollectTime)
    }

    @Test
    fun invoke_collectsAggregatesAndWritesDailyUsageWhenEnabledAndAuthorized() = runBlocking {
        val repository = FakeUsageRepository()
        val source = FakeUsageEventsSource(
            events = listOf(
                UsageEventRecord("com.example.mail", 0L, UsageEventKind.FOREGROUND),
                UsageEventRecord("com.example.mail", 3_000L, UsageEventKind.BACKGROUND),
            ),
        )
        val useCase = createUseCase(
            settingsRepository = FakeUsageSettingsRepository(enabled = true),
            accessChecker = FakeUsageAccessChecker(hasAccess = true),
            eventsSource = source,
            repository = repository,
        )

        val result = useCase(0L, 5_000L)

        assertEquals(CollectUsageResult.Collected(appOpenCount = 1), result)
        assertEquals(1, source.collectCallCount)
        assertEquals(1, repository.upserts.size)
        assertEquals("com.example.mail", repository.upserts.single().packageName)
        assertEquals(1, repository.upserts.single().openCount)
        assertEquals("Mail", repository.appInfos.single().appName)
        assertEquals(5_000L, settingsRepository.lastCollectTime)
    }

    @Test
    fun invokeWithoutExplicitRange_usesStartOfTodayWhenLastCollectTimeIsEmpty() = runBlocking {
        val repository = FakeUsageRepository()
        val source = FakeUsageEventsSource(
            events = listOf(
                UsageEventRecord(
                    "com.example.mail",
                    Instant.parse("2026-06-04T00:00:00Z").toEpochMilli(),
                    UsageEventKind.FOREGROUND,
                ),
                UsageEventRecord(
                    "com.example.mail",
                    Instant.parse("2026-06-04T00:00:03Z").toEpochMilli(),
                    UsageEventKind.BACKGROUND,
                ),
            ),
        )
        val useCase = createUseCase(
            settingsRepository = FakeUsageSettingsRepository(enabled = true),
            accessChecker = FakeUsageAccessChecker(hasAccess = true),
            eventsSource = source,
            repository = repository,
        )

        useCase()

        assertEquals(Instant.parse("2026-06-04T00:00:00Z").toEpochMilli(), source.lastFromMillis)
        assertEquals(fixedClock.millis(), source.lastToMillis)
    }

    @Test
    fun invokeWithoutExplicitRange_usesLastCollectTimeWhenPresent() = runBlocking {
        val settingsRepository = FakeUsageSettingsRepository(
            enabled = true,
            initialLastCollectTime = 1_000L,
        )
        val source = FakeUsageEventsSource(events = emptyList())
        val useCase = createUseCase(
            settingsRepository = settingsRepository,
            accessChecker = FakeUsageAccessChecker(hasAccess = true),
            eventsSource = source,
            repository = FakeUsageRepository(),
        )

        useCase()

        assertEquals(1_000L, source.lastFromMillis)
        assertEquals(fixedClock.millis(), source.lastToMillis)
    }

    @Test
    fun invoke_returnsFailedAndDoesNotUpdateLastCollectTimeWhenCollectionThrows() = runBlocking {
        val settingsRepository = FakeUsageSettingsRepository(enabled = true)
        val repository = FakeUsageRepository()
        val useCase = createUseCase(
            settingsRepository = settingsRepository,
            accessChecker = FakeUsageAccessChecker(hasAccess = true),
            eventsSource = ThrowingUsageEventsSource(),
            repository = repository,
        )

        val result = useCase(0L, 5_000L)

        assertEquals(CollectUsageResult.Failed, result)
        assertTrue(repository.upserts.isEmpty())
        assertNull(settingsRepository.lastCollectTime)
    }

    private fun createUseCase(
        settingsRepository: UsageSettingsRepository,
        accessChecker: UsageAccessChecker,
        eventsSource: UsageEventsSource,
        repository: UsageRepository,
    ): CollectUsageUseCase {
        return CollectUsageUseCase(
            usageSettingsRepository = settingsRepository,
            usageAccessChecker = accessChecker,
            usageEventsSource = eventsSource,
            usageEventAggregator = UsageEventAggregator(
                currentPackageName = "com.example.appopencounter",
                zoneId = zoneId,
            ),
            usageRepository = repository,
            appInfoResolver = FakeAppInfoResolver(),
            clock = fixedClock,
        )
    }

    private class FakeUsageSettingsRepository(
        private val enabled: Boolean,
        initialLastCollectTime: Long? = null,
    ) : UsageSettingsRepository {
        override val usageCollectionEnabled: Flow<Boolean> = MutableStateFlow(enabled)
        override val lastCollectTimeMillis: Flow<Long?> = MutableStateFlow(initialLastCollectTime)

        var lastCollectTime: Long? = initialLastCollectTime
            private set

        override suspend fun isUsageCollectionEnabled() = enabled

        override suspend fun setUsageCollectionEnabled(enabled: Boolean) = Unit

        override suspend fun getLastCollectTimeMillis() = lastCollectTime

        override suspend fun setLastCollectTimeMillis(timestampMillis: Long) {
            lastCollectTime = timestampMillis
        }

        override suspend fun clearLastCollectTimeMillis() {
            lastCollectTime = null
        }
    }

    private class FakeUsageAccessChecker(
        private val hasAccess: Boolean,
    ) : UsageAccessChecker {
        override fun hasUsageAccess() = hasAccess
    }

    private class FakeUsageEventsSource(
        private val events: List<UsageEventRecord>,
    ) : UsageEventsSource {
        var collectCallCount = 0
            private set
        var lastFromMillis: Long? = null
            private set
        var lastToMillis: Long? = null
            private set

        override fun collectUsageEvents(
            fromMillis: Long,
            toMillis: Long,
        ): List<UsageEventRecord> {
            collectCallCount += 1
            lastFromMillis = fromMillis
            lastToMillis = toMillis
            return events
        }
    }

    private class ThrowingUsageEventsSource : UsageEventsSource {
        override fun collectUsageEvents(
            fromMillis: Long,
            toMillis: Long,
        ): List<UsageEventRecord> {
            error("Collector failed.")
        }
    }

    private class FakeUsageRepository : UsageRepository {
        val appInfos = mutableListOf<AppInfo>()
        val upserts = mutableListOf<DailyUsageUpsert>()

        override suspend fun upsertAppInfo(appInfo: AppInfo) {
            appInfos += appInfo
        }

        override suspend fun upsertDailyUsage(usage: DailyUsageUpsert) {
            upserts += usage
        }

        override suspend fun replaceDailyUsageForDates(
            dates: List<String>,
            usages: List<DailyUsageUpsert>,
        ) {
            upserts += usages
        }

        override suspend fun getTodayRanking(date: String): List<DailyAppUsage> {
            return emptyList()
        }

        override suspend fun getAppUsageLast7Days(
            packageName: String,
            fromDate: String,
            toDate: String,
        ): List<DailyAppUsage> {
            return emptyList()
        }

        override suspend fun getAppUsageForLast7Days(packageName: String): List<DailyAppUsage> {
            return emptyList()
        }

        override suspend fun clearAllUsageData() = Unit
    }

    private class FakeAppInfoResolver : AppInfoResolver {
        override suspend fun resolve(packageName: String): AppInfo {
            return AppInfo(
                packageName = packageName,
                appName = "Mail",
                iconUri = null,
                createdAt = 1000L,
                updatedAt = 1000L,
            )
        }
    }
}
