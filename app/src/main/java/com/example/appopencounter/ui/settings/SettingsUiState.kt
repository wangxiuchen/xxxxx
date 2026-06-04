package com.example.appopencounter.ui.settings

data class SettingsUiState(
    val hasUsageAccess: Boolean = false,
    val usageCollectionEnabled: Boolean = true,
    val showClearDataConfirmation: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val isClearingData: Boolean = false,
)
