package com.example.appopencounter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appopencounter.data.local.entity.AppInfoEntity

@Dao
interface AppInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appInfo: AppInfoEntity)

    @Query("SELECT * FROM app_info WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): AppInfoEntity?

    @Query("DELETE FROM app_info")
    suspend fun clearAll()
}
