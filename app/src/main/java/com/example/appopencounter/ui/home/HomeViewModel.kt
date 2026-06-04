package com.example.appopencounter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.appopencounter.domain.model.DailyAppUsage
import com.example.appopencounter.domain.repository.UsageRepository
import com.example.appopencounter.domain.usecase.CollectUsageResult
import com.example.appopencounter.domain.usecase.CollectUsageUseCase
import com.example.appopencounter.usage.UsageAccessChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class HomeViewModel(
    private val usageRepository: UsageRepository,
    private val collectUsageUseCase: CollectUsageUseCase,
    private val usageAccessChecker: UsageAccessChecker,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshTodayStatistics()
    }

    fun refreshTodayStatistics() {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true) }

            val hasUsageAccess = usageAccessChecker.hasUsageAccess()
            if (!hasUsageAccess) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    hasUsageAccess = false,
                )
                return@launch
            }

            val now = clock.millis()
            val collectResult = collectUsageUseCase()
            loadTodayRanking(
                date = todayString(now),
                hasUsageAccess = true,
                errorMessage = collectResult.toErrorMessage(),
            )
        }
    }

    fun reloadTodayStatisticsFromLocalData() {
        viewModelScope.launch {
            val hasUsageAccess = usageAccessChecker.hasUsageAccess()
            if (!hasUsageAccess) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    hasUsageAccess = false,
                )
                return@launch
            }

            loadTodayRanking(
                date = todayString(clock.millis()),
                hasUsageAccess = true,
                errorMessage = null,
            )
        }
    }

    private suspend fun loadTodayRanking(
        date: String,
        hasUsageAccess: Boolean,
        errorMessage: String?,
    ) {
        val ranking = usageRepository
            .getTodayRanking(date)
            .sortedByDescending { usage -> usage.openCount }

        val rankingItems = ranking.map { usage -> usage.toRankingItem() }

        _uiState.value = HomeUiState(
            isLoading = false,
            hasUsageAccess = hasUsageAccess,
            totalOpenCount = ranking.sumOf { usage -> usage.openCount },
            topApp = rankingItems.firstOrNull(),
            ranking = rankingItems,
            lastUpdatedAt = ranking.maxOfOrNull { usage -> usage.updatedAt },
            errorMessage = errorMessage,
        )
    }

    private fun DailyAppUsage.toRankingItem(): HomeRankingItem {
        return HomeRankingItem(
            packageName = packageName,
            appName = appName ?: packageName,
            iconUri = iconUri,
            openCount = openCount,
        )
    }

    private fun startOfTodayMillis(nowMillis: Long): Long {
        return Instant
            .ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun todayString(nowMillis: Long): String {
        return Instant
            .ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .toString()
    }

    private fun CollectUsageResult.toErrorMessage(): String? {
        return when (this) {
            CollectUsageResult.Failed -> "采集失败，请稍后重试。已有统计数据不会被清除。"
            CollectUsageResult.PermissionDenied -> "Usage Access 权限已关闭，无法采集最新数据。"
            CollectUsageResult.CollectionDisabled -> null
            is CollectUsageResult.Collected -> null
        }
    }

    class Factory(
        private val usageRepository: UsageRepository,
        private val collectUsageUseCase: CollectUsageUseCase,
        private val usageAccessChecker: UsageAccessChecker,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(
                    usageRepository = usageRepository,
                    collectUsageUseCase = collectUsageUseCase,
                    usageAccessChecker = usageAccessChecker,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
