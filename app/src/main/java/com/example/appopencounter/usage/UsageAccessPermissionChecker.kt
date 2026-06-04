package com.example.appopencounter.usage

import android.app.AppOpsManager
import android.content.Context
import android.os.Process

class UsageAccessPermissionChecker(
    private val context: Context,
) : UsageAccessChecker {
    @Suppress("DEPRECATION")
    override fun hasUsageAccess(): Boolean {
        return try {
            val appOpsManager = context.getSystemService(AppOpsManager::class.java)
                ?: return false
            val mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: SecurityException) {
            false
        }
    }
}
