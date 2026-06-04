package com.example.appopencounter.ui.home

import com.example.appopencounter.MainDispatcherRule
import com.example.appopencounter.domain.model.AppInfo
import com.example.appopencounter.domain.model.DailyAppUsage
import com.example.appopencounter.domain.model.DailyUsageUpsert
import com.example.appopencounter.domain.repository.AppInfoResolver
import com.example.appopencounter.domain.repository.UsageRepository
import com.example.appopencounter.domain.repository.UsageSettingsRepository
import com.example.appopencounter.domain.usecase.CollectUsageUseCase
import com.example.appopencounter.usage.UsageAccessChecker
import com.example.appopencounter.usage.UsageEventAggregator
import com.example.appopencounter.usage.UsageEventRecord
import com.example.appopencounter.usage.UsageEventsSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zoneId = ZoneId.of("UTC")
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-06-04T12:00:00Z"),
        zoneId,
    )

    @Test
    fun refreshTodayStatistics_calculatesTotalCountAndSortsRankingDescending() = runTest {
        val repository = FakeUsageRepository(
            todayRanking = listOf(
                usage(
                    packageName = "com.example.mail",
                    appName = "Mail",
                    openCount = 2,
                ),
                usage(
                    packageName = "com.example.browser",
                    appName = "Browser",
                    openCount = 5,
                ),
            ),
        )

        val viewModel = createViewModel(
            repository = repository,
            accessChecker = FakeUsageAccessChecker(hasAccess = true),
        )

        val state = viewModel.uiState.value

        assertEquals(7, state.totalOpenCount)
        assertEquals("Browser", state.topApp?.appName)
        assertEquals("com.example.browser", state.ranking[0].packageName)
        assertEquals("com.example.mail", state.ranking[1].packageName)
    }

    @Test
    fun refreshTodayStatistics_setsPermissionStateWhenUsageAccessIsClosed() = runTest {
        val repository = FakeUsageRepository(todayRanking = emptyList())

        val viewModel = createViewModel(
            repository = repository,
            accessChecker = FakeUsageAccessChecker(hasAccess = false),
        )

        val state = viewModel.uiState.value

        assertFalse(state.hasUsageAccess)
        assertEquals(0, state.totalOpenCount)
        assertEquals(emptyList<HomeRankingItem>(), state.ranking)
    }

    @Test
    fun refreshTodayStatistics_showsErrorWhenCollectionFails() = runTest {
        val repository = FakeUsageRepository(todayRanking = emptyList())

        val viewModel = createViewModel(
            repository = repository,
            accessChecker = FakeUsageAccessChecker(hasAccess = true),
            eventsSource = ThrowingUsageEventsSource(),
        )

        val state = viewModel.uiState.value

        assertNotNull(state.errorMessage)
    }

    private fun createViewModel(
        repository: UsageRepository,
        accessChecker: UsageAccessChecker,
        eventsSource: UsageEventsSource = EmptyUsageEventsSource(),
    ): HomeViewModel {
        return HomeViewModel(
            usageRepository = repository,
            collectUsageUseCase = CollectUsageUseCase(
                usageSettingsRepository = FakeUsageSettingsRepository(),
                usageAccessChecker = accessChecker,
                usageEventsSource = eventsSource,
                usageEventAggregator = UsageEventAggregator(
                    currentPackageName = "com.example.appopencounter",
                    zoneId = zoneId,
                ),
                usageRepository = repository,
                appInfoResolver = EmptyAppInfoResolver(),
                clock = fixedClock,
            ),
            usageAccessChecker = accessChecker,
            clock = fixedClock,
            zoneId = zoneId,
        )
    }

    private fun usage(
        packageName: String,
        appName: String,
        openCount: Int,
    ): DailyAppUsage {
        return DailyAppUsage(
            packageName = packageName,
            appName = appName,
            iconUri = null,
            date = "2026-06-04",
            openCount = openCount,
            firstOpenTime = null,
            lastOpenTime = null,
            updatedAt = fixedClock.millis(),
        )
    }

    private class FakeUsageRepository(
        private val todayRanking: List<DailyAppUsage>,
    ) : UsageRepository {
        override suspend fun upsertAppInfo(appInfo: AppInfo) = Unit

        override suspend fun upsertDailyUsage(usage: DailyUsageUpsert) = Unit

        override suspend fun replaceDailyUsageForDates(
            dates: List<String>,
            usages: List<DailyUsageUpsert>,
        ) = Unit

        override suspend fun getTodayRanking(date: String): List<DailyAppUsage> {
            return todayRanking
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

    private class FakeUsageSettingsRepository : UsageSettingsRepository {
        override val usageCollectionEnabled: Flow<Boolean> = MutableStateFlow(true)
        override val lastCollectTimeMillis: Flow<Long?> = MutableStateFlow(null)

        override suspend fun isUsageCollectionEnabled() = true

        override suspend fun setUsageCollectionEnabled(enabled: Boolean) = Unit

        override suspend fun getLastCollectTimeMillis(): Long? = null

        override suspend fun setLastCollectTimeMillis(timestampMillis: Long) = Unit

        override suspend fun clearLastCollectTimeMillis() = Unit
    }

    private class FakeUsageAccessChecker(
        private val hasAccess: Boolean,
    ) : UsageAccessChecker {
        override fun hasUsageAccess() = hasAccess
    }

    private class EmptyUsageEventsSource : UsageEventsSource {
        override fun collectUsageEvents(
            fromMillis: Long,
            toMillis: Long,
        ): List<UsageEventRecord> {
            return emptyList()
        }
    }

    private class ThrowingUsageEventsSource : UsageEventsSource {
        override fun collectUsageEvents(
            fromMillis: Long,
            toMillis: Long,
        ): List<UsageEventRecord> {
            error("Collection failed.")
        }
    }

    private class EmptyAppInfoResolver : AppInfoResolver {
        override suspend fun resolve(packageName: String): AppInfo? = null
    }
}
