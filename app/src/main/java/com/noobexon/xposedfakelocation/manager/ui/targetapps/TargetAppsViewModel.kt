package com.noobexon.xposedfakelocation.manager.ui.targetapps

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noobexon.xposedfakelocation.data.MANAGER_APP_PACKAGE_NAME
import com.noobexon.xposedfakelocation.data.model.AppLocationProfile
import com.noobexon.xposedfakelocation.data.model.FavoriteLocation
import com.noobexon.xposedfakelocation.data.model.GpsNoiseLevel
import com.noobexon.xposedfakelocation.data.model.LocationTemplate
import com.noobexon.xposedfakelocation.data.model.toAppLocationProfile
import com.noobexon.xposedfakelocation.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TargetAppItem(
    val label: String,
    val packageName: String,
    val isSelected: Boolean = false,
    val profile: AppLocationProfile? = null
)

data class TargetAppsUiState(
    val apps: List<TargetAppItem> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val profiles: Map<String, AppLocationProfile> = emptyMap(),
    val locationTemplates: List<FavoriteLocation> = emptyList(),
    val templates: List<LocationTemplate> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val editingPackageName: String? = null
) {
    val filteredApps: List<TargetAppItem>
        get() {
            val query = searchQuery.trim()
            return if (query.isEmpty()) {
                apps
            } else {
                apps.filter {
                    it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                }
            }
        }
}

class TargetAppsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = PreferencesRepository(application)
    private val packageManager = application.packageManager

    private val _uiState = MutableStateFlow(TargetAppsUiState())
    val uiState: StateFlow<TargetAppsUiState> = _uiState.asStateFlow()

    init {
        observeProfiles()
        observeLocationTemplates()
        observeTemplates()
        loadInstalledApps()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleApp(packageName: String) {
        val existingProfile = _uiState.value.profiles[packageName]
        val nextProfile = existingProfile?.copy(enabled = !existingProfile.enabled)
            ?: createProfileFromCurrentSettings(packageName)

        viewModelScope.launch {
            preferencesRepository.saveAppLocationProfile(nextProfile)
            syncTargetApps()
        }
    }

    fun editApp(packageName: String) {
        if (_uiState.value.profiles[packageName] == null) {
            viewModelScope.launch {
                preferencesRepository.saveAppLocationProfile(createProfileFromCurrentSettings(packageName))
                syncTargetApps()
                _uiState.update { it.copy(editingPackageName = packageName) }
            }
            return
        }

        _uiState.update { it.copy(editingPackageName = packageName) }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editingPackageName = null) }
    }

    fun saveProfile(profile: AppLocationProfile) {
        viewModelScope.launch {
            preferencesRepository.saveAppLocationProfile(profile)
            syncTargetApps()
            _uiState.update { it.copy(editingPackageName = null) }
        }
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            preferencesRepository.getAppLocationProfilesFlow().collectLatest { profiles ->
                val selectedPackages = profiles.values
                    .filter { it.enabled }
                    .map { it.packageName }
                    .toSet()

                _uiState.update {
                    it.copy(
                        profiles = profiles,
                        selectedPackages = selectedPackages,
                        apps = it.apps.map { app ->
                            val profile = profiles[app.packageName]
                            app.copy(
                                isSelected = profile?.enabled == true,
                                profile = profile
                            )
                        }
                    )
                }
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val installedApps = withContext(Dispatchers.IO) {
                val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
                    .asSequence()
                    .map { it.activityInfo.applicationInfo }
                    .filter { it.packageName != MANAGER_APP_PACKAGE_NAME }
                    .distinctBy { it.packageName }
                    .map {
                        TargetAppItem(
                            label = it.loadLabel(packageManager).toString(),
                            packageName = it.packageName
                        )
                    }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
                    .toList()
            }

            _uiState.update { state ->
                state.copy(
                    apps = installedApps.map {
                        val profile = state.profiles[it.packageName]
                        it.copy(
                            isSelected = profile?.enabled == true,
                            profile = profile
                        )
                    },
                    isLoading = false
                )
            }
        }
    }

    private fun observeLocationTemplates() {
        viewModelScope.launch {
            preferencesRepository.getFavoritesFlow().collectLatest { favorites ->
                _uiState.update { it.copy(locationTemplates = favorites) }
            }
        }
    }

    private fun observeTemplates() {
        viewModelScope.launch {
            preferencesRepository.getLocationTemplatesFlow().collectLatest { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    private fun createProfileFromCurrentSettings(packageName: String): AppLocationProfile {
        val location = preferencesRepository.getLastClickedLocation()

        return AppLocationProfile(
            packageName = packageName,
            templateId = null,
            useCustomLocation = false,
            useCustomAdvancedSettings = false,
            latitude = location?.latitude ?: 0.0,
            longitude = location?.longitude ?: 0.0,
            enabled = true,
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

    private suspend fun syncTargetApps() {
        val selectedPackages = preferencesRepository.getAppLocationProfilesFlow()
            .firstOrNull()
            .orEmpty()
            .values
            .filter { it.enabled }
            .map { it.packageName }
            .toSet()
        preferencesRepository.saveTargetApps(selectedPackages)
    }
}
