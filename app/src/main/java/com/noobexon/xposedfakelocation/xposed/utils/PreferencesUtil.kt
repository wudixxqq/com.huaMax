// PreferencesUtil.kt
package com.noobexon.xposedfakelocation.xposed.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.noobexon.xposedfakelocation.data.*
import com.noobexon.xposedfakelocation.data.model.AppLocationProfile
import com.noobexon.xposedfakelocation.data.model.GpsNoiseLevel
import com.noobexon.xposedfakelocation.data.model.LastClickedLocation
import com.noobexon.xposedfakelocation.data.model.LocationTemplate
import com.noobexon.xposedfakelocation.data.model.OverrideState
import com.noobexon.xposedfakelocation.data.model.toAppLocationProfile
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

object PreferencesUtil {
    private const val TAG = "[PreferencesUtil]"

    private val locationProxyPackages = setOf(
        "com.android.location.fused",
        "com.google.android.gms"
    )

    private val preferences: XSharedPreferences = XSharedPreferences(MANAGER_APP_PACKAGE_NAME, SHARED_PREFS_FILE).apply {
        makeWorldReadable()
        reload()
    }

    fun getIsPlaying(): Boolean? {
        return getPreference<Boolean>(KEY_IS_PLAYING)
    }

    fun getLastClickedLocation(): LastClickedLocation? {
        return getPreference<LastClickedLocation>(KEY_LAST_CLICKED_LOCATION)
    }

    fun getUseAccuracy(): Boolean? {
        return getPreference<Boolean>(KEY_USE_ACCURACY)
    }

    fun getAccuracy(): Double? {
        return getPreference<Double>(KEY_ACCURACY)
    }

    fun getUseAltitude(): Boolean? {
        return getPreference<Boolean>(KEY_USE_ALTITUDE)
    }

    fun getAltitude(): Double? {
        return getPreference<Double>(KEY_ALTITUDE)
    }

    fun getUseRandomize(): Boolean? {
        return getPreference<Boolean>(KEY_USE_RANDOMIZE)
    }

    fun getRandomizeRadius(): Double? {
        return getPreference<Double>(KEY_RANDOMIZE_RADIUS)
    }

    fun getUseGpsNoise(): Boolean? {
        return getPreference<Boolean>(KEY_USE_GPS_NOISE)
    }

    fun getGpsNoiseLevel(): GpsNoiseLevel {
        preferences.reload()
        val raw = preferences.getString(KEY_GPS_NOISE_LEVEL, DEFAULT_GPS_NOISE_LEVEL) ?: DEFAULT_GPS_NOISE_LEVEL
        return runCatching { GpsNoiseLevel.valueOf(raw) }.getOrDefault(GpsNoiseLevel.NORMAL)
    }

    fun getUseVerticalAccuracy(): Boolean? {
        return getPreference<Boolean>(KEY_USE_VERTICAL_ACCURACY)
    }

    fun getVerticalAccuracy(): Float? {
        return getPreference<Float>(KEY_VERTICAL_ACCURACY)
    }

    fun getUseMeanSeaLevel(): Boolean? {
        return getPreference<Boolean>(KEY_USE_MEAN_SEA_LEVEL)
    }

    fun getMeanSeaLevel(): Double? {
        return getPreference<Double>(KEY_MEAN_SEA_LEVEL)
    }

    fun getUseMeanSeaLevelAccuracy(): Boolean? {
        return getPreference<Boolean>(KEY_USE_MEAN_SEA_LEVEL_ACCURACY)
    }

    fun getMeanSeaLevelAccuracy(): Float? {
        return getPreference<Float>(KEY_MEAN_SEA_LEVEL_ACCURACY)
    }

    fun getUseSpeed(): Boolean? {
        return getPreference<Boolean>(KEY_USE_SPEED)
    }

    fun getSpeed(): Float? {
        return getPreference<Float>(KEY_SPEED)
    }

    fun getUseSpeedAccuracy(): Boolean? {
        return getPreference<Boolean>(KEY_USE_SPEED_ACCURACY)
    }

