package com.example.appopencounter.usage

data class AppOpenEvent(
    val packageName: String,
    val foregroundTime: Long,
    val backgroundTime: Long?,
    val durationMs: Long,
    val date: String,
)
