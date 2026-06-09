package com.huaMax.manager.ui.map.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.huaMax.R
import com.huaMax.data.DEFAULT_MAP_ZOOM
import com.huaMax.data.WORLD_MAP_ZOOM
import com.huaMax.data.geo.CoordinateTransform
import com.huaMax.manager.ui.map.LoadingState
import com.huaMax.manager.ui.map.MapViewModel
import kotlinx.coroutines.flow.collect
import org.osmdroid.util.GeoPoint

@Composable
fun MapViewContainer(
    mapViewModel: MapViewModel
) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsStateWithLifecycle()

    val loadingState = uiState.loadingState
    val lastClickedLocation = uiState.lastClickedLocation
    val isPlaying = uiState.isPlaying
    val mapZoom = uiState.mapZoom

    val mapView = rememberAmapView(context)
    val amap = remember(mapView) { mapView.map }
    val selectedMarker = remember { mutableStateOf<Marker?>(null) }

    ConfigureAmap(amap)
    HandleCenterMapEvent(context, amap, mapViewModel)
    HandleGoToPointEvent(amap, mapViewModel)
    HandleMarkerUpdates(amap, selectedMarker.value, { selectedMarker.value = it }, lastClickedLocation)
    SetupMapClickListener(amap, mapViewModel, isPlaying)
    TrackCameraZoom(amap, mapViewModel)
    CenterMapOnUserLocation(context, amap, mapViewModel, lastClickedLocation, mapZoom)
    ManageMapViewLifecycle(mapView, amap)

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        if (loadingState == LoadingState.Loading) {
            LoadingSpinner()
        }
    }
}

@Composable
private fun rememberAmapView(context: Context): MapView {
    return remember {
        MapView(context).apply {
            onCreate(null)
        }
    }
}

@Composable
private fun ConfigureAmap(amap: AMap) {
    val context = LocalContext.current
    DisposableEffect(amap) {
        amap.uiSettings.isZoomControlsEnabled = false
        amap.uiSettings.isMyLocationButtonEnabled = false
        amap.uiSettings.isScaleControlsEnabled = false
        amap.uiSettings.isCompassEnabled = false
        amap.uiSettings.isRotateGesturesEnabled = false
        amap.uiSettings.isTiltGesturesEnabled = false

        if (hasLocationPermission(context)) {
            runCatching {
                amap.setMyLocationStyle(
                    MyLocationStyle()
                        .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                        .showMyLocation(true)
                )
                amap.isMyLocationEnabled = true
            }
        }

        onDispose {
            runCatching {
                amap.isMyLocationEnabled = false
            }
        }
    }
}

