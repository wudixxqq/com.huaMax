package com.huaMax.manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.huaMax.data.repository.PreferencesRepository
import com.huaMax.manager.ui.about.AboutScreen
import com.huaMax.manager.ui.disclaimer.DisclaimerScreen
import com.huaMax.manager.ui.favorites.FavoritesScreen
import com.huaMax.manager.ui.map.MapScreen
import com.huaMax.manager.ui.map.MapViewModel
import com.huaMax.manager.ui.permissions.PermissionsScreen
import com.huaMax.manager.ui.settings.SettingsScreen
import com.huaMax.manager.ui.targetapps.TargetAppsScreen
import com.huaMax.manager.ui.update.UpdateScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
) {
    val mapViewModel: MapViewModel = viewModel()
    val context = LocalContext.current
    val preferencesRepository = remember {
        PreferencesRepository(context.applicationContext)
    }
    val hasAcceptedDisclaimer = remember {
        preferencesRepository.hasAcceptedDisclaimer()
    }
    val firstContentRoute = Screen.Permissions.route

    NavHost(
        navController = navController,
        startDestination = if (hasAcceptedDisclaimer) firstContentRoute else Screen.Disclaimer.route,
    ) {
        composable(route = Screen.Disclaimer.route) {
            DisclaimerScreen(
                navController = navController,
                preferencesRepository = preferencesRepository,
                nextRoute = firstContentRoute
            )
        }
        composable(route = Screen.About.route) {
            AboutScreen(navController = navController)
        }
        composable(route = Screen.Favorites.route) {
            FavoritesScreen(navController = navController, mapViewModel)
        }
        composable(route = Screen.Map.route) {
            MapScreen(navController = navController, mapViewModel)
        }
        composable(route = Screen.Permissions.route) {
            PermissionsScreen(navController = navController)
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(route = Screen.TargetApps.route) {
            TargetAppsScreen(navController = navController)
        }
        composable(route = Screen.Update.route) {
            UpdateScreen(navController = navController)
        }
    }
}
