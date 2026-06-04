package com.example.appopencounter.ui.settings

import com.example.appopencounter.MainDispatcherRule
import com.example.appopencounter.domain.model.AppInfo
import com.example.appopencounter.domain.model.DailyAppUsage
import com.example.appopencounter.domain.model.DailyUsageUpsert
import com.example.appopencounter.domain.repository.UsageRepository
import com.example.appopencounter.domain.repository.UsageSettingsRepository
import com.example.appopencounter.usage.UsageAccessChecker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun setUsageCollectionEnabled_canDisableAndReEnableCollection() = runTest {
        val settingsRepository = FakeUsageSettingsRepository(initialEnabled = true)
        val viewModel = createViewModel(settingsRepository = settingsRepository)

        viewModel.setUsageCollectionEnabled(false)
        assertFalse(viewModel.uiState.value.usageCollectionEnabled)

        viewModel.setUsageCollectionEnabled(true)
        assertTrue(viewModel.uiState.value.usageCollectionEnabled)
    }

    @Test
    fun confirmClearData_clearsRepositoryAfterConfirmation() = runTest {
        val usageRepository = FakeUsageRepository()
        val viewModel = createViewModel(usageRepository = usageRepository)

        viewModel.requestClearDataConfirmation()
        assertTrue(viewModel.uiState.value.showClearDataConfirmation)

        viewModel.confirmClearData()

        assertEquals(1, usageRepository.clearAllCallCount)
        assertEquals(1, settingsRepository.clearLastCollectTimeCallCount)
        assertFalse(viewModel.uiState.value.showClearDataConfirmation)
        assertFalse(viewModel.uiState.value.isClearingData)
    }

    @Test
    fun privacyDialog_canBeShownAndDismissed() = runTest {
        val viewModel = createViewModel()

        viewModel.showPrivacyDialog()
        assertTrue(viewModel.uiState.value.showPrivacyDialog)

        viewModel.dismissPrivacyDialog()
        assertFalse(viewModel.uiState.value.showPrivacyDialog)
    }

    @Test
    fun refreshUsageAccessStatus_updatesPermissionState() = runTest {
        val accessChecker = FakeUsageAccessChecker(hasAccess = false)
        val viewModel = createViewModel(accessChecker = accessChecker)

        assertFalse(viewModel.uiState.value.hasUsageAccess)

        accessChecker.hasAccess = true
        viewModel.refreshUsageAccessStatus()

        assertTrue(viewModel.uiState.value.hasUsageAccess)
    }

    private fun createViewModel(
        settingsRepository: UsageSettingsRepository = FakeUsageSettingsRepository(initialEnabled = true),
        usageRepository: FakeUsageRepository = FakeUsageRepository(),
        accessChecker: FakeUsageAccessChecker = FakeUsageAccessChecker(hasAccess = true),
    ): SettingsViewModel {
        return SettingsViewModel(
            usageSettingsRepository = settingsRepository,
            usageRepository = usageRepository,
            usageAccessChecker = accessChecker,
        )
    }

    private class FakeUsageSettingsRepository(
        initialEnabled: Boolean,
    ) : UsageSettingsRepository {
        private val enabledFlow = MutableStateFlow(initialEnabled)

        override val usageCollectionEnabled: Flow<Boolean> = enabledFlow
        override val lastCollectTimeMillis: Flow<Long?> = MutableStateFlow(null)
        var clearLastCollectTimeCallCount = 0
            private set

        override suspend fun isUsageCollectionEnabled(): Boolean {
            return enabledFlow.value
        }

        override suspend fun setUsageCollectionEnabled(enabled: Boolean) {
            enabledFlow.value = enabled
        }

        override suspend fun getLastCollectTimeMillis(): Long? = null

        override suspend fun setLastCollectTimeMillis(timestampMillis: Long) = Unit

        override suspend fun clearLastCollectTimeMillis() {
            clearLastCollectTimeCallCount += 1
        }
    }

    private class FakeUsageAccessChecker(
        var hasAccess: Boolean,
    ) : UsageAccessChecker {
        override fun hasUsageAccess() = hasAccess
    }

    private class FakeUsageRepository : UsageRepository {
        var clearAllCallCount = 0
            private set

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
            return emptyList()
        }

        override suspend fun getAppUsageForLast7Days(packageName: String): List<DailyAppUsage> {
            return emptyList()
        }

        override suspend fun clearAllUsageData() {
            clearAllCallCount += 1
        }
    }
}