@Composable
private fun HandleCenterMapEvent(
    context: Context,
    amap: AMap,
    mapViewModel: MapViewModel
) {
    val userLocationNotAvailable = stringResource(R.string.toast_user_location_not_available)
    LaunchedEffect(Unit) {
        mapViewModel.centerMapEvent.collect {
            val userLocation = getLastKnownDeviceLocation(context)
            if (userLocation != null) {
                centerOnGeoPoint(amap, userLocation, mapViewModel)
            } else {
                Toast.makeText(context, userLocationNotAvailable, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun HandleGoToPointEvent(
    amap: AMap,
    mapViewModel: MapViewModel
) {
    LaunchedEffect(Unit) {
        mapViewModel.goToPointEvent.collect { geoPoint ->
            amap.animateCamera(CameraUpdateFactory.newLatLngZoom(geoPoint.toAmapLatLng(), DEFAULT_MAP_ZOOM.toFloat()))
            mapViewModel.updateClickedLocation(geoPoint)
        }
    }
}

@Composable
private fun HandleMarkerUpdates(
    amap: AMap,
    selectedMarker: Marker?,
    updateSelectedMarker: (Marker?) -> Unit,
    lastClickedLocation: GeoPoint?
) {
    LaunchedEffect(lastClickedLocation) {
        if (lastClickedLocation != null) {
            val latLng = lastClickedLocation.toAmapLatLng()
            if (selectedMarker == null) {
                updateSelectedMarker(
                    amap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .anchor(0.5f, 1.0f)
                    )
                )
            } else {
                selectedMarker.position = latLng
            }
            amap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        } else {
            selectedMarker?.remove()
            updateSelectedMarker(null)
        }
    }
}

@Composable
private fun SetupMapClickListener(
    amap: AMap,
    mapViewModel: MapViewModel,
    isPlaying: Boolean
) {
    DisposableEffect(amap, isPlaying) {
        amap.setOnMapClickListener { latLng ->
            if (!isPlaying) {
                mapViewModel.updateClickedLocation(latLng.toWgsGeoPoint())
            }
        }

        onDispose {
            amap.setOnMapClickListener(null)
        }
    }
}

@Composable
private fun TrackCameraZoom(
    amap: AMap,
    mapViewModel: MapViewModel
) {
    DisposableEffect(amap) {
        amap.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(cameraPosition: CameraPosition?) = Unit

            override fun onCameraChangeFinish(cameraPosition: CameraPosition?) {
                cameraPosition?.zoom?.let { mapViewModel.updateMapZoom(it.toDouble()) }
            }
        })

        onDispose {
            amap.setOnCameraChangeListener(null)
        }
    }
}

@Composable
private fun CenterMapOnUserLocation(
    context: Context,
    amap: AMap,
    mapViewModel: MapViewModel,
    lastClickedLocation: GeoPoint?,
    mapZoom: Double?
) {
    LaunchedEffect(amap, lastClickedLocation) {
        if (lastClickedLocation != null) {
            centerOnMarkerLocation(amap, lastClickedLocation, mapZoom, mapViewModel)
        } else {
            val lastKnown = getLastKnownDeviceLocation(context)
            if (lastKnown != null) {
                centerOnGeoPoint(amap, lastKnown, mapViewModel)
            } else {
                centerOnDefaultLocation(amap, mapViewModel)
            }
        }
    }
}

private fun centerOnMarkerLocation(
    amap: AMap,
    markerLocation: GeoPoint,
    mapZoom: Double?,
    mapViewModel: MapViewModel
) {
    val zoom = mapZoom ?: DEFAULT_MAP_ZOOM
    amap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerLocation.toAmapLatLng(), zoom.toFloat()))
    mapViewModel.updateMapZoom(zoom)
    mapViewModel.setLoadingFinished()
}

private fun centerOnGeoPoint(
    amap: AMap,
    point: GeoPoint,
    mapViewModel: MapViewModel
) {
    amap.moveCamera(CameraUpdateFactory.newLatLngZoom(point.toAmapLatLng(), DEFAULT_MAP_ZOOM.toFloat()))
    mapViewModel.updateUserLocation(point)
    mapViewModel.updateMapZoom(DEFAULT_MAP_ZOOM)
    mapViewModel.setLoadingFinished()
}

private fun centerOnDefaultLocation(
    amap: AMap,
    mapViewModel: MapViewModel
) {
    amap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), WORLD_MAP_ZOOM.toFloat()))
    mapViewModel.updateMapZoom(WORLD_MAP_ZOOM)
    mapViewModel.setLoadingFinished()
}

@Composable
private fun ManageMapViewLifecycle(
    mapView: MapView,
    amap: AMap
) {
    DisposableEffect(mapView, amap) {
        mapView.onResume()
        onDispose {
            runCatching { amap.isMyLocationEnabled = false }
            mapView.onPause()
            mapView.onDestroy()
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

private fun getLastKnownDeviceLocation(context: Context): GeoPoint? {
    if (!hasLocationPermission(context)) return null

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

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

private fun GeoPoint.toAmapLatLng(): LatLng {
    val gcj = CoordinateTransform.wgs84ToGcj02(latitude, longitude)
    return LatLng(gcj.latitude, gcj.longitude)
}

private fun LatLng.toWgsGeoPoint(): GeoPoint {
    val wgs = CoordinateTransform.gcj02ToWgs84(latitude, longitude)
    return GeoPoint(wgs.latitude, wgs.longitude)
}
