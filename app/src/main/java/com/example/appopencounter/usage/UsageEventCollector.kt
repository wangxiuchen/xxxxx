package com.example.appopencounter.usage

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.os.Build

class UsageEventCollector(
    private val context: Context,
    private val permissionChecker: UsageAccessChecker = UsageAccessPermissionChecker(
        context.applicationContext,
    ),
    private val aggregator: UsageEventAggregator = UsageEventAggregator(
        currentPackageName = context.packageName,
    ),
) : UsageEventsSource {
    fun collect(
        fromMillis: Long,
        toMillis: Long,
    ): List<AppOpenEvent> {
        val events = collectUsageEvents(
            fromMillis = fromMillis,
            toMillis = toMillis,
        )
        return aggregator.aggregate(
            events = events,
            rangeEndTimeMillis = toMillis,
        )
    }

    override fun collectUsageEvents(
        fromMillis: Long,
        toMillis: Long,
    ): List<UsageEventRecord> {
        require(fromMillis <= toMillis) {
            "fromMillis must be less than or equal to toMillis."
        }
        if (!permissionChecker.hasUsageAccess()) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
            ?: return emptyList()
        val usageEvents = try {
            usageStatsManager.queryEvents(fromMillis, toMillis)
        } catch (_: SecurityException) {
            return emptyList()
        }
        val event = UsageEvents.Event()
        val records = mutableListOf<UsageEventRecord>()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val kind = event.toUsageEventKind() ?: continue
            val packageName = event.packageName ?: continue
            if (packageName.isBlank()) {
                continue
            }

            records += UsageEventRecord(
                packageName = packageName,
                timestamp = event.timeStamp,
                kind = kind,
            )
        }

        return records
    }

    @SuppressLint("NewApi")
    private fun UsageEvents.Event.toUsageEventKind(): UsageEventKind? {
        return when (eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageEventKind.FOREGROUND
            UsageEvents.Event.MOVE_TO_BACKGROUND -> UsageEventKind.BACKGROUND
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    UsageEventKind.FOREGROUND
                } else {
                    null
                }
            }
            UsageEvents.Event.ACTIVITY_PAUSED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    UsageEventKind.BACKGROUND
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
