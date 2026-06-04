package com.example.appopencounter.domain.repository

import kotlinx.coroutines.flow.Flow

interface UsageSettingsRepository {
    val usageCollectionEnabled: Flow<Boolean>

    val lastCollectTimeMillis: Flow<Long?>

    suspend fun isUsageCollectionEnabled(): Boolean

    suspend fun setUsageCollectionEnabled(enabled: Boolean)

    suspend fun getLastCollectTimeMillis(): Long?

    suspend fun setLastCollectTimeMillis(timestampMillis: Long)

    suspend fun clearLastCollectTimeMillis()
}
