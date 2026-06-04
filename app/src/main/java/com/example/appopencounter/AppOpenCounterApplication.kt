package com.example.appopencounter

import android.app.Application
import com.example.appopencounter.di.AppContainer

class AppOpenCounterApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
