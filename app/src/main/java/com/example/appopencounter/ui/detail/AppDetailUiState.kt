package com.example.appopencounter.ui.detail

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val packageName: String = "",
    val appName: String = "",
    val iconUri: String? = null,
    val todayOpenCount: Int = 0,
    val todayFirstOpenTime: Long? = null,
    val todayLastOpenTime: Long? = null,
    val trend: List<AppDetailTrendPoint> = emptyList(),
)

data class AppDetailTrendPoint(
    val date: String,
    val openCount: Int,
)
