package com.noobexon.xposedfakelocation.manager.ui.map

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.noobexon.xposedfakelocation.R
import com.noobexon.xposedfakelocation.data.model.FavoriteLocation
import com.noobexon.xposedfakelocation.manager.ui.drawer.DrawerContent
import com.noobexon.xposedfakelocation.manager.ui.map.components.AddToFavoritesDialog
import com.noobexon.xposedfakelocation.manager.ui.map.components.GoToPointDialog
import com.noobexon.xposedfakelocation.manager.ui.map.components.MapViewContainer
import com.noobexon.xposedfakelocation.manager.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    mapViewModel: MapViewModel
) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsStateWithLifecycle()
    val isPlaying = uiState.isPlaying
    val isFabClickable = uiState.isFabClickable
    val showGoToPointDialog = uiState.goToPointDialogState == DialogState.Visible
    val showAddToFavoritesDialog = uiState.addToFavoritesDialogState == DialogState.Visible
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showOptionsMenu by remember { mutableStateOf(false) }
    val fakeLocationSet = stringResource(R.string.toast_fake_location_set)
    val fakeLocationUnset = stringResource(R.string.toast_unset_fake_location)

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerContent = {
            DrawerContent(
                onCloseDrawer = { scope.launch { drawerState.close() } },
                navController = navController
            )
        },
        scrimColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
        drawerState = drawerState,
        gesturesEnabled = false,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = stringResource(R.string.cd_menu))
                        }
                    },
                    actions = {
                        IconButton(onClick = { mapViewModel.triggerCenterMapEvent() }) {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = stringResource(R.string.cd_center))
                        }
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_options))
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocationSearching,
                                        contentDescription = stringResource(R.string.map_go_to_point)
                                    )
                                },
                                text = { Text(stringResource(R.string.map_go_to_point)) },
                                onClick = {
                                    showOptionsMenu = false
                                    mapViewModel.showGoToPointDialog()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FavoriteBorder,
                                        contentDescription = stringResource(R.string.map_add_to_favorites)
                                    )
                                },
                                text = { Text(stringResource(R.string.map_add_to_favorites)) },
                                onClick = {
                                    showOptionsMenu = false
                                    mapViewModel.showAddToFavoritesDialog()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = stringResource(R.string.screen_favorites)
                                    )
                                },
                                text = { Text(stringResource(R.string.screen_favorites)) },
                                onClick = {
                                    showOptionsMenu = false
                                    navController.navigate(Screen.Favorites.route)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.map_clear_location)
                                    )
                                },
                                text = { Text(stringResource(R.string.map_clear_location)) },
                                onClick = {
                                    showOptionsMenu = false
                                    mapViewModel.updateClickedLocation(null)
                                },
                                enabled = isFabClickable
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (isFabClickable) {
                            val wasPlaying = uiState.isPlaying
                            mapViewModel.togglePlaying()
                            Toast.makeText(
                                context,
                                if (!wasPlaying) fakeLocationSet else fakeLocationUnset,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp),
                    containerColor = if (isFabClickable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    },
                    contentColor = if (isFabClickable) {
                        contentColorFor(MaterialTheme.colorScheme.primary)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = if (isFabClickable) 6.dp else 0.dp,
                        pressedElevation = if (isFabClickable) 12.dp else 0.dp
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) {
                            stringResource(R.string.cd_stop)
                        } else {
                            stringResource(R.string.cd_play)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MapViewContainer(mapViewModel)
            }
        }

        if (showGoToPointDialog) {
            GoToPointDialog(
                onDismissRequest = { mapViewModel.hideGoToPointDialog() },
                onGoToPoint = { latitude, longitude ->
                    mapViewModel.goToPoint(latitude, longitude)
                    mapViewModel.hideGoToPointDialog()
                },
                mapViewModel = mapViewModel
            )
        }

        if (showAddToFavoritesDialog) {
            val lastClickedLocation = uiState.lastClickedLocation

            LaunchedEffect(lastClickedLocation) {
                mapViewModel.prefillCoordinatesFromMarker(
                    lastClickedLocation?.latitude,
                    lastClickedLocation?.longitude
                )
            }

            AddToFavoritesDialog(
                mapViewModel = mapViewModel,
                onDismissRequest = { mapViewModel.hideAddToFavoritesDialog() },
                onAddFavorite = { name, latitude, longitude ->
                    val favorite = FavoriteLocation(name, latitude, longitude)
                    mapViewModel.addFavoriteLocation(favorite)
                    mapViewModel.hideAddToFavoritesDialog()
                }
            )
        }
    }
}
