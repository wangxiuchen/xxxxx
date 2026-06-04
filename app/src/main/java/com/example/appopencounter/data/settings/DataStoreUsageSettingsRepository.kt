package com.example.appopencounter.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.appopencounter.domain.repository.UsageSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_open_counter_settings",
)

class DataStoreUsageSettingsRepository(
    context: Context,
) : UsageSettingsRepository {
    private val dataStore = context.applicationContext.settingsDataStore

    override val usageCollectionEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[USAGE_COLLECTION_ENABLED_KEY] ?: true
        }

    override val lastCollectTimeMillis: Flow<Long?> = dataStore.data
        .map { preferences ->
            preferences[LAST_COLLECT_TIME_MILLIS_KEY]
        }

    override suspend fun isUsageCollectionEnabled(): Boolean {
        return usageCollectionEnabled.first()
    }

    override suspend fun setUsageCollectionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[USAGE_COLLECTION_ENABLED_KEY] = enabled
        }
    }

    override suspend fun getLastCollectTimeMillis(): Long? {
        return lastCollectTimeMillis.first()
    }

    override suspend fun setLastCollectTimeMillis(timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_COLLECT_TIME_MILLIS_KEY] = timestampMillis
        }
    }

    override suspend fun clearLastCollectTimeMillis() {
        dataStore.edit { preferences ->
            preferences.remove(LAST_COLLECT_TIME_MILLIS_KEY)
        }
    }

    private companion object {
        val USAGE_COLLECTION_ENABLED_KEY = booleanPreferencesKey("usage_collection_enabled")
        val LAST_COLLECT_TIME_MILLIS_KEY = longPreferencesKey("last_collect_time_millis")
    }
}
