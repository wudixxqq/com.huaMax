package com.huaMax.xposed.hooks

import android.location.Location
import android.os.Build
import android.util.Log
import com.huaMax.xposed.utils.LocationUtil
import com.huaMax.xposed.utils.PreferencesUtil
import io.github.libxposed.api.XposedInterface

class LocationApiHooks(private val module: XposedInterface, private val classLoader: ClassLoader) {
    private val tag = "[LocationApiHooks]"

    private companion object {
        const val LOG_LOCATION_EVENTS = false
    }

    fun initHooks() {
        hookLocation()
        hookLocationManager()
        module.log(Log.INFO, tag, "Instantiated hooks successfully")
    }

    private inline fun logLocationEvent(message: () -> String) {
        if (LOG_LOCATION_EVENTS) {
            module.log(Log.INFO, tag, message())
        }
    }

    private fun hookLocation() {
        try {
            val locationClass = Class.forName("android.location.Location", false, classLoader)

            module.hook(locationClass.getDeclaredMethod("getLatitude")).intercept { chain ->
                val original = chain.proceed()
                if (PreferencesUtil.getIsPlaying()) {
                    LocationUtil.updateLocation()
                    logLocationEvent { "getLatitude(): $original -> ${LocationUtil.latitude}" }
                    LocationUtil.latitude
                } else {
                    original
                }
            }

            module.hook(locationClass.getDeclaredMethod("getLongitude")).intercept { chain ->
                val original = chain.proceed()
                if (PreferencesUtil.getIsPlaying()) {
                    LocationUtil.updateLocation()
                    logLocationEvent { "getLongitude(): $original -> ${LocationUtil.longitude}" }
                    LocationUtil.longitude
                } else {
                    original
                }
            }

            module.hook(locationClass.getDeclaredMethod("getAccuracy")).intercept { chain ->
                val original = chain.proceed()
                if (PreferencesUtil.getIsPlaying() && PreferencesUtil.getUseAccuracy()) {
                    LocationUtil.updateLocation()
                    logLocationEvent { "getAccuracy(): $original -> ${LocationUtil.accuracy}" }
                    LocationUtil.accuracy
                } else {
                    original
                }
            }

            module.hook(locationClass.getDeclaredMethod("getAltitude")).intercept { chain ->
                val original = chain.proceed()
                if (PreferencesUtil.getIsPlaying() && PreferencesUtil.getUseAltitude()) {
                    LocationUtil.updateLocation()
                    logLocationEvent { "getAltitude(): $original -> ${LocationUtil.altitude}" }
                    LocationUtil.altitude
                } else {
                    original
                }
            }

            module.hook(locationClass.getDeclaredMethod("getVerticalAccuracyMeters")).intercept { chain ->
                val original = chain.proceed()
                if (PreferencesUtil.getIsPlaying() && PreferencesUtil.getUseVerticalAccuracy()) {
                    LocationUtil.updateLocation()
                    logLocationEvent {
                        "getVerticalAccuracyMeters(): $original -> ${LocationUtil.verticalAccuracy}"
                    }
                    LocationUtil.verticalAccuracy
                } else {
                    original
                }
            }

            module.hook(locationClass.getDeclaredMethod("getSpeed")).intercept { chain ->
                val original = chain.proceed()
                if (PreferencesUtil.getIsPlaying() && PreferencesUtil.getUseSpeed()) {
                    LocationUtil.updateLocation()
                    logLocationEvent { "getSpeed(): $original -> ${LocationUtil.speed}" }
                    LocationUtil.speed
                } else {
                    original
                }
            }

            module.hook(locationClass.getDeclaredMethod("getSpeedAccuracyMetersPerSecond")).intercept { chain ->
                val original = chain.proceed()
                if (PreferencesUtil.getIsPlaying() && PreferencesUtil.getUseSpeedAccuracy()) {
                    LocationUtil.updateLocation()
                    logLocationEvent {
                        "getSpeedAccuracyMetersPerSecond(): $original -> ${LocationUtil.speedAccuracy}"
                    }
                    LocationUtil.speedAccuracy
                } else {
                    original
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                module.hook(locationClass.getDeclaredMethod("getMslAltitudeMeters")).intercept { chain ->
                    val original = chain.proceed()
                    if (PreferencesUtil.getIsPlaying() && PreferencesUtil.getUseMeanSeaLevel()) {
                        LocationUtil.updateLocation()
                        logLocationEvent { "getMslAltitudeMeters(): $original -> ${LocationUtil.meanSeaLevel}" }
                        LocationUtil.meanSeaLevel
                    } else {
                        original
                    }
                }

                module.hook(locationClass.getDeclaredMethod("getMslAltitudeAccuracyMeters")).intercept { chain ->
                    val original = chain.proceed()
                    if (PreferencesUtil.getIsPlaying() && PreferencesUtil.getUseMeanSeaLevelAccuracy()) {
                        LocationUtil.updateLocation()
                        logLocationEvent {
                            "getMslAltitudeAccuracyMeters(): $original -> ${LocationUtil.meanSeaLevelAccuracy}"
                        }
                        LocationUtil.meanSeaLevelAccuracy
                    } else {
                        original
                    }
                }
            } else {
                module.log(Log.INFO, tag, "MSL altitude APIs not available on this API level")
            }

            hookMockLocationFlags(locationClass)
        } catch (e: Exception) {
            module.log(Log.ERROR, tag, "Error hooking Location class - ${e.message}")
        }
    }

    private fun hookMockLocationFlags(locationClass: Class<*>) {
        listOf("isFromMockProvider", "isMock").forEach { methodName ->
            runCatching {
                val method = locationClass.getDeclaredMethod(methodName)
                module.hook(method).intercept { chain ->
                    if (PreferencesUtil.getIsPlaying()) {
                        false
                    } else {
                        chain.proceed()
                    }
                }
            }.onFailure {
                logLocationEvent { "$methodName API not available or could not be hooked: ${it.message}" }
            }
        }
    }

    private fun hookLocationManager() {
        try {
            val locationManagerClass = Class.forName("android.location.LocationManager", false, classLoader)
            val method = locationManagerClass.getDeclaredMethod("getLastKnownLocation", String::class.java)

            module.hook(method).intercept { chain ->
                val original = chain.proceed() as? Location
                val provider = chain.getArg(0) as String
                if (PreferencesUtil.getIsPlaying()) {
                    val fakeLocation = LocationUtil.createFakeLocation(provider = provider)
                    logLocationEvent { "getLastKnownLocation($provider): $original -> $fakeLocation" }
                    fakeLocation
                } else {
                    original
                }
            }
        } catch (e: Exception) {
            module.log(Log.ERROR, tag, "Error hooking LocationManager - ${e.message}")
        }
    }
}
