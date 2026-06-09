package com.huaMax.manager.ui.map.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huaMax.R
import com.huaMax.data.DEFAULT_MAP_ZOOM
import com.huaMax.data.LOCATION_DETECTION_DELAY_MS
import com.huaMax.data.LOCATION_DETECTION_MAX_ATTEMPTS
import com.huaMax.data.WORLD_MAP_ZOOM
import com.huaMax.manager.ui.map.DialogState
import com.huaMax.manager.ui.map.LoadingState
import com.huaMax.manager.ui.map.MapViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

@Composable
fun MapViewContainer(
    mapViewModel: MapViewModel
) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsStateWithLifecycle()

    // Extract state from uiState
    val loadingState = uiState.loadingState
    val lastClickedLocation = uiState.lastClickedLocation
    val isPlaying = uiState.isPlaying
    val mapZoom = uiState.mapZoom

    // Remember MapView and overlays
    val mapView = rememberMapView(context)
    val userMarker = rememberUserMarker(mapView)
    val locationOverlay = rememberLocationOverlay(context, mapView)

    // Add the location overlay to the map
    AddLocationOverlayToMap(mapView, locationOverlay)

    // Handle map events and updates
    HandleCenterMapEvent(mapView, locationOverlay, mapViewModel)
    HandleGoToPointEvent(mapView, mapViewModel)
    HandleMarkerUpdates(mapView, userMarker, lastClickedLocation)
    SetupMapClickListener(mapView, mapViewModel, isPlaying)
    CenterMapOnUserLocation(mapView, locationOverlay, mapViewModel, lastClickedLocation, mapZoom)
    ManageMapViewLifecycle(mapView, locationOverlay)

    // Add MapListener to update zoom level
    DisposableEffect(mapView) {
        val mapListener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                // Optional: update map center if needed
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                // Update zoom state through proper ViewModel methods
                // This will be handled by the ViewModel's state update logic
                mapViewModel.updateMapZoom(mapView.zoomLevelDouble)
                return true
            }
        }
        mapView.addMapListener(mapListener)

        onDispose {
            mapView.removeMapListener(mapListener)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DisplayMapView(mapView)
        if (loadingState == LoadingState.Loading) {
            LoadingSpinner()
        }
    }
}

@Composable
private fun rememberMapView(context: Context): MapView {
    return remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setBuiltInZoomControls(false)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_MAP_ZOOM)
        }
    }
}

@Composable
private fun rememberUserMarker(mapView: MapView): Marker {
    return remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
    }
}

@Composable
private fun rememberLocationOverlay(context: Context, mapView: MapView): MyLocationNewOverlay {
    return remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
        }
    }
}

@Composable
private fun AddLocationOverlayToMap(
    mapView: MapView,
    locationOverlay: MyLocationNewOverlay
) {
    LaunchedEffect(Unit) {
        if (!mapView.overlays.contains(locationOverlay)) {
            mapView.overlays.add(locationOverlay)
        }
    }
}

