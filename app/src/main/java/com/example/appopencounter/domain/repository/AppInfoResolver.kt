package com.example.appopencounter.domain.repository

import com.example.appopencounter.domain.model.AppInfo

interface AppInfoResolver {
    suspend fun resolve(packageName: String): AppInfo?
}
