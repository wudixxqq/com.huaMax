package com.noobexon.xposedfakelocation.manager.ui.map

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.noobexon.xposedfakelocation.data.model.FavoriteLocation
import com.noobexon.xposedfakelocation.manager.ui.drawer.DrawerContent
import com.noobexon.xposedfakelocation.manager.ui.map.components.AddToFavoritesDialog
import com.noobexon.xposedfakelocation.manager.ui.map.components.AddToTemplateDialog
import com.noobexon.xposedfakelocation.manager.ui.map.components.GoToPointDialog
import com.noobexon.xposedfakelocation.manager.ui.map.components.MapViewContainer
import com.noobexon.xposedfakelocation.manager.ui.map.components.UpdateTemplateLocationDialog
import com.noobexon.xposedfakelocation.manager.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    mapViewModel: MapViewModel
) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsStateWithLifecycle()
    
    // Extract values from UI state
    val isPlaying = uiState.isPlaying
    val isFabClickable = uiState.isFabClickable
    val isLoading = uiState.loadingState == LoadingState.Loading
    
    // Dialog states
    val showGoToPointDialog = uiState.goToPointDialogState == DialogState.Visible
    val showAddToFavoritesDialog = uiState.addToFavoritesDialogState == DialogState.Visible
    val showAddToTemplateDialog = uiState.addToTemplateDialogState == DialogState.Visible
    val showUpdateTemplateLocationDialog = uiState.updateTemplateLocationDialogState == DialogState.Visible

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showOptionsMenu by remember { mutableStateOf(false) }

    // BackHandler to close the drawer when open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // Scaffold with drawer
    ModalNavigationDrawer(
        drawerContent = {
            DrawerContent(
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                },
                navController = navController
            )
        },
        scrimColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f), // Custom scrim color
        drawerState = drawerState,
        gesturesEnabled = false,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("XposedFakeLocation") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                mapViewModel.triggerCenterMapEvent()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Center")
                        }
                        IconButton(
                            onClick = {
                                showOptionsMenu = true
                            }
                        ) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(imageVector = Icons.Default.LocationSearching, contentDescription = "Go to Point") },
                                text = { Text("Go to Point") },
                                onClick = {
                                    showOptionsMenu = false
                                    mapViewModel.showGoToPointDialog()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(imageVector = Icons.Default.FavoriteBorder, contentDescription = "Add to Favorites") },
                                text = { Text("Add to Favorites") },
                                onClick = {
                                    showOptionsMenu = false
                                    mapViewModel.showAddToFavoritesDialog()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = "Add to Templates") },
                                text = { Text("Add to Templates") },
                                onClick = {
                                    showOptionsMenu = false
                                    mapViewModel.showAddToTemplateDialog()
                                },
                                enabled = isFabClickable
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(imageVector = Icons.Default.EditLocationAlt, contentDescription = "Update Template Location") },
                                text = { Text("Update Template Location") },
                                onClick = {
                                    showOptionsMenu = false
                                    mapViewModel.showUpdateTemplateLocationDialog()
                                },
                                enabled = isFabClickable && uiState.templates.isNotEmpty()
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(imageVector = Icons.Default.Star, contentDescription = "Favorites") },
                                text = { Text("Favorites") },
                                onClick = {
                                    showOptionsMenu = false
                                    navController.navigate(Screen.Favorites.route)
                                }
                            )
                            // add clear location feature
                            DropdownMenuItem(
                                leadingIcon = { Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Location") },
                                text = { Text("Clear Location") },
                                onClick = {
                                    showOptionsMenu = false
                                    mapViewModel.updateClickedLocation(null)
                                },
                                enabled = isFabClickable // allow clearing only when a location is marked.
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
                            if (!wasPlaying) {
                                Toast.makeText(context, "Fake Location Set", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Unset Fake Location", Toast.LENGTH_SHORT).show()
                            }
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
                        contentDescription = if (isPlaying) "Stop" else "Play"
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
            // Prefill coordinates from the last clicked location (marker)
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

        if (showAddToTemplateDialog) {
            AddToTemplateDialog(
                mapViewModel = mapViewModel,
                onDismissRequest = { mapViewModel.hideAddToTemplateDialog() },
                onAddTemplate = { template ->
                    mapViewModel.addTemplate(template)
                    mapViewModel.hideAddToTemplateDialog()
                    Toast.makeText(context, "Template saved", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showUpdateTemplateLocationDialog) {
            UpdateTemplateLocationDialog(
                templates = uiState.templates,
                marker = uiState.lastClickedLocation,
                onDismissRequest = { mapViewModel.hideUpdateTemplateLocationDialog() },
                onUpdateTemplate = { template, latitude, longitude ->
                    mapViewModel.updateTemplateLocation(template, latitude, longitude)
                    mapViewModel.hideUpdateTemplateLocationDialog()
                    Toast.makeText(context, "Template location updated", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
