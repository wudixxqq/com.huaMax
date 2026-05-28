package com.noobexon.xposedfakelocation.manager.ui.templates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noobexon.xposedfakelocation.data.model.GpsNoiseLevel
import com.noobexon.xposedfakelocation.data.model.LocationTemplate
import com.noobexon.xposedfakelocation.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class TemplatesUiState(
    val templates: List<LocationTemplate> = emptyList(),
    val editingTemplate: LocationTemplate? = null,
    val isAddingTemplate: Boolean = false
)

class TemplatesViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = PreferencesRepository(application)

    private val _uiState = MutableStateFlow(TemplatesUiState())
    val uiState: StateFlow<TemplatesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.getLocationTemplatesFlow().collectLatest { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    fun addTemplate() {
        val currentLocation = preferencesRepository.getLastClickedLocation()
        _uiState.update {
            it.copy(
                isAddingTemplate = true,
                editingTemplate = LocationTemplate(
                    id = UUID.randomUUID().toString(),
                    name = "New template",
                    latitude = currentLocation?.latitude ?: 0.0,
                    longitude = currentLocation?.longitude ?: 0.0,
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
            )
        }
    }

    fun editTemplate(template: LocationTemplate) {
        _uiState.update { it.copy(editingTemplate = template, isAddingTemplate = false) }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editingTemplate = null, isAddingTemplate = false) }
    }

    fun saveTemplate(template: LocationTemplate) {
        viewModelScope.launch {
            preferencesRepository.saveLocationTemplate(template)
            dismissEditor()
        }
    }

    fun deleteTemplate(template: LocationTemplate) {
        viewModelScope.launch {
            preferencesRepository.removeLocationTemplate(template.id)
        }
    }
}
