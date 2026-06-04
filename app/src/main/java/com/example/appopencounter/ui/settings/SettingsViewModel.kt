package com.example.appopencounter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.appopencounter.domain.repository.UsageRepository
import com.example.appopencounter.domain.repository.UsageSettingsRepository
import com.example.appopencounter.usage.UsageAccessChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val usageSettingsRepository: UsageSettingsRepository,
    private val usageRepository: UsageRepository,
    private val usageAccessChecker: UsageAccessChecker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            hasUsageAccess = usageAccessChecker.hasUsageAccess(),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeUsageCollectionSetting()
    }

    fun refreshUsageAccessStatus() {
        _uiState.update { state ->
            state.copy(hasUsageAccess = usageAccessChecker.hasUsageAccess())
        }
    }

    fun setUsageCollectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            usageSettingsRepository.setUsageCollectionEnabled(enabled)
        }
    }

    fun requestClearDataConfirmation() {
        _uiState.update { state ->
            state.copy(showClearDataConfirmation = true)
        }
    }

    fun dismissClearDataConfirmation() {
        _uiState.update { state ->
            state.copy(showClearDataConfirmation = false)
        }
    }

    fun confirmClearData() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isClearingData = true)
            }
            usageRepository.clearAllUsageData()
            usageSettingsRepository.clearLastCollectTimeMillis()
            _uiState.update { state ->
                state.copy(
                    showClearDataConfirmation = false,
                    isClearingData = false,
                )
            }
        }
    }

    fun showPrivacyDialog() {
        _uiState.update { state ->
            state.copy(showPrivacyDialog = true)
        }
    }

    fun dismissPrivacyDialog() {
        _uiState.update { state ->
            state.copy(showPrivacyDialog = false)
        }
    }

    private fun observeUsageCollectionSetting() {
        viewModelScope.launch {
            usageSettingsRepository.usageCollectionEnabled.collect { enabled ->
                _uiState.update { state ->
                    state.copy(usageCollectionEnabled = enabled)
                }
            }
        }
    }

    class Factory(
        private val usageSettingsRepository: UsageSettingsRepository,
        private val usageRepository: UsageRepository,
        private val usageAccessChecker: UsageAccessChecker,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(
                    usageSettingsRepository = usageSettingsRepository,
                    usageRepository = usageRepository,
                    usageAccessChecker = usageAccessChecker,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