@Composable
private fun HandleCenterMapEvent(
    mapView: MapView,
    locationOverlay: MyLocationNewOverlay,
    mapViewModel: MapViewModel
) {
    val context = LocalContext.current
    val userLocationNotAvailable = stringResource(R.string.toast_user_location_not_available)
    LaunchedEffect(Unit) {
        mapViewModel.centerMapEvent.collect {
            val userLocation = locationOverlay.myLocation
            if (userLocation != null) {
                mapView.controller.animateTo(userLocation)
            } else {
                Toast.makeText(context, userLocationNotAvailable, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun HandleGoToPointEvent(
    mapView: MapView,
    mapViewModel: MapViewModel
) {
    LaunchedEffect(Unit) {
        mapViewModel.goToPointEvent.collect { geoPoint ->
            mapView.controller.animateTo(geoPoint)
            mapViewModel.updateClickedLocation(geoPoint)
        }
    }
}

@Composable
private fun HandleMarkerUpdates(
    mapView: MapView,
    userMarker: Marker,
    lastClickedLocation: GeoPoint?,
) {
    LaunchedEffect(lastClickedLocation) {
        if (lastClickedLocation != null) {
            // Add the marker to the map if not already added
            if (!mapView.overlays.contains(userMarker)) {
                mapView.overlays.add(userMarker)
            }
            userMarker.position = lastClickedLocation
            mapView.controller.animateTo(lastClickedLocation)
            mapView.invalidate()
        } else {
            // Remove the marker from the map if it exists
            if (mapView.overlays.contains(userMarker)) {
                mapView.overlays.remove(userMarker)
                mapView.invalidate()
            }
        }
    }
}

@Composable
private fun SetupMapClickListener(
    mapView: MapView,
    mapViewModel: MapViewModel,
    isPlaying: Boolean
) {
    DisposableEffect(mapView, isPlaying) {
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (!isPlaying) {
                    mapViewModel.updateClickedLocation(p)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }

        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(mapEventsOverlay)

        onDispose {
            mapView.overlays.remove(mapEventsOverlay)
        }
    }
}

@Composable
private fun CenterMapOnUserLocation(
    mapView: MapView,
    locationOverlay: MyLocationNewOverlay,
    mapViewModel: MapViewModel,
    lastClickedLocation: GeoPoint?,
    mapZoom: Double?
) {
    val context = LocalContext.current
    LaunchedEffect(mapView, lastClickedLocation) {
        if (lastClickedLocation != null) {
            centerOnMarkerLocation(mapView, lastClickedLocation, mapZoom, mapViewModel)
        } else {
            val lastKnown = getLastKnownDeviceLocation(context)
            if (lastKnown != null) {
                centerOnGeoPoint(mapView, lastKnown, mapViewModel)
            } else if (!tryToFindAndCenterUserLocation(mapView, locationOverlay, mapViewModel)) {
                centerOnDefaultLocation(mapView, mapViewModel)
            }
        }
    }
}

/**
 * Centers the map on a specific marker location
 */
private suspend fun centerOnMarkerLocation(
    mapView: MapView,
    markerLocation: GeoPoint,
    mapZoom: Double?,
    mapViewModel: MapViewModel
) {
    val zoom = mapZoom ?: DEFAULT_MAP_ZOOM
    mapView.controller.setZoom(zoom)
    mapView.controller.animateTo(markerLocation)
    mapViewModel.updateMapZoom(zoom)
    mapViewModel.setLoadingFinished()
}

private fun centerOnGeoPoint(
    mapView: MapView,
    point: GeoPoint,
    mapViewModel: MapViewModel
) {
    mapView.controller.setZoom(DEFAULT_MAP_ZOOM)
    mapView.controller.setCenter(point)
    mapViewModel.updateUserLocation(point)
    mapViewModel.updateMapZoom(DEFAULT_MAP_ZOOM)
    mapViewModel.setLoadingFinished()
}

private fun getLastKnownDeviceLocation(context: Context): GeoPoint? {
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    if (!granted) return null

    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = try {
        lm.getProviders(true)
    } catch (e: SecurityException) {
        return null
    }
    var best: Location? = null
    for (provider in providers) {
        val loc = try {
            lm.getLastKnownLocation(provider)
        } catch (e: SecurityException) {
            null
        } ?: continue
        if (best == null || loc.time > best.time) best = loc
    }
    return best?.let { GeoPoint(it.latitude, it.longitude) }
}

/**
 * Attempts to find and center on the user's current location
 * @return true if user location was found, false otherwise
 */
private suspend fun tryToFindAndCenterUserLocation(
    mapView: MapView,
    locationOverlay: MyLocationNewOverlay,
    mapViewModel: MapViewModel
): Boolean {
    // Attempt to find user location within a timeout period
    repeat(LOCATION_DETECTION_MAX_ATTEMPTS) {
        val userLocation = locationOverlay.myLocation
        if (userLocation != null) {
            mapViewModel.updateUserLocation(userLocation)
            mapView.controller.setZoom(DEFAULT_MAP_ZOOM)
            mapView.controller.animateTo(userLocation)
            mapViewModel.updateMapZoom(DEFAULT_MAP_ZOOM)
            mapViewModel.setLoadingFinished()
            return true
        }
        delay(LOCATION_DETECTION_DELAY_MS)
    }
    return false
}

/**
 * Centers the map on a default world location when user location can't be found
 */
private fun centerOnDefaultLocation(
    mapView: MapView,
    mapViewModel: MapViewModel
) {
    // If location is not available after timeout, set default location
    mapView.controller.setZoom(WORLD_MAP_ZOOM)
    mapView.controller.setCenter(GeoPoint(0.0, 0.0))
    mapViewModel.updateMapZoom(WORLD_MAP_ZOOM)
    mapViewModel.setLoadingFinished()
}

@Composable
private fun ManageMapViewLifecycle(
    mapView: MapView,
    locationOverlay: MyLocationNewOverlay
) {
    DisposableEffect(mapView, locationOverlay) {
        mapView.onResume()
        locationOverlay.enableMyLocation()
        onDispose {
            locationOverlay.disableMyLocation()
            mapView.onPause()
        }
    }
}

@Composable
private fun LoadingSpinner() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.map_updating),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DisplayMapView(mapView: MapView) {
    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}
