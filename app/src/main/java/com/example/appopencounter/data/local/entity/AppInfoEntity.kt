package com.example.appopencounter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfoEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val iconUri: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
