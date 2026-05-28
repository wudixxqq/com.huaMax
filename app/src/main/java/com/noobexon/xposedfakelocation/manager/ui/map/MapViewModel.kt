package com.noobexon.xposedfakelocation.manager.ui.map

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noobexon.xposedfakelocation.R
import com.noobexon.xposedfakelocation.data.DEFAULT_MAP_ZOOM
import com.noobexon.xposedfakelocation.data.model.FavoriteLocation
import com.noobexon.xposedfakelocation.data.model.GpsNoiseLevel
import com.noobexon.xposedfakelocation.data.model.LocationTemplate
import com.noobexon.xposedfakelocation.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.UUID

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
        val addToFavoritesDialogState: DialogState = DialogState.Hidden,
        val addToTemplateDialogState: DialogState = DialogState.Hidden,
        val updateTemplateLocationDialogState: DialogState = DialogState.Hidden,
        val goToPointState: Pair<InputFieldState, InputFieldState> = InputFieldState() to InputFieldState(),
        val addToFavoritesState: FavoritesInputState = FavoritesInputState(),
        val addToTemplateName: InputFieldState = InputFieldState(),
        val addToTemplateDraft: LocationTemplate? = null,
        val templates: List<LocationTemplate> = emptyList()
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
                _uiState.update { it.copy(lastClickedLocation = geoPoint) }
            }
        }

        viewModelScope.launch {
            preferencesRepository.getLocationTemplatesFlow().collectLatest { templates ->
                _uiState.update { it.copy(templates = templates) }
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
        _uiState.update { it.copy(lastClickedLocation = geoPoint) }
        
        viewModelScope.launch {
            geoPoint?.let {
                preferencesRepository.saveLastClickedLocation(
                    it.latitude,
                    it.longitude
                )
            } ?: preferencesRepository.clearLastClickedLocation()
        }
    }

    fun addFavoriteLocation(favoriteLocation: FavoriteLocation) {
        viewModelScope.launch {
            preferencesRepository.addFavorite(favoriteLocation)
        }
    }

    fun addTemplateFromMarker(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            preferencesRepository.saveLocationTemplate(createTemplateFromCurrentSettings(name, latitude, longitude))
        }
    }

    fun addTemplate(template: LocationTemplate) {
        viewModelScope.launch {
            preferencesRepository.saveLocationTemplate(template)
        }
    }

    fun updateTemplateLocation(template: LocationTemplate, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            preferencesRepository.saveLocationTemplate(
                template.copy(
                    latitude = latitude,
                    longitude = longitude
                )
            )
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

    fun showAddToTemplateDialog() {
        val marker = _uiState.value.lastClickedLocation ?: return
        _uiState.update {
            it.copy(
                addToTemplateDialogState = DialogState.Visible,
                addToTemplateDraft = createTemplateFromCurrentSettings("", marker.latitude, marker.longitude)
            )
        }
    }

    fun hideAddToTemplateDialog() {
        _uiState.update {
            it.copy(
                addToTemplateDialogState = DialogState.Hidden,
                addToTemplateName = InputFieldState(),
                addToTemplateDraft = null
            )
        }
    }

    fun showUpdateTemplateLocationDialog() {
        _uiState.update { it.copy(updateTemplateLocationDialogState = DialogState.Visible) }
    }

    fun hideUpdateTemplateLocationDialog() {
        _uiState.update { it.copy(updateTemplateLocationDialogState = DialogState.Hidden) }
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

    fun updateAddToTemplateName(value: String) {
        _uiState.update {
            it.copy(
                addToTemplateName = InputFieldState(
                    value = value,
                    errorMessageRes = if (value.isBlank()) R.string.validation_name_required else null
                )
            )
        }
    }

    fun validateAndAddTemplate(onSuccess: (name: String, latitude: Double, longitude: Double) -> Unit) {
        val marker = _uiState.value.lastClickedLocation
        val name = _uiState.value.addToTemplateName.value
        val nameError = if (name.isBlank()) R.string.validation_name_required else null

        _uiState.update {
            it.copy(addToTemplateName = it.addToTemplateName.copy(errorMessageRes = nameError))
        }

        if (nameError == null && marker != null) {
            onSuccess(name, marker.latitude, marker.longitude)
        }
    }

    private fun createTemplateFromCurrentSettings(name: String, latitude: Double, longitude: Double): LocationTemplate {
        return LocationTemplate(
            id = UUID.randomUUID().toString(),
            name = name,
            latitude = latitude,
            longitude = longitude,
            useRandomize = preferencesRepository.getUseRandomize(),
            randomizeRadius = preferencesRepository.getRandomizeRadius(),
            useAccuracy = preferencesRepository.getUseAccuracy(),
            accuracy = preferencesRepository.getAccuracy(),
            useAltitude = preferencesRepository.getUseAltitude(),
            altitude = preferencesRepository.getAltitude(),
            useVerticalAccuracy = preferencesRepository.getUseVerticalAccuracy(),
            verticalAccuracy = preferencesRepository.getVerticalAccuracy(),
            useMeanSeaLevel = preferencesRepository.getUseMeanSeaLevel(),
            meanSeaLevel = preferencesRepository.getMeanSeaLevel(),
            useMeanSeaLevelAccuracy = preferencesRepository.getUseMeanSeaLevelAccuracy(),
            meanSeaLevelAccuracy = preferencesRepository.getMeanSeaLevelAccuracy(),
            useSpeed = preferencesRepository.getUseSpeed(),
            speed = preferencesRepository.getSpeed(),
            useSpeedAccuracy = preferencesRepository.getUseSpeedAccuracy(),
            speedAccuracy = preferencesRepository.getSpeedAccuracy(),
            useGpsNoise = preferencesRepository.getUseGpsNoise(),
            gpsNoiseLevel = GpsNoiseLevel.fromPreferenceValue(preferencesRepository.getGpsNoiseLevel())
        )
    }

    // Clear AddToFavorites inputs
    fun clearAddToFavoritesInputs() {
        _uiState.update { it.copy(addToFavoritesState = FavoritesInputState()) }
    }
    
    // Update map zoom level
    fun updateMapZoom(zoom: Double) {
        _uiState.update { it.copy(mapZoom = zoom) }
    }
}
