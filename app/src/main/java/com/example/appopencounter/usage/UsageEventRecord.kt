package com.example.appopencounter.usage

data class UsageEventRecord(
    val packageName: String,
    val timestamp: Long,
    val kind: UsageEventKind,
)
