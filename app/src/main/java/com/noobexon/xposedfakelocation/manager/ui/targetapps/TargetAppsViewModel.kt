package com.noobexon.xposedfakelocation.manager.ui.targetapps

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noobexon.xposedfakelocation.data.MANAGER_APP_PACKAGE_NAME
import com.noobexon.xposedfakelocation.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TargetAppItem(
    val label: String,
    val packageName: String,
    val isSelected: Boolean = false
)

data class TargetAppsUiState(
    val apps: List<TargetAppItem> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
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
        observeSelectedApps()
        loadInstalledApps()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleApp(packageName: String) {
        val nextSelectedPackages = _uiState.value.selectedPackages.toMutableSet().apply {
            if (!add(packageName)) remove(packageName)
        }

        _uiState.update {
            it.copy(
                selectedPackages = nextSelectedPackages,
                apps = it.apps.map { app ->
                    app.copy(isSelected = nextSelectedPackages.contains(app.packageName))
                }
            )
        }

        viewModelScope.launch {
            preferencesRepository.saveTargetApps(nextSelectedPackages)
        }
    }

    private fun observeSelectedApps() {
        viewModelScope.launch {
            preferencesRepository.getTargetAppsFlow().collectLatest { selectedPackages ->
                _uiState.update {
                    it.copy(
                        selectedPackages = selectedPackages,
                        apps = it.apps.map { app ->
                            app.copy(isSelected = selectedPackages.contains(app.packageName))
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
                        it.copy(isSelected = state.selectedPackages.contains(it.packageName))
                    },
                    isLoading = false
                )
            }
        }
    }
}