    fun getSpeedAccuracy(): Float? {
        return getPreference<Float>(KEY_SPEED_ACCURACY)
    }

    fun getHideFakeLocationToast(): Boolean {
        return getPreference<Boolean>(KEY_HIDE_FAKE_LOCATION_TOAST) ?: DEFAULT_HIDE_FAKE_LOCATION_TOAST
    }

    fun getUseInAppTargetApps(): Boolean {
        preferences.reload()
        return if (preferences.contains(KEY_USE_INAPP_TARGET_APPS)) {
            preferences.getBoolean(KEY_USE_INAPP_TARGET_APPS, DEFAULT_USE_INAPP_TARGET_APPS)
        } else {
            DEFAULT_USE_INAPP_TARGET_APPS
        }
    }

    fun getTargetApps(): Set<String> {
        preferences.reload()
        val json = preferences.getString(KEY_TARGET_APPS, null) ?: return emptySet()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(json, type).toSet()
        } catch (e: Exception) {
            XposedBridge.log("$TAG Error parsing target apps JSON: ${e.message}")
            emptySet()
        }
    }

    fun getAppLocationProfiles(): Map<String, AppLocationProfile> {
        preferences.reload()
        val json = preferences.getString(KEY_APP_LOCATION_PROFILES, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<List<AppLocationProfile>>() {}.type
            Gson().fromJson<List<AppLocationProfile>>(json, type)
                .filter { it.packageName.isNotBlank() }
                .map(::normalizeAppLocationProfile)
                .associateBy { it.packageName }
        } catch (e: Exception) {
            XposedBridge.log("$TAG Error parsing app location profiles JSON: ${e.message}")
            emptyMap()
        }
    }

    fun getAppLocationProfile(packageName: String?): AppLocationProfile? {
        if (packageName.isNullOrBlank() || packageName == MANAGER_APP_PACKAGE_NAME) return null
        val profile = getAppLocationProfiles()[packageName]?.takeIf { it.enabled } ?: return null

        val globalProfile = createGlobalProfile(packageName) ?: return null
        val sourceProfile = when {
            profile.useCustomLocation -> globalProfile.copy(
                useCustomLocation = true,
                latitude = profile.latitude,
                longitude = profile.longitude
            )
            profile.templateId.isNullOrBlank() -> globalProfile
            else -> {
                val template = getLocationTemplates().firstOrNull { it.id == profile.templateId }
                if (template == null) {
                    globalProfile
                } else {
                    globalProfile.copy(
                        templateId = template.id,
                        latitude = template.latitude,
                        longitude = template.longitude
                    ).withAdvancedSettingsFrom(template.toAppLocationProfile(packageName = packageName, enabled = true))
                }
            }
        }

        return if (profile.useCustomAdvancedSettings) {
            sourceProfile.withAdvancedSettingsFrom(profile)
        } else {
            sourceProfile
        }
    }

    private fun AppLocationProfile.withAdvancedSettingsFrom(override: AppLocationProfile): AppLocationProfile {
        val randomizeOverride = override.randomizeOverride ?: OverrideState.INHERIT
        val accuracyOverride = override.accuracyOverride ?: OverrideState.INHERIT
        val altitudeOverride = override.altitudeOverride ?: OverrideState.INHERIT
        val verticalAccuracyOverride = override.verticalAccuracyOverride ?: OverrideState.INHERIT
        val meanSeaLevelOverride = override.meanSeaLevelOverride ?: OverrideState.INHERIT
        val meanSeaLevelAccuracyOverride = override.meanSeaLevelAccuracyOverride ?: OverrideState.INHERIT
        val speedOverride = override.speedOverride ?: OverrideState.INHERIT
        val speedAccuracyOverride = override.speedAccuracyOverride ?: OverrideState.INHERIT
        val gpsNoiseOverride = override.gpsNoiseOverride ?: OverrideState.INHERIT

        return copy(
            useCustomAdvancedSettings = true,
            randomizeOverride = randomizeOverride,
            useRandomize = resolveEnabled(useRandomize, randomizeOverride, override.useRandomize),
            randomizeRadius = if (randomizeOverride == OverrideState.ENABLED) override.randomizeRadius else randomizeRadius,
            accuracyOverride = accuracyOverride,
            useAccuracy = resolveEnabled(useAccuracy, accuracyOverride, override.useAccuracy),
            accuracy = if (accuracyOverride == OverrideState.ENABLED) override.accuracy else accuracy,
            altitudeOverride = altitudeOverride,
            useAltitude = resolveEnabled(useAltitude, altitudeOverride, override.useAltitude),
            altitude = if (altitudeOverride == OverrideState.ENABLED) override.altitude else altitude,
            verticalAccuracyOverride = verticalAccuracyOverride,
            useVerticalAccuracy = resolveEnabled(useVerticalAccuracy, verticalAccuracyOverride, override.useVerticalAccuracy),
            verticalAccuracy = if (verticalAccuracyOverride == OverrideState.ENABLED) override.verticalAccuracy else verticalAccuracy,
            meanSeaLevelOverride = meanSeaLevelOverride,
            useMeanSeaLevel = resolveEnabled(useMeanSeaLevel, meanSeaLevelOverride, override.useMeanSeaLevel),
            meanSeaLevel = if (meanSeaLevelOverride == OverrideState.ENABLED) override.meanSeaLevel else meanSeaLevel,
            meanSeaLevelAccuracyOverride = meanSeaLevelAccuracyOverride,
            useMeanSeaLevelAccuracy = resolveEnabled(useMeanSeaLevelAccuracy, meanSeaLevelAccuracyOverride, override.useMeanSeaLevelAccuracy),
            meanSeaLevelAccuracy = if (meanSeaLevelAccuracyOverride == OverrideState.ENABLED) override.meanSeaLevelAccuracy else meanSeaLevelAccuracy,
            speedOverride = speedOverride,
            useSpeed = resolveEnabled(useSpeed, speedOverride, override.useSpeed),
            speed = if (speedOverride == OverrideState.ENABLED) override.speed else speed,
            speedAccuracyOverride = speedAccuracyOverride,
            useSpeedAccuracy = resolveEnabled(useSpeedAccuracy, speedAccuracyOverride, override.useSpeedAccuracy),
            speedAccuracy = if (speedAccuracyOverride == OverrideState.ENABLED) override.speedAccuracy else speedAccuracy,
            gpsNoiseOverride = gpsNoiseOverride,
            useGpsNoise = resolveEnabled(useGpsNoise, gpsNoiseOverride, override.useGpsNoise),
            gpsNoiseLevel = if (gpsNoiseOverride == OverrideState.ENABLED) {
                override.gpsNoiseLevel ?: GpsNoiseLevel.NORMAL
            } else {
                gpsNoiseLevel
            }
        )
    }

    private fun resolveEnabled(base: Boolean, overrideState: OverrideState?, overrideValue: Boolean): Boolean {
        return when (overrideState ?: OverrideState.INHERIT) {
            OverrideState.INHERIT -> base
            OverrideState.ENABLED -> overrideValue
            OverrideState.DISABLED -> false
        }
    }

    private fun createGlobalProfile(
        packageName: String,
        latitudeOverride: Double? = null,
        longitudeOverride: Double? = null
    ): AppLocationProfile? {
        val location = getLastClickedLocation() ?: return null
        return AppLocationProfile(
            packageName = packageName,
            templateId = null,
            useCustomLocation = latitudeOverride != null && longitudeOverride != null,
            useCustomAdvancedSettings = false,
            latitude = latitudeOverride ?: location.latitude,
            longitude = longitudeOverride ?: location.longitude,
            enabled = true,
            useRandomize = getUseRandomize() == true,
            randomizeRadius = getRandomizeRadius() ?: DEFAULT_RANDOMIZE_RADIUS,
            useAccuracy = getUseAccuracy() == true,
            accuracy = getAccuracy() ?: DEFAULT_ACCURACY,
            useAltitude = getUseAltitude() == true,
            altitude = getAltitude() ?: DEFAULT_ALTITUDE,
            useVerticalAccuracy = getUseVerticalAccuracy() == true,
            verticalAccuracy = getVerticalAccuracy() ?: DEFAULT_VERTICAL_ACCURACY,
            useMeanSeaLevel = getUseMeanSeaLevel() == true,
            meanSeaLevel = getMeanSeaLevel() ?: DEFAULT_MEAN_SEA_LEVEL,
            useMeanSeaLevelAccuracy = getUseMeanSeaLevelAccuracy() == true,
            meanSeaLevelAccuracy = getMeanSeaLevelAccuracy() ?: DEFAULT_MEAN_SEA_LEVEL_ACCURACY,
            useSpeed = getUseSpeed() == true,
            speed = getSpeed() ?: DEFAULT_SPEED,
            useSpeedAccuracy = getUseSpeedAccuracy() == true,
            speedAccuracy = getSpeedAccuracy() ?: DEFAULT_SPEED_ACCURACY,
            useGpsNoise = getUseGpsNoise() == true,
            gpsNoiseLevel = getGpsNoiseLevel()
        )
    }

    fun getLocationTemplates(): List<LocationTemplate> {
        preferences.reload()
        val json = preferences.getString(KEY_LOCATION_TEMPLATES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LocationTemplate>>() {}.type
            Gson().fromJson<List<LocationTemplate>>(json, type)
                .filter { it.id.isNotBlank() && it.name.isNotBlank() }
                .map(::normalizeLocationTemplate)
        } catch (e: Exception) {
            XposedBridge.log("$TAG Error parsing location templates JSON: ${e.message}")
            emptyList()
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

    fun shouldSpoofPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank() || packageName == MANAGER_APP_PACKAGE_NAME) return false
        if (getIsPlaying() != true) return false

        return getAppLocationProfile(packageName) != null
    }

    private inline fun <reified T> getPreference(key: String): T? {
        preferences.reload()
        return when (T::class) {
            Double::class -> {
                val defaultValue = when (key) {
                    KEY_ACCURACY -> java.lang.Double.doubleToRawLongBits(DEFAULT_ACCURACY)
                    KEY_ALTITUDE -> java.lang.Double.doubleToRawLongBits(DEFAULT_ALTITUDE)
                    KEY_RANDOMIZE_RADIUS -> java.lang.Double.doubleToRawLongBits(DEFAULT_RANDOMIZE_RADIUS)
                    KEY_MEAN_SEA_LEVEL -> java.lang.Double.doubleToRawLongBits(DEFAULT_MEAN_SEA_LEVEL)
                    else -> -1L
                }
                val bits = preferences.getLong(key, defaultValue)
                java.lang.Double.longBitsToDouble(bits) as? T
            }
            Float::class -> {
                val defaultValue = when (key) {
                    KEY_VERTICAL_ACCURACY -> DEFAULT_VERTICAL_ACCURACY
                    KEY_MEAN_SEA_LEVEL_ACCURACY -> DEFAULT_MEAN_SEA_LEVEL_ACCURACY
                    KEY_SPEED -> DEFAULT_SPEED
                    KEY_SPEED_ACCURACY -> DEFAULT_SPEED_ACCURACY
                    else -> -1f
                }
                preferences.getFloat(key, defaultValue) as? T
            }
            Boolean::class -> preferences.getBoolean(key, false) as? T
            else -> {
                val json = preferences.getString(key, null)
                if (json != null) {
                    try {
                        Gson().fromJson(json, T::class.java).also {
                            XposedBridge.log("$TAG Retrieved $key: $it")
                        }
                    } catch (e: Exception) {
                        XposedBridge.log("$TAG Error parsing $key JSON: ${e.message}")
                        null
                    }
                } else {
                    XposedBridge.log("$TAG $key not found in preferences.")
                    null
                }
            }
        }
    }
}
