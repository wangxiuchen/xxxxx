package com.example.appopencounter.data.repository

import com.example.appopencounter.data.local.dao.AppInfoDao
import com.example.appopencounter.data.local.dao.DailyAppUsageDao
import com.example.appopencounter.data.local.entity.AppInfoEntity
import com.example.appopencounter.data.local.entity.DailyAppUsageEntity
import com.example.appopencounter.domain.model.AppInfo
import com.example.appopencounter.domain.model.DailyAppUsage
import com.example.appopencounter.domain.model.DailyUsageUpsert
import com.example.appopencounter.domain.repository.UsageRepository
import java.time.LocalDate

class UsageRepositoryImpl(
    private val appInfoDao: AppInfoDao,
    private val dailyAppUsageDao: DailyAppUsageDao,
) : UsageRepository {
    override suspend fun upsertAppInfo(appInfo: AppInfo) {
        appInfoDao.upsert(
            AppInfoEntity(
                packageName = appInfo.packageName,
                appName = appInfo.appName,
                iconUri = appInfo.iconUri,
                createdAt = appInfo.createdAt,
                updatedAt = appInfo.updatedAt,
            ),
        )
    }

    override suspend fun upsertDailyUsage(usage: DailyUsageUpsert) {
        dailyAppUsageDao.upsertDailyUsage(usage.toEntity())
    }

    override suspend fun replaceDailyUsageForDates(
        dates: List<String>,
        usages: List<DailyUsageUpsert>,
    ) {
        dailyAppUsageDao.replaceUsageForDates(
            dates = dates,
            usages = usages.map { usage -> usage.toEntity() },
        )
    }

    override suspend fun getTodayRanking(date: String): List<DailyAppUsage> {
        return dailyAppUsageDao
            .getRankingByDate(date)
            .map { usage -> usage.toDomain() }
    }

    override suspend fun getAppUsageLast7Days(
        packageName: String,
        fromDate: String,
        toDate: String,
    ): List<DailyAppUsage> {
        return dailyAppUsageDao
            .getUsageForPackageBetweenDates(
                packageName = packageName,
                fromDate = fromDate,
                toDate = toDate,
            )
            .map { usage -> usage.toDomain() }
    }

    override suspend fun getAppUsageForLast7Days(packageName: String): List<DailyAppUsage> {
        val toDate = LocalDate.now()
        val fromDate = toDate.minusDays(6)
        return getAppUsageLast7Days(
            packageName = packageName,
            fromDate = fromDate.toString(),
            toDate = toDate.toString(),
        )
    }

    override suspend fun clearAllUsageData() {
        dailyAppUsageDao.clearAll()
        appInfoDao.clearAll()
    }

    private suspend fun DailyAppUsageEntity.toDomain(): DailyAppUsage {
        val appInfo = appInfoDao.getByPackageName(packageName)
        return DailyAppUsage(
            packageName = packageName,
            appName = appInfo?.appName,
            iconUri = appInfo?.iconUri,
            date = date,
            openCount = openCount,
            firstOpenTime = firstOpenTime,
            lastOpenTime = lastOpenTime,
            updatedAt = updatedAt,
        )
    }

    private fun DailyUsageUpsert.toEntity(): DailyAppUsageEntity {
        return DailyAppUsageEntity(
            packageName = packageName,
            date = date,
            openCount = openCount,
            firstOpenTime = firstOpenTime,
            lastOpenTime = lastOpenTime,
            updatedAt = updatedAt,
        )
    }
}
