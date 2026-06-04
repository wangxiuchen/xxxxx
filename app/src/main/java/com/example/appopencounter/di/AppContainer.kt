package com.example.appopencounter.di

import android.content.Context
import com.example.appopencounter.data.local.db.AppDatabase
import com.example.appopencounter.data.repository.PackageManagerAppInfoResolver
import com.example.appopencounter.data.repository.UsageRepositoryImpl
import com.example.appopencounter.data.settings.DataStoreUsageSettingsRepository
import com.example.appopencounter.domain.repository.UsageRepository
import com.example.appopencounter.domain.repository.UsageSettingsRepository
import com.example.appopencounter.domain.usecase.CollectUsageUseCase
import com.example.appopencounter.usage.UsageAccessPermissionChecker
import com.example.appopencounter.usage.UsageEventAggregator
import com.example.appopencounter.usage.UsageEventCollector

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val database = AppDatabase.getInstance(applicationContext)

    val usageRepository: UsageRepository = UsageRepositoryImpl(
        appInfoDao = database.appInfoDao(),
        dailyAppUsageDao = database.dailyAppUsageDao(),
    )

    val usageSettingsRepository: UsageSettingsRepository = DataStoreUsageSettingsRepository(
        applicationContext,
    )

    val usageAccessPermissionChecker = UsageAccessPermissionChecker(applicationContext)

    private val usageEventAggregator = UsageEventAggregator(
        currentPackageName = applicationContext.packageName,
    )

    private val usageEventCollector = UsageEventCollector(
        context = applicationContext,
        permissionChecker = usageAccessPermissionChecker,
        aggregator = usageEventAggregator,
    )

    private val appInfoResolver = PackageManagerAppInfoResolver(applicationContext)

    val collectUsageUseCase = CollectUsageUseCase(
        usageSettingsRepository = usageSettingsRepository,
        usageAccessChecker = usageAccessPermissionChecker,
        usageEventsSource = usageEventCollector,
        usageEventAggregator = usageEventAggregator,
        usageRepository = usageRepository,
        appInfoResolver = appInfoResolver,
    )
}
