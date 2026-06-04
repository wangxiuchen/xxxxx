package com.example.appopencounter.ui.detail

import com.example.appopencounter.MainDispatcherRule
import com.example.appopencounter.domain.model.AppInfo
import com.example.appopencounter.domain.model.DailyAppUsage
import com.example.appopencounter.domain.model.DailyUsageUpsert
import com.example.appopencounter.domain.repository.UsageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zoneId = ZoneId.of("UTC")
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-06-04T12:00:00Z"),
        zoneId,
    )

    @Test
    fun loadAppDetail_fillsMissingDatesWithZeroAndSortsOldToNew() = runTest {
        val viewModel = createViewModel(
            repository = FakeUsageRepository(
                usages = listOf(
                    usage(
                        date = "2026-05-31",
                        openCount = 3,
                    ),
                    usage(
                        date = "2026-06-04",
                        openCount = 5,
                    ),
                ),
            ),
        )

        val state = viewModel.uiState.value

        assertEquals(7, state.trend.size)
        assertEquals(
            listOf(
                "2026-05-29",
                "2026-05-30",
                "2026-05-31",
                "2026-06-01",
                "2026-06-02",
                "2026-06-03",
                "2026-06-04",
            ),
            state.trend.map { point -> point.date },
        )
        assertEquals(listOf(0, 0, 3, 0, 0, 0, 5), state.trend.map { point -> point.openCount })
    }

    @Test
    fun loadAppDetail_usesTodayUsageForTodayMetrics() = runTest {
        val viewModel = createViewModel(
            repository = FakeUsageRepository(
                usages = listOf(
                    usage(
                        date = "2026-06-04",
                        openCount = 5,
                        firstOpenTime = Instant.parse("2026-06-04T09:00:00Z").toEpochMilli(),
                        lastOpenTime = Instant.parse("2026-06-04T11:00:00Z").toEpochMilli(),
                    ),
                ),
            ),
        )

        val state = viewModel.uiState.value

        assertEquals("Mail", state.appName)
        assertEquals(5, state.todayOpenCount)
        assertEquals(Instant.parse("2026-06-04T09:00:00Z").toEpochMilli(), state.todayFirstOpenTime)
        assertEquals(Instant.parse("2026-06-04T11:00:00Z").toEpochMilli(), state.todayLastOpenTime)
    }

    private fun createViewModel(
        repository: UsageRepository,
    ): AppDetailViewModel {
        return AppDetailViewModel(
            packageName = PACKAGE_NAME,
            usageRepository = repository,
            clock = fixedClock,
            zoneId = zoneId,
        )
    }

    private fun usage(
        date: String,
        openCount: Int,
        firstOpenTime: Long? = null,
        lastOpenTime: Long? = null,
    ): DailyAppUsage {
        return DailyAppUsage(
            packageName = PACKAGE_NAME,
            appName = "Mail",
            iconUri = null,
            date = date,
            openCount = openCount,
            firstOpenTime = firstOpenTime,
            lastOpenTime = lastOpenTime,
            updatedAt = fixedClock.millis(),
        )
    }

    private class FakeUsageRepository(
        private val usages: List<DailyAppUsage>,
    ) : UsageRepository {
        override suspend fun upsertAppInfo(appInfo: AppInfo) = Unit

        override suspend fun upsertDailyUsage(usage: DailyUsageUpsert) = Unit

        override suspend fun replaceDailyUsageForDates(
            dates: List<String>,
            usages: List<DailyUsageUpsert>,
        ) = Unit

        override suspend fun getTodayRanking(date: String): List<DailyAppUsage> {
            return emptyList()
        }

        override suspend fun getAppUsageLast7Days(
            packageName: String,
            fromDate: String,
            toDate: String,
        ): List<DailyAppUsage> {
            return usages.filter { usage ->
                usage.packageName == packageName &&
                    usage.date >= fromDate &&
                    usage.date <= toDate
            }
        }

        override suspend fun getAppUsageForLast7Days(packageName: String): List<DailyAppUsage> {
            return usages.filter { usage -> usage.packageName == packageName }
        }

        override suspend fun clearAllUsageData() = Unit
    }

    private companion object {
        const val PACKAGE_NAME = "com.example.mail"
    }
}
