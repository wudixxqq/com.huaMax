// LocationUtil.kt
package com.noobexon.xposedfakelocation.xposed.utils

import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import com.noobexon.xposedfakelocation.data.RADIUS_EARTH
import com.noobexon.xposedfakelocation.data.model.AppLocationProfile
import com.noobexon.xposedfakelocation.data.model.GpsNoiseLevel
import de.robv.android.xposed.XposedBridge
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.Random
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtil {
    private const val TAG = "[LocationUtil]"

    private const val DEBUG: Boolean = true

    private val random: Random = Random()
    private val driftStates = mutableMapOf<String, DriftState>()

    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var accuracy: Float = 0F
    var altitude: Double = 0.0
    var verticalAccuracy: Float = 0F
    var meanSeaLevel: Double = 0.0
    var meanSeaLevelAccuracy: Float = 0F
    var speed: Float = 0F
    var speedAccuracy: Float = 0F
    var useAccuracy: Boolean = false
    var useAltitude: Boolean = false
    var useVerticalAccuracy: Boolean = false
    var useMeanSeaLevel: Boolean = false
    var useMeanSeaLevelAccuracy: Boolean = false
    var useSpeed: Boolean = false
    var useSpeedAccuracy: Boolean = false

    fun isSpoofingEnabled(): Boolean {
        return PreferencesUtil.getIsPlaying() == true &&
            PreferencesUtil.getAppLocationProfiles().values.any { it.enabled }
    }

    fun shouldSpoofPackage(packageName: String?): Boolean {
        return PreferencesUtil.shouldSpoofPackage(packageName)
    }

    @Synchronized
    fun createFakeLocation(
        originalLocation: Location? = null,
        provider: String = LocationManager.GPS_PROVIDER,
        packageName: String? = null
    ): Location {
        updateLocation(packageName)

        val fakeLocation = if (originalLocation == null) {
            Location(provider).apply {
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            }
        } else {
            Location(originalLocation.provider).apply {
                time = System.currentTimeMillis()
                accuracy = originalLocation.accuracy
                bearing = originalLocation.bearing
                bearingAccuracyDegrees = originalLocation.bearingAccuracyDegrees
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                verticalAccuracyMeters = originalLocation.verticalAccuracyMeters
            }
        }

        fakeLocation.latitude = latitude
        fakeLocation.longitude = longitude

        if (useAccuracy) {
            fakeLocation.accuracy = accuracy
        }

        if (useAltitude) {
            fakeLocation.altitude = altitude
        }

        if (useVerticalAccuracy) {
            fakeLocation.verticalAccuracyMeters = verticalAccuracy
        }

        if (useSpeed) {
            fakeLocation.speed = speed
        }

        if (useSpeedAccuracy) {
            fakeLocation.speedAccuracyMetersPerSecond = speedAccuracy
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (useMeanSeaLevel) {
                fakeLocation.mslAltitudeMeters = meanSeaLevel
            }

            if (useMeanSeaLevelAccuracy) {
                fakeLocation.mslAltitudeAccuracyMeters = meanSeaLevelAccuracy
            }
        }

        attemptHideMockProvider(fakeLocation)

        return fakeLocation
    }

    private fun attemptHideMockProvider(fakeLocation: Location) {
        try {
            HiddenApiBypass.invoke(fakeLocation.javaClass, fakeLocation, "setIsFromMockProvider", false)
            XposedBridge.log("$TAG invoked hidden API - setIsFromMockProvider: false)")
        } catch (e: Exception) {
            XposedBridge.log("$TAG Not possible to mock - ${e.message}")
        }
    }

    @Synchronized
    fun updateLocation(packageName: String? = null) {
        try {
            latitude = 0.0
            longitude = 0.0
            accuracy = 0F
            altitude = 0.0
            verticalAccuracy = 0F
            meanSeaLevel = 0.0
            meanSeaLevelAccuracy = 0F
            speed = 0F
            speedAccuracy = 0F
            useAccuracy = false
            useAltitude = false
            useVerticalAccuracy = false
            useMeanSeaLevel = false
            useMeanSeaLevelAccuracy = false
            useSpeed = false
            useSpeedAccuracy = false

            val profile = resolveProfile(packageName)
            if (profile != null) {
                val coordinates = resolveCoordinates(profile)
                latitude = coordinates.first
                longitude = coordinates.second

                useAccuracy = profile.useAccuracy
                useAltitude = profile.useAltitude
                useVerticalAccuracy = profile.useVerticalAccuracy
                useMeanSeaLevel = profile.useMeanSeaLevel
                useMeanSeaLevelAccuracy = profile.useMeanSeaLevelAccuracy
                useSpeed = profile.useSpeed
                useSpeedAccuracy = profile.useSpeedAccuracy

                if (profile.useAccuracy) {
                    accuracy = profile.accuracy.toFloat()
                }

                if (profile.useAltitude) altitude = profile.altitude
                if (profile.useVerticalAccuracy) verticalAccuracy = profile.verticalAccuracy
                if (profile.useMeanSeaLevel) meanSeaLevel = profile.meanSeaLevel
                if (profile.useMeanSeaLevelAccuracy) meanSeaLevelAccuracy = profile.meanSeaLevelAccuracy
                if (profile.useSpeed) speed = profile.speed
                if (profile.useSpeedAccuracy) speedAccuracy = profile.speedAccuracy

                if (DEBUG) {
                    XposedBridge.log("$TAG Updated fake location values to:")
                    XposedBridge.log("\tCoordinates: (latitude = $latitude, longitude = $longitude)")
                    XposedBridge.log("\tAccuracy: $accuracy")
                    XposedBridge.log("\tAltitude: $altitude")
                    XposedBridge.log("\tVertical Accuracy: $verticalAccuracy")
                    XposedBridge.log("\tMean Sea Level: $meanSeaLevel")
                    XposedBridge.log("\tMean Sea Level Accuracy: $meanSeaLevelAccuracy")
                    XposedBridge.log("\tSpeed: $speed")
                    XposedBridge.log("\tSpeed Accuracy: $speedAccuracy")
                }
            } else {
                XposedBridge.log("$TAG No enabled app location profile for package=$packageName")
            }
        } catch (e: Exception) {
            XposedBridge.log("$TAG Error - ${e.message}")
        }
    }

    private fun resolveProfile(packageName: String?): AppLocationProfile? {
        if (packageName.isNullOrBlank()) return null
        return PreferencesUtil.getAppLocationProfile(packageName)
    }

    private fun resolveCoordinates(profile: AppLocationProfile): Pair<Double, Double> {
        if (profile.useGpsNoise) return applyStationaryNoise(profile)
        if (profile.useRandomize && profile.randomizeRadius > 0.0) {
            return getRandomLocation(profile.latitude, profile.longitude, profile.randomizeRadius)
        }
        return profile.latitude to profile.longitude
    }

    private fun applyStationaryNoise(profile: AppLocationProfile): Pair<Double, Double> {
        if (!profile.useGpsNoise) return profile.latitude to profile.longitude
        val preset = profile.gpsNoiseLevel.preset()
        val state = driftStates.getOrPut(profile.packageName) {
            DriftState(
                latitude = profile.latitude,
                longitude = profile.longitude,
                eastMeters = 0.0,
                northMeters = 0.0,
                lastUpdateMillis = 0L
            )
        }

        if (state.latitude != profile.latitude || state.longitude != profile.longitude) {
            state.latitude = profile.latitude
            state.longitude = profile.longitude
            state.eastMeters = 0.0
            state.northMeters = 0.0
            state.lastUpdateMillis = 0L
        }

        val now = System.currentTimeMillis()
        if (now - state.lastUpdateMillis >= preset.updateIntervalMillis) {
            state.eastMeters += randomSignedDouble(preset.stepMeters)
            state.northMeters += randomSignedDouble(preset.stepMeters)

            val distance = sqrt(state.eastMeters * state.eastMeters + state.northMeters * state.northMeters)
            if (distance > preset.radiusMeters && distance > 0.0) {
                val scale = preset.radiusMeters / distance
                state.eastMeters *= scale
                state.northMeters *= scale
            }

            state.lastUpdateMillis = now
        }

        return offsetCoordinates(profile.latitude, profile.longitude, state.eastMeters, state.northMeters)
    }

    private fun offsetCoordinates(latitude: Double, longitude: Double, eastMeters: Double, northMeters: Double): Pair<Double, Double> {
        val newLatitude = latitude + Math.toDegrees(northMeters / RADIUS_EARTH)
        val latitudeRadians = Math.toRadians(latitude)
        val longitudeScale = cos(latitudeRadians).coerceAtLeast(0.000001)
        val newLongitude = longitude + Math.toDegrees(eastMeters / (RADIUS_EARTH * longitudeScale))
        val normalizedLongitude = ((newLongitude + 180) % 360 + 360) % 360 - 180
        return newLatitude.coerceIn(-90.0, 90.0) to normalizedLongitude
    }

    private fun getRandomLocation(lat: Double, lon: Double, radiusInMeters: Double): Pair<Double, Double> {
        val radiusInRadians = radiusInMeters / RADIUS_EARTH
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val distance = radiusInRadians * sqrt(random.nextDouble())
        val bearing = 2 * Math.PI * random.nextDouble()
        val sinDistance = sin(distance)
        val cosDistance = cos(distance)
        val newLatRad = asin(sinLat * cosDistance + cosLat * sinDistance * cos(bearing))
        val newLonRad = lonRad + atan2(
            sin(bearing) * sinDistance * cosLat,
            cosDistance - sinLat * sin(newLatRad)
        )
        val newLat = Math.toDegrees(newLatRad).coerceIn(-90.0, 90.0)
        val newLon = ((Math.toDegrees(newLonRad) + 180) % 360 + 360) % 360 - 180
        return newLat to newLon
    }

    private fun randomSignedDouble(maxAbs: Double): Double {
        return (random.nextDouble() * 2.0 - 1.0) * maxAbs
    }

    private fun GpsNoiseLevel.preset(): NoisePreset {
        return when (this) {
            GpsNoiseLevel.LOW -> NoisePreset(radiusMeters = 3.0, updateIntervalMillis = 5_000L, stepMeters = 0.8)
            GpsNoiseLevel.NORMAL -> NoisePreset(radiusMeters = 8.0, updateIntervalMillis = 3_000L, stepMeters = 1.5)
            GpsNoiseLevel.HIGH -> NoisePreset(radiusMeters = 20.0, updateIntervalMillis = 2_000L, stepMeters = 3.0)
        }
    }

    private data class NoisePreset(
        val radiusMeters: Double,
        val updateIntervalMillis: Long,
        val stepMeters: Double
    )

    private data class DriftState(
        var latitude: Double,
        var longitude: Double,
        var eastMeters: Double,
        var northMeters: Double,
        var lastUpdateMillis: Long
    )
}
