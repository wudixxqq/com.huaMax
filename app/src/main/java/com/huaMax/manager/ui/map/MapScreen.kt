package com.huaMax.manager.ui.map

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huaMax.R
import com.huaMax.data.model.FavoriteLocation
import com.huaMax.manager.ui.drawer.DrawerContent
import com.huaMax.manager.ui.map.components.AddToFavoritesDialog
import com.huaMax.manager.ui.map.components.GoToPointDialog
import com.huaMax.manager.ui.map.components.MapViewContainer
import com.huaMax.manager.ui.navigation.Screen
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

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
            containerColor = Color(0xFFF4FAF8),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF061F1A)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF7FCFA),
                        titleContentColor = Color(0xFF061F1A),
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.cd_menu),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { mapViewModel.triggerCenterMapEvent() }) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = stringResource(R.string.cd_center),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_options),
                                modifier = Modifier.size(32.dp)
                            )
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
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MapViewContainer(mapViewModel)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.18f))
                )
                MapStatusPanel(
                    lastClickedLocation = uiState.lastClickedLocation,
                    selectedLocationAddress = uiState.selectedLocationAddress,
                    isAddressLoading = uiState.isSelectedLocationAddressLoading,
                    addressMessageRes = uiState.selectedLocationAddressMessageRes,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 18.dp, top = 16.dp, end = 18.dp)
                )
                MapSearchPanel(
                    query = uiState.placeSearchQuery,
                    isLoading = uiState.isPlaceSearchLoading,
                    results = uiState.placeSearchResults,
                    errorMessageRes = uiState.placeSearchErrorMessageRes,
                    onQueryChange = mapViewModel::updatePlaceSearchQuery,
                    onSearch = mapViewModel::searchPlace,
                    onClear = mapViewModel::clearPlaceSearch,
                    onResultClick = mapViewModel::selectPlaceSearchResult,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                MapPlayButton(
                    isPlaying = isPlaying,
                    enabled = isFabClickable,
                    onClick = {
                        val wasPlaying = uiState.isPlaying
                        mapViewModel.togglePlaying()
                        Toast.makeText(
                            context,
                            if (!wasPlaying) fakeLocationSet else fakeLocationUnset,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
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

@Composable
private fun MapSearchPanel(
    query: String,
    isLoading: Boolean,
    results: List<PlaceSearchResult>,
    errorMessageRes: Int?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onResultClick: (PlaceSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 94.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (results.isNotEmpty() || errorMessageRes != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp
            ) {
                if (results.isNotEmpty()) {
                    LazyColumn {
                        items(results) { result ->
                            PlaceSearchResultRow(
                                result = result,
                                onClick = { onResultClick(result) }
                            )
                        }
                    }
                } else if (errorMessageRes != null) {
                    Text(
                        text = stringResource(errorMessageRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(8.dp, RoundedCornerShape(26.dp), clip = false),
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            placeholder = { Text(stringResource(R.string.map_search_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.cd_search_place),
                    modifier = Modifier.size(26.dp),
                    tint = Color(0xFF5D6B68)
                )
            },
            trailingIcon = {
                when {
                    isLoading -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    query.isNotEmpty() -> IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.cd_clear_search)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.95f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.92f),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun MapPlayButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val background = if (enabled) {
        Brush.linearGradient(
            listOf(
                Color(0xFF009F87),
                Color(0xFF007765)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFFDDE6E3),
                Color(0xFFC7D3D0)
            )
        )
    }
    val iconColor = if (enabled) Color.White else Color(0xFF73817D)

    Box(
        modifier = modifier
            .imePadding()
            .navigationBarsPadding()
            .padding(end = 20.dp, bottom = 16.dp)
            .size(60.dp)
            .shadow(10.dp, shape, clip = false)
            .clip(shape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) {
                stringResource(R.string.cd_stop)
            } else {
                stringResource(R.string.cd_play)
            },
            tint = iconColor,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun PlaceSearchResultRow(
    result: PlaceSearchResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = result.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = result.address,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(
                R.string.map_status_coordinates,
                result.latitude,
                result.longitude
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MapStatusPanel(
    lastClickedLocation: GeoPoint?,
    selectedLocationAddress: String?,
    isAddressLoading: Boolean,
    addressMessageRes: Int?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val title = when {
        isPlaying -> stringResource(R.string.map_status_active)
        lastClickedLocation != null -> stringResource(R.string.map_status_ready)
        else -> stringResource(R.string.map_status_waiting)
    }
    val coordinatesText = if (lastClickedLocation != null) {
        stringResource(
            R.string.map_status_coordinates,
            lastClickedLocation.latitude,
            lastClickedLocation.longitude
        )
    } else {
        stringResource(R.string.map_status_hint)
    }
    val addressText = when {
        lastClickedLocation == null -> null
        selectedLocationAddress?.isNotBlank() == true -> {
            stringResource(R.string.map_status_address, selectedLocationAddress)
        }
        isAddressLoading -> stringResource(R.string.map_status_address_loading)
        addressMessageRes != null -> stringResource(addressMessageRes)
        else -> null
    }
    val accentColor = if (isPlaying) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 7.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = 0.14f),
                contentColor = accentColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.LocationSearching,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF071C18)
                )
                Text(
                    text = coordinatesText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF63706D),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (addressText != null) {
                    Text(
                        text = addressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF63706D),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
