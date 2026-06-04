package com.example.appopencounter.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.appopencounter.domain.model.DailyAppUsage
import com.example.appopencounter.domain.repository.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AppDetailViewModel(
    private val packageName: String,
    private val usageRepository: UsageRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AppDetailUiState(
            packageName = packageName,
            appName = packageName,
        ),
    )
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    init {
        loadAppDetail()
    }

    fun loadAppDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val today = today()
            val fromDate = today.minusDays(6)
            val usages = usageRepository.getAppUsageLast7Days(
                packageName = packageName,
                fromDate = fromDate.toString(),
                toDate = today.toString(),
            )
            val usageByDate = usages.associateBy { usage -> usage.date }
            val todayUsage = usageByDate[today.toString()]
            val displayUsage = usages.firstOrNull { usage ->
                !usage.appName.isNullOrBlank() || usage.iconUri != null
            } ?: todayUsage

            _uiState.value = AppDetailUiState(
                isLoading = false,
                packageName = packageName,
                appName = displayUsage?.appName ?: packageName,
                iconUri = displayUsage?.iconUri,
                todayOpenCount = todayUsage?.openCount ?: 0,
                todayFirstOpenTime = todayUsage?.firstOpenTime,
                todayLastOpenTime = todayUsage?.lastOpenTime,
                trend = buildLast7DaysTrend(
                    fromDate = fromDate,
                    usageByDate = usageByDate,
                ),
            )
        }
    }

    private fun buildLast7DaysTrend(
        fromDate: LocalDate,
        usageByDate: Map<String, DailyAppUsage>,
    ): List<AppDetailTrendPoint> {
        return (0L..6L).map { offset ->
            val date = fromDate.plusDays(offset).toString()
            AppDetailTrendPoint(
                date = date,
                openCount = usageByDate[date]?.openCount ?: 0,
            )
        }
    }

    private fun today(): LocalDate {
        return Instant
            .ofEpochMilli(clock.millis())
            .atZone(zoneId)
            .toLocalDate()
    }

    class Factory(
        private val packageName: String,
        private val usageRepository: UsageRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppDetailViewModel::class.java)) {
                return AppDetailViewModel(
                    packageName = packageName,
                    usageRepository = usageRepository,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
