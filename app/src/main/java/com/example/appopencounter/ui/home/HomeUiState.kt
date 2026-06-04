package com.example.appopencounter.ui.home

data class HomeUiState(
    val isLoading: Boolean = false,
    val hasUsageAccess: Boolean = true,
    val totalOpenCount: Int = 0,
    val topApp: HomeRankingItem? = null,
    val ranking: List<HomeRankingItem> = emptyList(),
    val lastUpdatedAt: Long? = null,
    val errorMessage: String? = null,
)

data class HomeRankingItem(
    val packageName: String,
    val appName: String,
    val iconUri: String?,
    val openCount: Int,
)
