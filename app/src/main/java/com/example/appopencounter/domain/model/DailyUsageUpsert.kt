package com.example.appopencounter.domain.model

data class DailyUsageUpsert(
    val packageName: String,
    val date: String,
    val openCount: Int,
    val firstOpenTime: Long?,
    val lastOpenTime: Long?,
    val updatedAt: Long,
)
