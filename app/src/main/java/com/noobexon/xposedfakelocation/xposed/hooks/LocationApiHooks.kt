// LocationApiHooks.kt
package com.noobexon.xposedfakelocation.xposed.hooks

import android.location.Location
import com.noobexon.xposedfakelocation.xposed.utils.LocationUtil
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class LocationApiHooks(val appLpparam: LoadPackageParam) {
    private val tag = "[LocationApiHooks]"

    fun initHooks() {
        hookLocationAPI()
        XposedBridge.log("$tag Instantiated hooks successfully")
    }

    private fun hookLocationAPI() {
        hookLocation(appLpparam.classLoader)
        hookLocationManager(appLpparam.classLoader)
    }

    private fun hookLocation(classLoader: ClassLoader) {
        try {
            val locationClass = XposedHelpers.findClass("android.location.Location", classLoader)

            XposedHelpers.findAndHookMethod(
                locationClass,
                "getLatitude",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!shouldSpoofCurrentPackage()) return
                        LocationUtil.updateLocation(appLpparam.packageName)
                        XposedBridge.log("$tag Leaving method getLatitude()")
                        XposedBridge.log("\t Original latitude: ${param.result as Double}")
                        param.result = LocationUtil.latitude
                        XposedBridge.log("\t Modified to: ${LocationUtil.latitude}")
                    }
                })

            XposedHelpers.findAndHookMethod(
                locationClass,
                "getLongitude",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!shouldSpoofCurrentPackage()) return
                        LocationUtil.updateLocation(appLpparam.packageName)
                        XposedBridge.log("$tag Leaving method getLongitude()")
                        XposedBridge.log("\t Original longitude: ${param.result as Double}")
                        param.result =  LocationUtil.longitude
                        XposedBridge.log("\t Modified to: ${LocationUtil.longitude}")
                    }
                })

            XposedHelpers.findAndHookMethod(
                locationClass,
                "getAccuracy",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!shouldSpoofCurrentPackage()) return
                        LocationUtil.updateLocation(appLpparam.packageName)
                        XposedBridge.log("$tag Leaving method getAccuracy()")
                        XposedBridge.log("\t Original accuracy: ${param.result as Float}")
                        if (LocationUtil.useAccuracy) {
                            param.result =  LocationUtil.accuracy
                            XposedBridge.log("\t Modified to: ${LocationUtil.accuracy}")
                        }
                    }

                    })

            XposedHelpers.findAndHookMethod(
                locationClass,
                "getAltitude",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!shouldSpoofCurrentPackage()) return
                        LocationUtil.updateLocation(appLpparam.packageName)
                        XposedBridge.log("$tag Leaving method getAltitude()")
                        XposedBridge.log("\t Original altitude: ${param.result as Double}")
                        if (LocationUtil.useAltitude) {
                            param.result =  LocationUtil.altitude
                            XposedBridge.log("\t Modified to: ${LocationUtil.altitude}")
                        }
                    }

                })

            XposedHelpers.findAndHookMethod(
                locationClass,
                "getVerticalAccuracyMeters",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!shouldSpoofCurrentPackage()) return
                        LocationUtil.updateLocation(appLpparam.packageName)
                        XposedBridge.log("$tag Leaving method getVerticalAccuracyMeters()")
                        XposedBridge.log("\tOriginal vertical accuracy: ${param.result as Float}")
                        if (LocationUtil.useVerticalAccuracy) {
                            param.result = LocationUtil.verticalAccuracy
                            XposedBridge.log("\tModified to: ${LocationUtil.verticalAccuracy}")
                        }
                    }
                })

            XposedHelpers.findAndHookMethod(
                locationClass,
                "getSpeed",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!shouldSpoofCurrentPackage()) return
                        LocationUtil.updateLocation(appLpparam.packageName)
                        XposedBridge.log("$tag Leaving method getSpeed()")
                        XposedBridge.log("\tOriginal speed: ${param.result as Float}")
                        if (LocationUtil.useSpeed) {
                            param.result = LocationUtil.speed
                            XposedBridge.log("\tModified to: ${LocationUtil.speed}")
                        }
                    }
                })

            XposedHelpers.findAndHookMethod(
                locationClass,
                "getSpeedAccuracyMetersPerSecond",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!shouldSpoofCurrentPackage()) return
                        LocationUtil.updateLocation(appLpparam.packageName)
                        XposedBridge.log("$tag Leaving method getSpeedAccuracyMetersPerSecond()")
                        XposedBridge.log("\tOriginal speed accuracy: ${param.result as Float}")
                        if (LocationUtil.useSpeedAccuracy) {
                            param.result = LocationUtil.speedAccuracy
                            XposedBridge.log("\tModified to: ${LocationUtil.speedAccuracy}")
                        }
                    }
                })

            hookOptionalLocationMethod(locationClass, "getMslAltitudeMeters", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!shouldSpoofCurrentPackage()) return
                    LocationUtil.updateLocation(appLpparam.packageName)
                    XposedBridge.log("$tag Leaving method getMslAltitudeMeters()")
                    val originalMslAltitude = param.result as? Double
                    XposedBridge.log("\tOriginal MSL altitude: $originalMslAltitude")
                    if (LocationUtil.useMeanSeaLevel) {
                        param.result = LocationUtil.meanSeaLevel
                        XposedBridge.log("\tModified to: ${LocationUtil.meanSeaLevel}")
                    }
                }
            })

            hookOptionalLocationMethod(locationClass, "getMslAltitudeAccuracyMeters", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!shouldSpoofCurrentPackage()) return
                    LocationUtil.updateLocation(appLpparam.packageName)
                    XposedBridge.log("$tag Leaving method getMslAltitudeAccuracyMeters()")
                    val originalMslAltitudeAccuracy = param.result as? Float
                    XposedBridge.log("\tOriginal MSL altitude accuracy: $originalMslAltitudeAccuracy")
                    if (LocationUtil.useMeanSeaLevelAccuracy) {
                        param.result = LocationUtil.meanSeaLevelAccuracy
                        XposedBridge.log("\tModified to: ${LocationUtil.meanSeaLevelAccuracy}")
                    }
                }
            })

        } catch (e: Exception) {
            XposedBridge.log("$tag Error hooking Location class - ${e.message}")
        }
    }

    private fun hookOptionalLocationMethod(
        locationClass: Class<*>,
        methodName: String,
        callback: XC_MethodHook
    ) {
        val methodExists = locationClass.methods.any { it.name == methodName && it.parameterTypes.isEmpty() } ||
            locationClass.declaredMethods.any { it.name == methodName && it.parameterTypes.isEmpty() }
        if (!methodExists) return

        XposedHelpers.findAndHookMethod(locationClass, methodName, callback)
    }

    private fun hookLocationManager(classLoader: ClassLoader) {
        try {
            val locationManagerClass = XposedHelpers.findClass("android.location.LocationManager", classLoader)

            XposedHelpers.findAndHookMethod(
                locationManagerClass,
                "getLastKnownLocation",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!shouldSpoofCurrentPackage()) return
                        XposedBridge.log("$tag Leaving method getLastKnownLocation(provider)")
                        XposedBridge.log("\t Original location: ${param.result as? Location}")
                        val provider = param.args[0] as String
                        XposedBridge.log("\t Requested data from: $provider")
                        val fakeLocation = LocationUtil.createFakeLocation(provider = provider, packageName = appLpparam.packageName)
                        param.result = fakeLocation
                        XposedBridge.log("\t Modified location: $fakeLocation")
                    }
                })

        } catch (e: Exception) {
            XposedBridge.log("$tag Error hooking LocationManager - ${e.message}")
        }
    }

    private fun shouldSpoofCurrentPackage(): Boolean {
        return LocationUtil.shouldSpoofPackage(appLpparam.packageName)
    }
}
