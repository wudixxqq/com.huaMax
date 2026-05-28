package com.noobexon.xposedfakelocation.manager.ui.permissions

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.noobexon.xposedfakelocation.R
import com.noobexon.xposedfakelocation.manager.ui.navigation.Screen
import com.noobexon.xposedfakelocation.manager.ui.permissions.components.PermanentlyDeniedScreen
import com.noobexon.xposedfakelocation.manager.ui.permissions.components.PermissionRequestScreen

@Composable
fun PermissionsScreen(navController: NavController, permissionsViewModel: PermissionsViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? Activity

    if (activity == null) {
        Text(stringResource(R.string.permissions_activity_error))
        return
    }

    val hasPermissions by permissionsViewModel.hasPermissions
    val permanentlyDenied by permissionsViewModel.permanentlyDenied
    val permissionsChecked by permissionsViewModel.permissionsChecked

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            permissionsViewModel.updatePermissionsStatus(granted)
            if (granted) {
                navController.navigate(Screen.Map.route) {
                    popUpTo(Screen.Permissions.route) { inclusive = true }
                }
            } else {
                permissionsViewModel.checkIfPermanentlyDenied(activity)
            }
        }
    )

    LaunchedEffect(Unit) {
        permissionsViewModel.checkPermissions(context)
        if (hasPermissions) {
            navController.navigate(Screen.Map.route) {
                popUpTo(Screen.Permissions.route) { inclusive = true }
            }
        }
    }

    if (!permissionsChecked) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (!hasPermissions) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (permanentlyDenied) {
                    PermanentlyDeniedScreen(context)
                } else {
                    PermissionRequestScreen {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            }
        }
    }
}
