package com.example.appopencounter.ui.navigation

object AppDestinations {
    const val Permission = "permission"
    const val Home = "home"
    const val AppDetail = "app-detail"
    const val AppDetailPackageNameArg = "packageName"
    const val AppDetailRoute = "$AppDetail/{$AppDetailPackageNameArg}"
    const val Settings = "settings"

    fun appDetailRoute(packageName: String): String {
        return "$AppDetail/$packageName"
    }
}
