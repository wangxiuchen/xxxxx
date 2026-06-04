package com.example.appopencounter.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_app_usage",
    indices = [
        Index(
            value = ["packageName", "date"],
            unique = true,
        ),
    ],
)
data class DailyAppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val date: String,
    val openCount: Int,
    val firstOpenTime: Long?,
    val lastOpenTime: Long?,
    val updatedAt: Long,
)
