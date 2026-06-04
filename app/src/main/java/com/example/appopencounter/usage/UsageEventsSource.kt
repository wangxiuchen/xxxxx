package com.example.appopencounter.usage

interface UsageEventsSource {
    fun collectUsageEvents(
        fromMillis: Long,
        toMillis: Long,
    ): List<UsageEventRecord>
}
