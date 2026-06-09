package com.huaMax.manager.ui.navigation

sealed class Screen(val route: String) {
    object Authorization : Screen("authorization")
    object About : Screen("about")
    object Disclaimer : Screen("disclaimer")
    object Favorites : Screen("favorites")
    object Map : Screen("map")
    object Permissions : Screen("permissions")
    object Settings : Screen("settings")
    object TargetApps : Screen("target_apps")
    object Update : Screen("update")
}
