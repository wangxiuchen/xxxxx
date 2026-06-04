package com.example.appopencounter.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.example.appopencounter.domain.model.AppInfo
import com.example.appopencounter.domain.repository.AppInfoResolver
import java.time.Clock

class PackageManagerAppInfoResolver(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) : AppInfoResolver {
    private val packageManager = context.applicationContext.packageManager

    override suspend fun resolve(packageName: String): AppInfo? {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager
                .getApplicationLabel(applicationInfo)
                .toString()
                .ifBlank { packageName }
            val now = clock.millis()

            AppInfo(
                packageName = packageName,
                appName = appName,
                iconUri = null,
                createdAt = now,
                updatedAt = now,
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
