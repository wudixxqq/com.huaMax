package com.noobexon.xposedfakelocation.manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.noobexon.xposedfakelocation.manager.ui.about.AboutScreen
import com.noobexon.xposedfakelocation.manager.ui.favorites.FavoritesScreen
import com.noobexon.xposedfakelocation.manager.ui.map.MapScreen
import com.noobexon.xposedfakelocation.manager.ui.map.MapViewModel
import com.noobexon.xposedfakelocation.manager.ui.permissions.PermissionsScreen
import com.noobexon.xposedfakelocation.manager.ui.settings.SettingsScreen
import com.noobexon.xposedfakelocation.manager.ui.targetapps.TargetAppsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
) {
    val mapViewModel: MapViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Permissions.route,
    ) {
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
    }
}
