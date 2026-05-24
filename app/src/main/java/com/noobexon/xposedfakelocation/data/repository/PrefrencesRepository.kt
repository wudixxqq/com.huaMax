// PreferencesRepository.kt
package com.noobexon.xposedfakelocation.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.noobexon.xposedfakelocation.data.*
import com.noobexon.xposedfakelocation.data.model.AppLocationProfile
import com.noobexon.xposedfakelocation.data.model.FavoriteLocation
import com.noobexon.xposedfakelocation.data.model.GpsNoiseLevel
import com.noobexon.xposedfakelocation.data.model.LastClickedLocation
import com.noobexon.xposedfakelocation.data.model.LocationTemplate
import com.noobexon.xposedfakelocation.data.model.OverrideState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SHARED_PREFS_FILE)

class PreferencesRepository(private val context: Context) {
    private val tag = "PreferencesRepository"
    
    // Legacy SharedPreferences for Xposed Module compatibility
    @SuppressLint("WorldReadableFiles")
    private val sharedPrefs = try {
        context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_WORLD_READABLE)
    } catch (e: SecurityException) {
        Log.w(tag, "MODE_WORLD_READABLE not available: ${e.message}")
        context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
    }

    private val gson = Gson()

    // DataStore preference keys
    private object PreferenceKeys {
        val IS_PLAYING = booleanPreferencesKey(KEY_IS_PLAYING)
        val LAST_CLICKED_LOCATION = stringPreferencesKey(KEY_LAST_CLICKED_LOCATION)
        val USE_ACCURACY = booleanPreferencesKey(KEY_USE_ACCURACY)
        val ACCURACY = doublePreferencesKey(KEY_ACCURACY)
        val USE_ALTITUDE = booleanPreferencesKey(KEY_USE_ALTITUDE)
        val ALTITUDE = doublePreferencesKey(KEY_ALTITUDE)
        val USE_RANDOMIZE = booleanPreferencesKey(KEY_USE_RANDOMIZE)
        val RANDOMIZE_RADIUS = doublePreferencesKey(KEY_RANDOMIZE_RADIUS)
        val USE_GPS_NOISE = booleanPreferencesKey(KEY_USE_GPS_NOISE)
        val GPS_NOISE_LEVEL = stringPreferencesKey(KEY_GPS_NOISE_LEVEL)
        val USE_VERTICAL_ACCURACY = booleanPreferencesKey(KEY_USE_VERTICAL_ACCURACY)
        val VERTICAL_ACCURACY = floatPreferencesKey(KEY_VERTICAL_ACCURACY)
        val USE_MEAN_SEA_LEVEL = booleanPreferencesKey(KEY_USE_MEAN_SEA_LEVEL)
        val MEAN_SEA_LEVEL = doublePreferencesKey(KEY_MEAN_SEA_LEVEL)
        val USE_MEAN_SEA_LEVEL_ACCURACY = booleanPreferencesKey(KEY_USE_MEAN_SEA_LEVEL_ACCURACY)
        val MEAN_SEA_LEVEL_ACCURACY = floatPreferencesKey(KEY_MEAN_SEA_LEVEL_ACCURACY)
        val USE_SPEED = booleanPreferencesKey(KEY_USE_SPEED)
        val SPEED = floatPreferencesKey(KEY_SPEED)
        val USE_SPEED_ACCURACY = booleanPreferencesKey(KEY_USE_SPEED_ACCURACY)
        val SPEED_ACCURACY = floatPreferencesKey(KEY_SPEED_ACCURACY)
        val FAVORITES = stringPreferencesKey(KEY_FAVORITES)
        val TARGET_APPS = stringPreferencesKey(KEY_TARGET_APPS)
        val APP_LOCATION_PROFILES = stringPreferencesKey(KEY_APP_LOCATION_PROFILES)
        val LOCATION_TEMPLATES = stringPreferencesKey(KEY_LOCATION_TEMPLATES)
        val HIDE_FAKE_LOCATION_TOAST = booleanPreferencesKey(KEY_HIDE_FAKE_LOCATION_TOAST)
        val USE_INAPP_TARGET_APPS = booleanPreferencesKey(KEY_USE_INAPP_TARGET_APPS)
        val ENABLE_BROADCAST_CONTROL = booleanPreferencesKey(KEY_ENABLE_BROADCAST_CONTROL)
    }

    // Generic helper for DataStore flows with error handling
    private fun <T> getPreferenceFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(tag, "Error reading preferences: ${exception.message}")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultValue
            }
    }

    // Helper to write both to DataStore and legacy SharedPreferences
    private suspend inline fun <reified T> savePreference(
        key: Preferences.Key<T>,
        value: T,
        sharedPrefsKey: String,
        sharedPrefsValue: Any
    ) {
        try {
            // Save to DataStore
            context.dataStore.edit { preferences ->
                preferences[key] = value
            }
            
            // Save to legacy SharedPreferences for Xposed Module
            when (value) {
                is Boolean -> sharedPrefs.edit().putBoolean(sharedPrefsKey, value).apply()
                is String -> sharedPrefs.edit().putString(sharedPrefsKey, value).apply()
                is Float -> sharedPrefs.edit().putFloat(sharedPrefsKey, value).apply()
                is Double -> {
                    val bits = java.lang.Double.doubleToRawLongBits(value)
                    sharedPrefs.edit().putLong(sharedPrefsKey, bits).apply()
                }
                is Long -> sharedPrefs.edit().putLong(sharedPrefsKey, value).apply()
                is Int -> sharedPrefs.edit().putInt(sharedPrefsKey, value).apply()
            }
            
            Log.d(tag, "Saved $sharedPrefsKey: $value")
        } catch (e: Exception) {
            Log.e(tag, "Error saving preference $sharedPrefsKey: ${e.message}")
        }
    }

    // Is Playing
    fun getIsPlayingFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.IS_PLAYING, DEFAULT_USE_ACCURACY)
    }
    
    suspend fun saveIsPlaying(isPlaying: Boolean) {
        savePreference(PreferenceKeys.IS_PLAYING, isPlaying, KEY_IS_PLAYING, isPlaying)
    }
    
    // For backward compatibility
    fun getIsPlaying(): Boolean {
        return sharedPrefs.getBoolean(KEY_IS_PLAYING, false)
    }

    // Last Clicked Location
    fun getLastClickedLocationFlow(): Flow<LastClickedLocation?> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(tag, "Error reading preferences: ${exception.message}")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val json = preferences[PreferenceKeys.LAST_CLICKED_LOCATION]
                if (json != null) {
                    try {
                        gson.fromJson(json, LastClickedLocation::class.java)
                    } catch (e: JsonSyntaxException) {
                        Log.e(tag, "Error parsing LastClickedLocation: ${e.message}")
                        null
                    }
                } else {
                    null
                }
            }
    }
    
    suspend fun saveLastClickedLocation(latitude: Double, longitude: Double) {
        try {
            val location = LastClickedLocation(latitude, longitude)
            val json = gson.toJson(location)
            savePreference(PreferenceKeys.LAST_CLICKED_LOCATION, json, KEY_LAST_CLICKED_LOCATION, json)
        } catch (e: Exception) {
            Log.e(tag, "Error saving LastClickedLocation: ${e.message}")
        }
    }
    
    // For backward compatibility
    fun getLastClickedLocation(): LastClickedLocation? {
        val json = sharedPrefs.getString(KEY_LAST_CLICKED_LOCATION, null)
        return if (json != null) {
            try {
                gson.fromJson(json, LastClickedLocation::class.java)
            } catch (e: JsonSyntaxException) {
                Log.e(tag, "Error parsing LastClickedLocation: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    suspend fun clearLastClickedLocation() {
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(PreferenceKeys.LAST_CLICKED_LOCATION)
            }
            
            sharedPrefs.edit()
                .remove(KEY_LAST_CLICKED_LOCATION)
                .apply()
                
            saveIsPlaying(false)
            Log.d(tag, "Cleared 'LastClickedLocation' from preferences and set 'IsPlaying' to false")
        } catch (e: Exception) {
            Log.e(tag, "Error clearing LastClickedLocation: ${e.message}")
        }
    }

    // Use Accuracy
    fun getUseAccuracyFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.USE_ACCURACY, DEFAULT_USE_ACCURACY)
    }
    
    suspend fun saveUseAccuracy(useAccuracy: Boolean) {
        savePreference(PreferenceKeys.USE_ACCURACY, useAccuracy, KEY_USE_ACCURACY, useAccuracy)
    }
    
    // For backward compatibility
    fun getUseAccuracy(): Boolean {
        return sharedPrefs.getBoolean(KEY_USE_ACCURACY, DEFAULT_USE_ACCURACY)
    }

    // Accuracy
    fun getAccuracyFlow(): Flow<Double> {
        return getPreferenceFlow(PreferenceKeys.ACCURACY, DEFAULT_ACCURACY)
    }
    
    suspend fun saveAccuracy(accuracy: Double) {
        savePreference(PreferenceKeys.ACCURACY, accuracy, KEY_ACCURACY, accuracy)
    }
    
    // For backward compatibility
    fun getAccuracy(): Double {
        val bits = sharedPrefs.getLong(KEY_ACCURACY, java.lang.Double.doubleToRawLongBits(DEFAULT_ACCURACY))
        return java.lang.Double.longBitsToDouble(bits)
    }

    // Use Altitude
    fun getUseAltitudeFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.USE_ALTITUDE, DEFAULT_USE_ALTITUDE)
    }
    
    suspend fun saveUseAltitude(useAltitude: Boolean) {
        savePreference(PreferenceKeys.USE_ALTITUDE, useAltitude, KEY_USE_ALTITUDE, useAltitude)
    }
    
    // For backward compatibility
    fun getUseAltitude(): Boolean {
        return sharedPrefs.getBoolean(KEY_USE_ALTITUDE, DEFAULT_USE_ALTITUDE)
    }

    // Altitude
    fun getAltitudeFlow(): Flow<Double> {
        return getPreferenceFlow(PreferenceKeys.ALTITUDE, DEFAULT_ALTITUDE)
    }
    
    suspend fun saveAltitude(altitude: Double) {
        savePreference(PreferenceKeys.ALTITUDE, altitude, KEY_ALTITUDE, altitude)
    }
    
    // For backward compatibility
    fun getAltitude(): Double {
        val bits = sharedPrefs.getLong(KEY_ALTITUDE, java.lang.Double.doubleToRawLongBits(DEFAULT_ALTITUDE))
        return java.lang.Double.longBitsToDouble(bits)
    }

    // Use Randomize
    fun getUseRandomizeFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.USE_RANDOMIZE, DEFAULT_USE_RANDOMIZE)
    }
    
    suspend fun saveUseRandomize(randomize: Boolean) {
        savePreference(PreferenceKeys.USE_RANDOMIZE, randomize, KEY_USE_RANDOMIZE, randomize)
    }
    
    // For backward compatibility
    fun getUseRandomize(): Boolean {
        return sharedPrefs.getBoolean(KEY_USE_RANDOMIZE, DEFAULT_USE_RANDOMIZE)
    }

    // Randomize Radius
    fun getRandomizeRadiusFlow(): Flow<Double> {
        return getPreferenceFlow(PreferenceKeys.RANDOMIZE_RADIUS, DEFAULT_RANDOMIZE_RADIUS)
    }
    
    suspend fun saveRandomizeRadius(radius: Double) {
        savePreference(PreferenceKeys.RANDOMIZE_RADIUS, radius, KEY_RANDOMIZE_RADIUS, radius)
    }
    
    // For backward compatibility
    fun getRandomizeRadius(): Double {
        val bits = sharedPrefs.getLong(
            KEY_RANDOMIZE_RADIUS,
            java.lang.Double.doubleToRawLongBits(DEFAULT_RANDOMIZE_RADIUS)
        )
        return java.lang.Double.longBitsToDouble(bits)
    }

    fun getUseGpsNoiseFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.USE_GPS_NOISE, DEFAULT_USE_GPS_NOISE)
    }

    suspend fun saveUseGpsNoise(useGpsNoise: Boolean) {
        savePreference(PreferenceKeys.USE_GPS_NOISE, useGpsNoise, KEY_USE_GPS_NOISE, useGpsNoise)
    }

    fun getUseGpsNoise(): Boolean {
        return sharedPrefs.getBoolean(KEY_USE_GPS_NOISE, DEFAULT_USE_GPS_NOISE)
    }

    fun getGpsNoiseLevelFlow(): Flow<String> {
        return getPreferenceFlow(PreferenceKeys.GPS_NOISE_LEVEL, DEFAULT_GPS_NOISE_LEVEL)
    }

    suspend fun saveGpsNoiseLevel(level: String) {
        savePreference(PreferenceKeys.GPS_NOISE_LEVEL, level, KEY_GPS_NOISE_LEVEL, level)
    }

    fun getGpsNoiseLevel(): String {
        return sharedPrefs.getString(KEY_GPS_NOISE_LEVEL, DEFAULT_GPS_NOISE_LEVEL) ?: DEFAULT_GPS_NOISE_LEVEL
    }

    // Favorites
    fun getFavoritesFlow(): Flow<List<FavoriteLocation>> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(tag, "Error reading preferences: ${exception.message}")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val json = preferences[PreferenceKeys.FAVORITES]
                if (json != null) {
                    try {
                        val type = object : TypeToken<List<FavoriteLocation>>() {}.type
                        gson.fromJson(json, type)
                    } catch (e: JsonSyntaxException) {
                        Log.e(tag, "Error parsing Favorites: ${e.message}")
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }
    }
    
    suspend fun addFavorite(favorite: FavoriteLocation) {
        try {
            val favorites = getFavoritesFlow().firstOrNull() ?: emptyList()
            val updatedFavorites = favorites.toMutableList().apply { add(favorite) }
            saveFavorites(updatedFavorites)
            Log.d(tag, "Added Favorite: $favorite")
        } catch (e: Exception) {
            Log.e(tag, "Error adding favorite: ${e.message}")
        }
    }
    
    private suspend fun saveFavorites(favorites: List<FavoriteLocation>) {
        try {
            val json = gson.toJson(favorites)
            savePreference(PreferenceKeys.FAVORITES, json, KEY_FAVORITES, json)
        } catch (e: Exception) {
            Log.e(tag, "Error saving favorites: ${e.message}")
        }
    }
    
    suspend fun removeFavorite(favorite: FavoriteLocation) {
        try {
            val favorites = getFavoritesFlow().firstOrNull() ?: emptyList()
            val updatedFavorites = favorites.toMutableList().apply { remove(favorite) }
            saveFavorites(updatedFavorites)
            Log.d(tag, "Removed Favorite: $favorite from preferences")
        } catch (e: Exception) {
            Log.e(tag, "Error removing favorite: ${e.message}")
        }
    }
    
    // For backward compatibility
    fun getFavorites(): List<FavoriteLocation> {
        val json = sharedPrefs.getString(KEY_FAVORITES, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<FavoriteLocation>>() {}.type
                gson.fromJson(json, type)
            } catch (e: JsonSyntaxException) {
                Log.e(tag, "Error parsing Favorites: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun getTargetAppsFlow(): Flow<Set<String>> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(tag, "Error reading target apps: ${exception.message}")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                parseTargetApps(preferences[PreferenceKeys.TARGET_APPS])
            }
    }

    suspend fun saveTargetApps(packageNames: Set<String>) {
        val normalized = packageNames
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val json = gson.toJson(normalized)
        savePreference(PreferenceKeys.TARGET_APPS, json, KEY_TARGET_APPS, json)
    }

    fun getAppLocationProfilesFlow(): Flow<Map<String, AppLocationProfile>> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(tag, "Error reading app location profiles: ${exception.message}")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                parseAppLocationProfiles(preferences[PreferenceKeys.APP_LOCATION_PROFILES])
            }
    }

    fun getLocationTemplatesFlow(): Flow<List<LocationTemplate>> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(tag, "Error reading location templates: ${exception.message}")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                parseLocationTemplates(preferences[PreferenceKeys.LOCATION_TEMPLATES])
            }
    }

    suspend fun saveLocationTemplate(template: LocationTemplate) {
        val currentTemplates = getLocationTemplatesFlow().firstOrNull().orEmpty().toMutableList()
        val index = currentTemplates.indexOfFirst { it.id == template.id }
        if (index >= 0) {
            currentTemplates[index] = template
        } else {
            currentTemplates.add(template)
        }
        saveLocationTemplates(currentTemplates)
    }

    suspend fun removeLocationTemplate(templateId: String) {
        val currentTemplates = getLocationTemplatesFlow()
            .firstOrNull()
            .orEmpty()
            .filterNot { it.id == templateId }
        saveLocationTemplates(currentTemplates)
    }

    private suspend fun saveLocationTemplates(templates: List<LocationTemplate>) {
        val normalized = templates
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
        val json = gson.toJson(normalized)
        savePreference(PreferenceKeys.LOCATION_TEMPLATES, json, KEY_LOCATION_TEMPLATES, json)
    }

    private fun parseLocationTemplates(json: String?): List<LocationTemplate> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<LocationTemplate>>() {}.type
            gson.fromJson<List<LocationTemplate>>(json, type)
                .filter { it.id.isNotBlank() && it.name.isNotBlank() }
                .map(::normalizeLocationTemplate)
                .sortedBy { it.name.lowercase() }
        } catch (e: JsonSyntaxException) {
            Log.e(tag, "Error parsing location templates: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveAppLocationProfile(profile: AppLocationProfile) {
        val currentProfiles = getAppLocationProfilesFlow().firstOrNull().orEmpty().toMutableMap()
        currentProfiles[profile.packageName] = profile
        saveAppLocationProfiles(currentProfiles)
    }

    suspend fun removeAppLocationProfile(packageName: String) {
        val currentProfiles = getAppLocationProfilesFlow().firstOrNull().orEmpty().toMutableMap()
        currentProfiles.remove(packageName)
        saveAppLocationProfiles(currentProfiles)
    }

    private suspend fun saveAppLocationProfiles(profiles: Map<String, AppLocationProfile>) {
        val normalized = profiles.values
            .filter { it.packageName.isNotBlank() }
            .distinctBy { it.packageName }
            .sortedBy { it.packageName }
        val json = gson.toJson(normalized)
        savePreference(PreferenceKeys.APP_LOCATION_PROFILES, json, KEY_APP_LOCATION_PROFILES, json)
    }

    private fun parseAppLocationProfiles(json: String?): Map<String, AppLocationProfile> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val type = object : TypeToken<List<AppLocationProfile>>() {}.type
            gson.fromJson<List<AppLocationProfile>>(json, type)
                .filter { it.packageName.isNotBlank() }
                .map(::normalizeAppLocationProfile)
                .associateBy { it.packageName }
        } catch (e: JsonSyntaxException) {
            Log.e(tag, "Error parsing app location profiles: ${e.message}")
            emptyMap()
        }
    }

    private fun normalizeAppLocationProfile(profile: AppLocationProfile): AppLocationProfile {
        return profile.copy(
            randomizeOverride = profile.randomizeOverride ?: OverrideState.INHERIT,
            accuracyOverride = profile.accuracyOverride ?: OverrideState.INHERIT,
            altitudeOverride = profile.altitudeOverride ?: OverrideState.INHERIT,
            verticalAccuracyOverride = profile.verticalAccuracyOverride ?: OverrideState.INHERIT,
            meanSeaLevelOverride = profile.meanSeaLevelOverride ?: OverrideState.INHERIT,
            meanSeaLevelAccuracyOverride = profile.meanSeaLevelAccuracyOverride ?: OverrideState.INHERIT,
            speedOverride = profile.speedOverride ?: OverrideState.INHERIT,
            speedAccuracyOverride = profile.speedAccuracyOverride ?: OverrideState.INHERIT,
            gpsNoiseOverride = profile.gpsNoiseOverride ?: OverrideState.INHERIT,
            gpsNoiseLevel = profile.gpsNoiseLevel ?: GpsNoiseLevel.NORMAL
        )
    }

    private fun normalizeLocationTemplate(template: LocationTemplate): LocationTemplate {
        return template.copy(
            randomizeOverride = template.randomizeOverride ?: OverrideState.INHERIT,
            accuracyOverride = template.accuracyOverride ?: OverrideState.INHERIT,
            altitudeOverride = template.altitudeOverride ?: OverrideState.INHERIT,
            verticalAccuracyOverride = template.verticalAccuracyOverride ?: OverrideState.INHERIT,
            meanSeaLevelOverride = template.meanSeaLevelOverride ?: OverrideState.INHERIT,
            meanSeaLevelAccuracyOverride = template.meanSeaLevelAccuracyOverride ?: OverrideState.INHERIT,
            speedOverride = template.speedOverride ?: OverrideState.INHERIT,
            speedAccuracyOverride = template.speedAccuracyOverride ?: OverrideState.INHERIT,
            gpsNoiseOverride = template.gpsNoiseOverride ?: OverrideState.INHERIT,
            gpsNoiseLevel = template.gpsNoiseLevel ?: GpsNoiseLevel.NORMAL
        )
    }

    private fun parseTargetApps(json: String?): Set<String> {
        if (json.isNullOrBlank()) return emptySet()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type).toSet()
        } catch (e: JsonSyntaxException) {
            Log.e(tag, "Error parsing target apps: ${e.message}")
            emptySet()
        }
    }

    // Vertical Accuracy
    fun getUseVerticalAccuracyFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.USE_VERTICAL_ACCURACY, DEFAULT_USE_VERTICAL_ACCURACY)
    }
    
    suspend fun saveUseVerticalAccuracy(useVerticalAccuracy: Boolean) {
        savePreference(PreferenceKeys.USE_VERTICAL_ACCURACY, useVerticalAccuracy, KEY_USE_VERTICAL_ACCURACY, useVerticalAccuracy)
    }
    
    // For backward compatibility
    fun getUseVerticalAccuracy(): Boolean {
        return sharedPrefs.getBoolean(KEY_USE_VERTICAL_ACCURACY, DEFAULT_USE_VERTICAL_ACCURACY)
    }

    // Vertical Accuracy Value
    fun getVerticalAccuracyFlow(): Flow<Float> {
        return getPreferenceFlow(PreferenceKeys.VERTICAL_ACCURACY, DEFAULT_VERTICAL_ACCURACY)
    }
    
    suspend fun saveVerticalAccuracy(verticalAccuracy: Float) {
        savePreference(PreferenceKeys.VERTICAL_ACCURACY, verticalAccuracy, KEY_VERTICAL_ACCURACY, verticalAccuracy)
    }
    
    // For backward compatibility
    fun getVerticalAccuracy(): Float {
        return sharedPrefs.getFloat(KEY_VERTICAL_ACCURACY, DEFAULT_VERTICAL_ACCURACY)
    }

    // Use Mean Sea Level
    fun getUseMeanSeaLevelFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.USE_MEAN_SEA_LEVEL, DEFAULT_USE_MEAN_SEA_LEVEL)
    }
    
    suspend fun saveUseMeanSeaLevel(useMeanSeaLevel: Boolean) {
        savePreference(PreferenceKeys.USE_MEAN_SEA_LEVEL, useMeanSeaLevel, KEY_USE_MEAN_SEA_LEVEL, useMeanSeaLevel)
    }
    
    // For backward compatibility
    fun getUseMeanSeaLevel(): Boolean {
        return sharedPrefs.getBoolean(KEY_USE_MEAN_SEA_LEVEL, DEFAULT_USE_MEAN_SEA_LEVEL)
    }

    // Mean Sea Level
    fun getMeanSeaLevelFlow(): Flow<Double> {
        return getPreferenceFlow(PreferenceKeys.MEAN_SEA_LEVEL, DEFAULT_MEAN_SEA_LEVEL)
    }
    
    suspend fun saveMeanSeaLevel(meanSeaLevel: Double) {
        savePreference(PreferenceKeys.MEAN_SEA_LEVEL, meanSeaLevel, KEY_MEAN_SEA_LEVEL, meanSeaLevel)
    }
    
    // For backward compatibility
    fun getMeanSeaLevel(): Double {
        val bits = sharedPrefs.getLong(KEY_MEAN_SEA_LEVEL, java.lang.Double.doubleToRawLongBits(DEFAULT_MEAN_SEA_LEVEL))
        return java.lang.Double.longBitsToDouble(bits)
    }

    // Use Mean Sea Level Accuracy
    fun getUseMeanSeaLevelAccuracyFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.USE_MEAN_SEA_LEVEL_ACCURACY, DEFAULT_USE_MEAN_SEA_LEVEL_ACCURACY)
    }
    
    suspend fun saveUseMeanSeaLevelAccuracy(useMeanSeaLevelAccuracy: Boolean) {
        savePreference(PreferenceKeys.USE_MEAN_SEA_LEVEL_ACCURACY, useMeanSeaLevelAccuracy, KEY_USE_MEAN_SEA_LEVEL_ACCURACY, useMeanSeaLevelAccuracy)
    }
    
    // For backward compatibility
    fun getUseMeanSeaLevelAccuracy(): Boolean {
        return sharedPrefs.getBoolean(KEY_USE_MEAN_SEA_LEVEL_ACCURACY, DEFAULT_USE_MEAN_SEA_LEVEL_ACCURACY)
    }

    // Mean Sea Level Accuracy
    fun getMeanSeaLevelAccuracyFlow(): Flow<Float> {
        return getPreferenceFlow(PreferenceKeys.MEAN_SEA_LEVEL_ACCURACY, DEFAULT_MEAN_SEA_LEVEL_ACCURACY)
    }
    
    suspend fun saveMeanSeaLevelAccuracy(meanSeaLevelAccuracy: Float) {
        savePreference(PreferenceKeys.MEAN_SEA_LEVEL_ACCURACY, meanSeaLevelAccuracy, KEY_MEAN_SEA_LEVEL_ACCURACY, meanSeaLevelAccuracy)
    }
    
    // For backward compatibility
    fun getMeanSeaLevelAccuracy(): Float {
        return sharedPrefs.getFloat(KEY_MEAN_SEA_LEVEL_ACCURACY, DEFAULT_MEAN_SEA_LEVEL_ACCURACY)
    }

    // Use Speed
    fun getUseSpeedFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.USE_SPEED, DEFAULT_USE_SPEED)
    }
    
    suspend fun saveUseSpeed(useSpeed: Boolean) {
        savePreference(PreferenceKeys.USE_SPEED, useSpeed, KEY_USE_SPEED, useSpeed)
    }
    
    // For backward compatibility
    fun getUseSpeed(): Boolean {
        return sharedPrefs.getBoolean(KEY_USE_SPEED, DEFAULT_USE_SPEED)
    }

    // Speed
    fun getSpeedFlow(): Flow<Float> {
        return getPreferenceFlow(PreferenceKeys.SPEED, DEFAULT_SPEED)
    }
    
    suspend fun saveSpeed(speed: Float) {
        savePreference(PreferenceKeys.SPEED, speed, KEY_SPEED, speed)
    }
    
    // For backward compatibility
    fun getSpeed(): Float {
        return sharedPrefs.getFloat(KEY_SPEED, DEFAULT_SPEED)
    }

    // Use Speed Accuracy
    fun getUseSpeedAccuracyFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.USE_SPEED_ACCURACY, DEFAULT_USE_SPEED_ACCURACY)
    }
    
    suspend fun saveUseSpeedAccuracy(useSpeedAccuracy: Boolean) {
        savePreference(PreferenceKeys.USE_SPEED_ACCURACY, useSpeedAccuracy, KEY_USE_SPEED_ACCURACY, useSpeedAccuracy)
    }
    
    // For backward compatibility
    fun getUseSpeedAccuracy(): Boolean {
        return sharedPrefs.getBoolean(KEY_USE_SPEED_ACCURACY, DEFAULT_USE_SPEED_ACCURACY)
    }

    // Speed Accuracy
    fun getSpeedAccuracyFlow(): Flow<Float> {
        return getPreferenceFlow(PreferenceKeys.SPEED_ACCURACY, DEFAULT_SPEED_ACCURACY)
    }
    
    suspend fun saveSpeedAccuracy(speedAccuracy: Float) {
        savePreference(PreferenceKeys.SPEED_ACCURACY, speedAccuracy, KEY_SPEED_ACCURACY, speedAccuracy)
    }
    
    // For backward compatibility
    fun getSpeedAccuracy(): Float {
        return sharedPrefs.getFloat(KEY_SPEED_ACCURACY, DEFAULT_SPEED_ACCURACY)
    }

    // Hide Fake Location Toast
    fun getHideFakeLocationToastFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.HIDE_FAKE_LOCATION_TOAST, DEFAULT_HIDE_FAKE_LOCATION_TOAST)
    }

    suspend fun saveHideFakeLocationToast(hideFakeLocationToast: Boolean) {
        savePreference(PreferenceKeys.HIDE_FAKE_LOCATION_TOAST, hideFakeLocationToast, KEY_HIDE_FAKE_LOCATION_TOAST, hideFakeLocationToast)
    }

    // For backward compatibility
    fun getHideFakeLocationToast(): Boolean {
        return sharedPrefs.getBoolean(KEY_HIDE_FAKE_LOCATION_TOAST, DEFAULT_HIDE_FAKE_LOCATION_TOAST)
    }

    // Use In-App Target Apps Selection
    fun getUseInAppTargetAppsFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.USE_INAPP_TARGET_APPS, DEFAULT_USE_INAPP_TARGET_APPS)
    }

    suspend fun saveUseInAppTargetApps(useInAppTargetApps: Boolean) {
        savePreference(PreferenceKeys.USE_INAPP_TARGET_APPS, useInAppTargetApps, KEY_USE_INAPP_TARGET_APPS, useInAppTargetApps)
    }

    // Enable Broadcast Control Receiver
    fun getEnableBroadcastControlFlow(): Flow<Boolean> {
        return getPreferenceFlow(PreferenceKeys.ENABLE_BROADCAST_CONTROL, DEFAULT_ENABLE_BROADCAST_CONTROL)
    }

    suspend fun saveEnableBroadcastControl(enable: Boolean) {
        savePreference(PreferenceKeys.ENABLE_BROADCAST_CONTROL, enable, KEY_ENABLE_BROADCAST_CONTROL, enable)
    }
}
