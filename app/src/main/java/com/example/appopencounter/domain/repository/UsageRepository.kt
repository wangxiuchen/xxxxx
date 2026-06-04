package com.example.appopencounter.domain.repository

import com.example.appopencounter.domain.model.AppInfo
import com.example.appopencounter.domain.model.DailyAppUsage
import com.example.appopencounter.domain.model.DailyUsageUpsert

interface UsageRepository {
    suspend fun upsertAppInfo(appInfo: AppInfo)

    suspend fun upsertDailyUsage(usage: DailyUsageUpsert)

    suspend fun replaceDailyUsageForDates(
        dates: List<String>,
        usages: List<DailyUsageUpsert>,
    )

    suspend fun getTodayRanking(date: String): List<DailyAppUsage>

    suspend fun getAppUsageLast7Days(
        packageName: String,
        fromDate: String,
        toDate: String,
    ): List<DailyAppUsage>

    suspend fun getAppUsageForLast7Days(packageName: String): List<DailyAppUsage>

    suspend fun clearAllUsageData()
}
