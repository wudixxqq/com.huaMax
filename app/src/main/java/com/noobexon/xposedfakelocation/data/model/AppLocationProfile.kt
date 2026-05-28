package com.noobexon.xposedfakelocation.data.model

enum class GpsNoiseLevel {
    LOW,
    NORMAL,
    HIGH;

    companion object {
        fun fromPreferenceValue(value: String?): GpsNoiseLevel {
            return runCatching { valueOf(value ?: NORMAL.name) }.getOrDefault(NORMAL)
        }
    }
}

enum class OverrideState {
    INHERIT,
    ENABLED,
    DISABLED
}

data class AppLocationProfile(
    val packageName: String,
    val templateId: String? = null,
    val useCustomLocation: Boolean = false,
    val useCustomAdvancedSettings: Boolean = false,
    val latitude: Double,
    val longitude: Double,
    val enabled: Boolean = true,
    val randomizeOverride: OverrideState = OverrideState.INHERIT,
    val useRandomize: Boolean = false,
    val randomizeRadius: Double = 0.0,
    val accuracyOverride: OverrideState = OverrideState.INHERIT,
    val useAccuracy: Boolean = false,
    val accuracy: Double = 0.0,
    val altitudeOverride: OverrideState = OverrideState.INHERIT,
    val useAltitude: Boolean = false,
    val altitude: Double = 0.0,
    val verticalAccuracyOverride: OverrideState = OverrideState.INHERIT,
    val useVerticalAccuracy: Boolean = false,
    val verticalAccuracy: Float = 0.0f,
    val meanSeaLevelOverride: OverrideState = OverrideState.INHERIT,
    val useMeanSeaLevel: Boolean = false,
    val meanSeaLevel: Double = 0.0,
    val meanSeaLevelAccuracyOverride: OverrideState = OverrideState.INHERIT,
    val useMeanSeaLevelAccuracy: Boolean = false,
    val meanSeaLevelAccuracy: Float = 0.0f,
    val speedOverride: OverrideState = OverrideState.INHERIT,
    val useSpeed: Boolean = false,
    val speed: Float = 0.0f,
    val speedAccuracyOverride: OverrideState = OverrideState.INHERIT,
    val useSpeedAccuracy: Boolean = false,
    val speedAccuracy: Float = 0.0f,
    val gpsNoiseOverride: OverrideState = OverrideState.INHERIT,
    val useGpsNoise: Boolean = false,
    val gpsNoiseLevel: GpsNoiseLevel = GpsNoiseLevel.NORMAL
)
