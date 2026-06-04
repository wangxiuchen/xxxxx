package com.example.appopencounter.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.appopencounter.AppOpenCounterApplication
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.appopencounter.usage.UsageAccessPermissionChecker
import com.example.appopencounter.ui.detail.AppDetailRoute
import com.example.appopencounter.ui.detail.AppDetailViewModel
import com.example.appopencounter.ui.home.HomeRoute
import com.example.appopencounter.ui.home.HomeViewModel
import com.example.appopencounter.ui.permission.PermissionScreen
import com.example.appopencounter.ui.settings.SettingsRoute
import com.example.appopencounter.ui.settings.SettingsViewModel

@Composable
fun AppNavHost() {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as AppOpenCounterApplication).appContainer
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val permissionChecker = remember(context) {
        UsageAccessPermissionChecker(context.applicationContext)
    }
    val homeViewModelFactory = remember(appContainer) {
        HomeViewModel.Factory(
            usageRepository = appContainer.usageRepository,
            collectUsageUseCase = appContainer.collectUsageUseCase,
            usageAccessChecker = appContainer.usageAccessPermissionChecker,
        )
    }
    val settingsViewModelFactory = remember(appContainer) {
        SettingsViewModel.Factory(
            usageSettingsRepository = appContainer.usageSettingsRepository,
            usageRepository = appContainer.usageRepository,
            usageAccessChecker = appContainer.usageAccessPermissionChecker,
        )
    }
    var hasUsageAccess by remember {
        mutableStateOf(permissionChecker.hasUsageAccess())
    }
    val startDestination = if (hasUsageAccess) {
        AppDestinations.Home
    } else {
        AppDestinations.Permission
    }

    fun refreshUsageAccessPermission() {
        hasUsageAccess = permissionChecker.hasUsageAccess()
    }

    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    DisposableEffect(lifecycleOwner, permissionChecker) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshUsageAccessPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(AppDestinations.Permission) {
            PermissionScreen(
                onOpenUsageAccessSettings = ::openUsageAccessSettings,
            )
        }

        composable(AppDestinations.Home) {
            HomeRoute(
                viewModelFactory = homeViewModelFactory,
                onOpenAppDetail = { packageName ->
                    navController.navigate(
                        AppDestinations.appDetailRoute(Uri.encode(packageName)),
                    )
                },
                onOpenSettings = { navController.navigate(AppDestinations.Settings) },
                onOpenPermissionSettings = ::openUsageAccessSettings,
            )
        }

        composable(
            route = AppDestinations.AppDetailRoute,
            arguments = listOf(
                navArgument(AppDestinations.AppDetailPackageNameArg) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments
                ?.getString(AppDestinations.AppDetailPackageNameArg)
                ?.let(Uri::decode)
                .orEmpty()
            AppDetailRoute(
                viewModelFactory = AppDetailViewModel.Factory(
                    packageName = packageName,
                    usageRepository = appContainer.usageRepository,
                ),
                onBack = { navController.popBackStack() },
            )
        }

        composable(AppDestinations.Settings) {
            SettingsRoute(
                viewModelFactory = settingsViewModelFactory,
                onBack = { navController.popBackStack() },
                onOpenUsageAccessSettings = ::openUsageAccessSettings,
            )
        }
    }

    LaunchedEffect(hasUsageAccess) {
        val targetDestination = if (hasUsageAccess) {
            AppDestinations.Home
        } else {
            AppDestinations.Permission
        }

        if (navController.currentBackStackEntry?.destination?.route != targetDestination) {
            navController.navigate(targetDestination) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                restoreState = true
                launchSingleTop = true
            }
        }
    }
}
