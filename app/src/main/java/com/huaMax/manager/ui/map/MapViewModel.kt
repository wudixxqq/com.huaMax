package com.huaMax.manager.ui.map

import android.app.Application
import android.location.Address
import android.location.Geocoder
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.GeocodeAddress
import com.amap.api.services.geocoder.GeocodeQuery
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.poisearch.PoiSearch
import com.huaMax.R
import com.huaMax.data.geo.CoordinateTransform
import com.huaMax.data.model.FavoriteLocation
import com.huaMax.data.repository.PreferencesRepository
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

/**
 * Sealed classes to represent different dialog states
 */
sealed class DialogState {
    object Hidden : DialogState()
    object Visible : DialogState()
}

/**
 * Sealed class to represent different loading states
 */
sealed class LoadingState {
    object Loading : LoadingState()
    object Loaded : LoadingState()
}

data class PlaceSearchResult(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * ViewModel for the Map screen that manages map-related state and operations.
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = PreferencesRepository(application)

    /**
     * Represents field input state with value and validation error message
     */
    data class InputFieldState(val value: String = "", @StringRes val errorMessageRes: Int? = null)

    /**
     * Represents the UI state for the favorites input dialog
     */
    data class FavoritesInputState(
        val name: InputFieldState = InputFieldState(),
        val latitude: InputFieldState = InputFieldState(),
        val longitude: InputFieldState = InputFieldState()
    )

    /**
     * Represents the complete UI state for the Map screen
     */
    data class MapUiState(
        val isPlaying: Boolean = false,
        val lastClickedLocation: GeoPoint? = null,
        val userLocation: GeoPoint? = null,
        val loadingState: LoadingState = LoadingState.Loading,
        val mapZoom: Double? = null,
        val goToPointDialogState: DialogState = DialogState.Hidden,
        val addToFavoritesState: FavoritesInputState = FavoritesInputState(),
        val addToFavoritesDialogState: DialogState = DialogState.Hidden,
        val goToPointState: Pair<InputFieldState, InputFieldState> = InputFieldState() to InputFieldState(),
        val placeSearchQuery: String = "",
        val isPlaceSearchLoading: Boolean = false,
        val placeSearchResults: List<PlaceSearchResult> = emptyList(),
        @StringRes val placeSearchErrorMessageRes: Int? = null,
        val selectedLocationAddress: String? = null,
        val isSelectedLocationAddressLoading: Boolean = false,
        @StringRes val selectedLocationAddressMessageRes: Int? = null,
    ) {
        val isFabClickable: Boolean
            get() = lastClickedLocation != null
    }

    // Private mutable state
    private val _uiState = MutableStateFlow(MapUiState())
    
    // Public immutable state
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // Events
    private val _goToPointEvent = MutableSharedFlow<GeoPoint>()
    val goToPointEvent: SharedFlow<GeoPoint> = _goToPointEvent.asSharedFlow()

    private val _centerMapEvent = MutableSharedFlow<Unit>()
    val centerMapEvent: SharedFlow<Unit> = _centerMapEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            // Load initial isPlaying state
            preferencesRepository.getIsPlayingFlow().collectLatest { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        }
        
        viewModelScope.launch {
            // Load initial lastClickedLocation
            preferencesRepository.getLastClickedLocationFlow().collectLatest { location ->
                val geoPoint = location?.let { GeoPoint(it.latitude, it.longitude) }
                _uiState.update {
                    it.copy(
                        lastClickedLocation = geoPoint,
                        selectedLocationAddress = null,
                        isSelectedLocationAddressLoading = false,
                        selectedLocationAddressMessageRes = null
                    )
                }
                if (geoPoint != null) {
                    resolveSelectedLocationAddress(geoPoint)
                }
            }
        }
    }

    fun togglePlaying() {
        val currentIsPlaying = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = currentIsPlaying) }
        
        viewModelScope.launch {
            preferencesRepository.saveIsPlaying(currentIsPlaying)
        }
    }

    fun updateUserLocation(location: GeoPoint) {
        _uiState.update { it.copy(userLocation = location) }
    }

    fun updateClickedLocation(geoPoint: GeoPoint?) {
        _uiState.update {
            it.copy(
                lastClickedLocation = geoPoint,
                selectedLocationAddress = null,
                isSelectedLocationAddressLoading = false,
                selectedLocationAddressMessageRes = null
            )
        }
        
        viewModelScope.launch {
            geoPoint?.let {
                preferencesRepository.saveLastClickedLocation(
                    it.latitude,
                    it.longitude
                )
                resolveSelectedLocationAddress(it)
            } ?: preferencesRepository.clearLastClickedLocation()
        }
    }

    fun addFavoriteLocation(favoriteLocation: FavoriteLocation) {
        viewModelScope.launch {
            preferencesRepository.addFavorite(favoriteLocation)
        }
    }

    // Update specific fields in the FavoritesInputState
    fun updateAddToFavoritesField(fieldName: String, newValue: String) {
        val currentState = _uiState.value.addToFavoritesState
        val errorMessageRes = when (fieldName) {
            "name" -> if (newValue.isBlank()) R.string.validation_name_required else null
            "latitude" -> validateInput(newValue, -90.0..90.0, R.string.validation_latitude_range)
            "longitude" -> validateInput(newValue, -180.0..180.0, R.string.validation_longitude_range)
            else -> null
        }

        val updatedState = when (fieldName) {
            "name" -> currentState.copy(name = currentState.name.copy(value = newValue, errorMessageRes = errorMessageRes))
            "latitude" -> currentState.copy(latitude = currentState.latitude.copy(value = newValue, errorMessageRes = errorMessageRes))
            "longitude" -> currentState.copy(longitude = currentState.longitude.copy(value = newValue, errorMessageRes = errorMessageRes))
            else -> currentState
        }
        
        _uiState.update { it.copy(addToFavoritesState = updatedState) }
    }

    // Go to point logic
    fun goToPoint(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _goToPointEvent.emit(GeoPoint(latitude, longitude))
        }
    }

    fun updatePlaceSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                placeSearchQuery = query,
                placeSearchErrorMessageRes = null
            )
        }
    }

    fun clearPlaceSearch() {
        _uiState.update {
            it.copy(
                placeSearchQuery = "",
                placeSearchResults = emptyList(),
                placeSearchErrorMessageRes = null,
                isPlaceSearchLoading = false
            )
        }
    }

    fun searchPlace() {
        val requestedQuery = _uiState.value.placeSearchQuery.trim()
        if (requestedQuery.isBlank()) {
            _uiState.update {
                it.copy(
                    placeSearchResults = emptyList(),
                    placeSearchErrorMessageRes = R.string.map_search_empty
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isPlaceSearchLoading = true,
                placeSearchResults = emptyList(),
                placeSearchErrorMessageRes = null
            )
        }

        viewModelScope.launch {
            val results = runCatching {
                withContext(Dispatchers.IO) {
                    searchPlacesByName(requestedQuery)
                }
            }

            if (_uiState.value.placeSearchQuery.trim() != requestedQuery) return@launch

            results
                .onSuccess { places ->
                    _uiState.update {
                        it.copy(
                            isPlaceSearchLoading = false,
                            placeSearchResults = places,
                            placeSearchErrorMessageRes = if (places.isEmpty()) {
                                R.string.map_search_no_results
                            } else {
                                null
                            }
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isPlaceSearchLoading = false,
                            placeSearchResults = emptyList(),
                            placeSearchErrorMessageRes = R.string.map_search_error
                        )
                    }
                }
        }
    }

    fun selectPlaceSearchResult(result: PlaceSearchResult) {
        _uiState.update {
            it.copy(
                placeSearchQuery = result.name,
                placeSearchResults = emptyList(),
                placeSearchErrorMessageRes = null,
                isPlaceSearchLoading = false
            )
        }
        goToPoint(result.latitude, result.longitude)
    }

    // Update specific fields in the GoToPointDialog state
    fun updateGoToPointField(fieldName: String, newValue: String) {
        val (latitudeField, longitudeField) = _uiState.value.goToPointState
        val updatedGoToPointState = when (fieldName) {
            "latitude" -> latitudeField.copy(value = newValue) to longitudeField
            "longitude" -> latitudeField to longitudeField.copy(value = newValue)
            else -> latitudeField to longitudeField
        }
        
        _uiState.update { it.copy(goToPointState = updatedGoToPointState) }
    }

    // Center map
    fun triggerCenterMapEvent() {
        viewModelScope.launch {
            _centerMapEvent.emit(Unit)
        }
    }

    fun setLoadingStarted() {
        _uiState.update { it.copy(loadingState = LoadingState.Loading) }
    }

    // Set loading finished
    fun setLoadingFinished() {
        _uiState.update { it.copy(loadingState = LoadingState.Loaded) }
    }

    // Dialog show/hide logic
    fun showGoToPointDialog() { 
        _uiState.update { it.copy(goToPointDialogState = DialogState.Visible) }
    }
    
    fun hideGoToPointDialog() {
        _uiState.update { it.copy(goToPointDialogState = DialogState.Hidden) }
        clearGoToPointInputs()
    }

    fun showAddToFavoritesDialog() { 
        _uiState.update { it.copy(addToFavoritesDialogState = DialogState.Visible) }
    }
    
    fun hideAddToFavoritesDialog() {
        _uiState.update { it.copy(addToFavoritesDialogState = DialogState.Hidden) }
        clearAddToFavoritesInputs()
    }

    // Helper for input validation
    private fun validateInput(
        input: String, range: ClosedRange<Double>, @StringRes errorMessageRes: Int
    ): Int? {
        val value = input.toDoubleOrNull()
        return if (value == null || value !in range) errorMessageRes else null
    }

    // Validate GoToPoint inputs
    fun validateAndGo(onSuccess: (latitude: Double, longitude: Double) -> Unit) {
        val (latField, lonField) = _uiState.value.goToPointState
        val latitudeError = validateInput(latField.value, -90.0..90.0, R.string.validation_latitude_range)
        val longitudeError = validateInput(lonField.value, -180.0..180.0, R.string.validation_longitude_range)

        val updatedGoToPointState = latField.copy(errorMessageRes = latitudeError) to lonField.copy(errorMessageRes = longitudeError)
        _uiState.update { it.copy(goToPointState = updatedGoToPointState) }

        if (latitudeError == null && longitudeError == null) {
            onSuccess(latField.value.toDouble(), lonField.value.toDouble())
        }
    }

    // Clear GoToPoint inputs
    fun clearGoToPointInputs() {
        _uiState.update { 
            it.copy(goToPointState = InputFieldState() to InputFieldState())
        }
    }

    // Prefill AddToFavorites latitude/longitude with marker values (if available)
    fun prefillCoordinatesFromMarker(latitude: Double?, longitude: Double?) {
        if (latitude != null && longitude != null) {
            val latField = InputFieldState(value = latitude.toString())
            val lngField = InputFieldState(value = longitude.toString())
            
            _uiState.update { currentState ->
                val favState = currentState.addToFavoritesState
                currentState.copy(
                    addToFavoritesState = favState.copy(
                        latitude = latField,
                        longitude = lngField
                    )
                )
            }
        }
    }

    // Validate and add favorite location
    fun validateAndAddFavorite(onSuccess: (name: String, latitude: Double, longitude: Double) -> Unit) {
        val currentState = _uiState.value.addToFavoritesState

        val latitudeError = validateInput(currentState.latitude.value, -90.0..90.0, R.string.validation_latitude_range)
        val longitudeError = validateInput(currentState.longitude.value, -180.0..180.0, R.string.validation_longitude_range)
        val nameError = if (currentState.name.value.isBlank()) R.string.validation_name_required else null

        val updatedState = currentState.copy(
            name = currentState.name.copy(errorMessageRes = nameError),
            latitude = currentState.latitude.copy(errorMessageRes = latitudeError),
            longitude = currentState.longitude.copy(errorMessageRes = longitudeError)
        )
        
        _uiState.update { it.copy(addToFavoritesState = updatedState) }

        if (nameError == null && latitudeError == null && longitudeError == null) {
            onSuccess(currentState.name.value, currentState.latitude.value.toDouble(), currentState.longitude.value.toDouble())
        }
    }

    // Clear AddToFavorites inputs
    fun clearAddToFavoritesInputs() {
        _uiState.update { it.copy(addToFavoritesState = FavoritesInputState()) }
    }
    
    // Update map zoom level
    fun updateMapZoom(zoom: Double) {
        _uiState.update { it.copy(mapZoom = zoom) }
    }

    private fun searchPlacesByName(query: String): List<PlaceSearchResult> {
        var failure: Throwable? = null

        val amapResults = try {
            searchWithAmap(query)
        } catch (e: Exception) {
            failure = e
            emptyList()
        }
        if (amapResults.isNotEmpty()) return amapResults

        val deviceResults = try {
            searchWithDeviceGeocoder(query)
        } catch (e: Exception) {
            failure = e
            emptyList()
        }
        if (deviceResults.isNotEmpty()) return deviceResults

        failure?.let { throw IOException("Place search failed", it) }
        return emptyList()
    }

    private fun resolveSelectedLocationAddress(geoPoint: GeoPoint) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSelectedLocationAddressLoading = true,
                    selectedLocationAddress = null,
                    selectedLocationAddressMessageRes = null
                )
            }

            val address = runCatching {
                withContext(Dispatchers.IO) {
                    reverseGeocodeAddress(geoPoint)
                }
            }.getOrNull()

            val currentLocation = _uiState.value.lastClickedLocation
            if (
                currentLocation?.latitude == geoPoint.latitude &&
                currentLocation.longitude == geoPoint.longitude
            ) {
                _uiState.update {
                    it.copy(
                        isSelectedLocationAddressLoading = false,
                        selectedLocationAddress = address,
                        selectedLocationAddressMessageRes = if (address.isNullOrBlank()) {
                            R.string.map_status_address_unavailable
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    private fun reverseGeocodeAddress(geoPoint: GeoPoint): String? {
        return reverseWithAmap(geoPoint)
            ?: reverseWithDeviceGeocoder(geoPoint)
    }

    @Suppress("DEPRECATION")
    private fun reverseWithDeviceGeocoder(geoPoint: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(getApplication<Application>().applicationContext, Locale.getDefault())
        return geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
            .orEmpty()
            .firstOrNull()
            ?.toDisplayAddress()
    }

    private fun reverseWithAmap(geoPoint: GeoPoint): String? {
        val context = getApplication<Application>().applicationContext
        val geocodeSearch = GeocodeSearch(context)
        val query = RegeocodeQuery(
            LatLonPoint(geoPoint.latitude, geoPoint.longitude),
            200f,
            GeocodeSearch.GPS
        ).apply {
            extensions = GeocodeSearch.EXTENSIONS_ALL
        }

        return geocodeSearch.getFromLocation(query)
            ?.formatAddress
            ?.takeIf { it.isNotBlank() }
    }

    private fun searchWithAmap(query: String): List<PlaceSearchResult> {
        val context = getApplication<Application>().applicationContext
        val poiQuery = PoiSearch.Query(query, "", "").apply {
            pageSize = PLACE_SEARCH_RESULT_LIMIT
            pageNum = 0
            cityLimit = false
            setQueryLanguage(PoiSearch.CHINESE)
        }
        val poiSearch = PoiSearch(context, poiQuery).apply {
            language = PoiSearch.CHINESE
        }

        val poiResults = poiSearch.searchPOI()
            ?.pois
            .orEmpty()
            .mapNotNull { it.toPlaceSearchResultFromAmap(query) }
            .distinctBy { "${it.latitude}:${it.longitude}" }
            .take(PLACE_SEARCH_RESULT_LIMIT)

        if (poiResults.isNotEmpty()) return poiResults

        return GeocodeSearch(context)
            .getFromLocationName(GeocodeQuery(query, ""))
            .orEmpty()
            .mapNotNull { it.toPlaceSearchResultFromAmap(query) }
            .distinctBy { "${it.latitude}:${it.longitude}" }
            .take(PLACE_SEARCH_RESULT_LIMIT)
    }

    @Suppress("DEPRECATION")
    private fun searchWithDeviceGeocoder(query: String): List<PlaceSearchResult> {
        if (!Geocoder.isPresent()) return emptyList()
        val geocoder = Geocoder(getApplication<Application>().applicationContext, Locale.getDefault())
        return geocoder.getFromLocationName(query, PLACE_SEARCH_RESULT_LIMIT)
            .orEmpty()
            .mapNotNull { it.toPlaceSearchResult(query) }
            .distinctBy { "${it.latitude}:${it.longitude}" }
    }

    private fun Address.toPlaceSearchResult(fallbackName: String): PlaceSearchResult? {
        if (!hasLatitude() || !hasLongitude()) return null
        val addressLine = runCatching { getAddressLine(0) }.getOrNull().orEmpty()
        val name = listOf(featureName, thoroughfare, subLocality, locality, adminArea, countryName)
            .firstOrNull { !it.isNullOrBlank() }
            ?: addressLine.substringBefore(",").takeIf { it.isNotBlank() }
            ?: fallbackName

        return PlaceSearchResult(
            name = name,
            address = addressLine.ifBlank { "$latitude, $longitude" },
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun PoiItem.toPlaceSearchResultFromAmap(fallbackName: String): PlaceSearchResult? {
        val point = latLonPoint ?: return null
        val wgs = point.toWgs84()
        val name = title?.takeIf { it.isNotBlank() } ?: fallbackName
        val address = listOfNotNull(provinceName, cityName, adName, snippet)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("")
            .ifBlank { "${wgs.latitude}, ${wgs.longitude}" }

        return PlaceSearchResult(
            name = name,
            address = address,
            latitude = wgs.latitude,
            longitude = wgs.longitude
        )
    }

    private fun GeocodeAddress.toPlaceSearchResultFromAmap(fallbackName: String): PlaceSearchResult? {
        val point = latLonPoint ?: return null
        val wgs = point.toWgs84()
        val address = formatAddress?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(province, city, district, township, neighborhood, building)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("")
        val name = address?.takeIf { it.isNotBlank() }
            ?.substringAfterLast("省")
            ?.substringAfterLast("市")
            ?.takeIf { it.isNotBlank() }
            ?: fallbackName

        return PlaceSearchResult(
            name = name,
            address = address?.ifBlank { "${wgs.latitude}, ${wgs.longitude}" } ?: "${wgs.latitude}, ${wgs.longitude}",
            latitude = wgs.latitude,
            longitude = wgs.longitude
        )
    }

    private fun LatLonPoint.toWgs84(): GeoPoint {
        val wgs = CoordinateTransform.gcj02ToWgs84(latitude, longitude)
        return GeoPoint(wgs.latitude, wgs.longitude)
    }

    private fun Address.toDisplayAddress(): String? {
        val addressLine = runCatching { getAddressLine(0) }.getOrNull().orEmpty()
        if (addressLine.isNotBlank()) return addressLine

        return listOfNotNull(
            featureName,
            thoroughfare,
            subLocality,
            locality,
            subAdminArea,
            adminArea,
            countryName
        )
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
            .takeIf { it.isNotBlank() }
    }

    private companion object {
        const val PLACE_SEARCH_RESULT_LIMIT = 5
    }
}
