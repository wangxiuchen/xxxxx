package com.example.appopencounter.domain.model

data class DailyAppUsage(
    val packageName: String,
    val appName: String?,
    val iconUri: String?,
    val date: String,
    val openCount: Int,
    val firstOpenTime: Long?,
    val lastOpenTime: Long?,
    val updatedAt: Long,
)
