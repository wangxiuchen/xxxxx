package com.example.appopencounter.domain.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val iconUri: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
