// SystemServicesHooks.kt
package com.noobexon.xposedfakelocation.xposed.hooks

import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiInfo
import android.os.Build
import android.telephony.CellInfo
import android.util.ArrayMap
import com.noobexon.xposedfakelocation.xposed.utils.LocationUtil
import dalvik.system.PathClassLoader
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.Field
import java.lang.reflect.Method

class SystemServicesHooks(val appLpparam: LoadPackageParam) {
    private val tag = "[SystemServicesHooks]"

    fun initHooks() {
        val classLoader = appLpparam.classLoader
        hookLastLocation(classLoader)
        hookCurrentLocation(classLoader)
        hookLocationDispatch(classLoader)
        hookMiuiLocationServices(classLoader)
        hookWifiServices(classLoader)
        hookGnssRegistration(classLoader)
        hookGeofence(classLoader)
        XposedBridge.log("$tag Instantiated hooks successfully")
    }

    private fun hookLastLocation(classLoader: ClassLoader) {
        val serviceClass = findClass(
            classLoader,
            "com.android.server.location.LocationManagerService",
            "com.android.server.LocationManagerService"
        ) ?: return

        hookAll(serviceClass, "getLastLocation", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!shouldSpoofArgs(param.args)) return

                val original = param.result as? Location
                param.result = LocationUtil.createFakeLocation(original)
                XposedBridge.log("$tag Replaced getLastLocation result.")
            }
        })
    }

    private fun hookCurrentLocation(classLoader: ClassLoader) {
        val serviceClass = findClass(
            classLoader,
            "com.android.server.location.LocationManagerService",
            "com.android.server.LocationManagerService"
        ) ?: return

        hookAll(serviceClass, "getCurrentLocation", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!shouldSpoofArgs(param.args)) return

                param.result = defaultReturnValue(param.method as? Method)
                XposedBridge.log("$tag Blocked getCurrentLocation request for spoofed target.")
            }
        })
    }

    private fun hookLocationDispatch(classLoader: ClassLoader) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hookLocationProviderManager(classLoader)
        }
        hookReceiverCallbacks(classLoader)
    }

    private fun hookLocationProviderManager(classLoader: ClassLoader) {
        val providerClass = findClass(
            classLoader,
            "com.android.server.location.provider.LocationProviderManager"
        ) ?: return

        hookAll(providerClass, "onReportLocation", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!LocationUtil.isSpoofingEnabled()) return
                val locationResult = param.args.firstOrNull() ?: return
                val registrationsField = findField(providerClass, "mRegistrations") ?: return
                val registrations = registrationsField.get(param.thisObject) as? Map<*, *> ?: return

                val locationsField = findField(locationResult.javaClass, "mLocations") ?: return
                val originalLocations = locationsField.get(locationResult) as? List<*> ?: return
                val original = originalLocations.firstOrNull() as? Location
                val fakeLocation = LocationUtil.createFakeLocation(original)
                val originalRegistrations = ArrayMap<Any?, Any?>()
                val passthroughRegistrations = ArrayMap<Any?, Any?>()

                registrations.forEach { (key, value) ->
                    originalRegistrations[key] = value
                    val packageNames = collectPackageNames(value)
                    val spoofedPackage = packageNames.firstOrNull(LocationUtil::shouldSpoofPackage)
                    if (spoofedPackage != null) {
                        locationsField.set(locationResult, arrayListOf(fakeLocation))
                        deliverLocationToRegistration(value, locationResult)
                        XposedBridge.log("$tag Delivered spoofed provider location to $spoofedPackage.")
                    } else {
                        passthroughRegistrations[key] = value
                    }
                }

                locationsField.set(locationResult, ArrayList(originalLocations))
                param.setObjectExtra("target_apps_original_registrations", originalRegistrations)
                registrationsField.set(param.thisObject, passthroughRegistrations)
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                val originalRegistrations = param.getObjectExtra("target_apps_original_registrations") as? Map<*, *>
                    ?: return
                val registrationsField = findField(providerClass, "mRegistrations") ?: return
                registrationsField.set(param.thisObject, originalRegistrations)
            }
        })
    }

    private fun hookMiuiLocationServices(classLoader: ClassLoader) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (!isXiaomiFamilyDevice()) {
            XposedBridge.log("$tag Skipping MIUI location hooks on non-Xiaomi device.")
            return
        }

        val miuiClass = findClass(
            classLoader,
            "com.android.server.location.MiuiBlurLocationManagerImpl",
            "com.android.server.location.MiuiBlurLocationManager"
        ) ?: return

        hookAll(miuiClass, "getBlurryLocation", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!shouldSpoofArgs(param.args)) return
                param.result = replaceLocationLikeResult(param.result, param.method as? Method)
                XposedBridge.log("$tag Replaced MIUI blurry location result.")
            }
        })

        hookAll(miuiClass, "getBlurryCellLocation", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!shouldSpoofArgs(param.args)) return
                param.result = null
                XposedBridge.log("$tag Cleared MIUI blurry cell location result.")
            }
        })

        hookAll(miuiClass, "getBlurryCellInfos", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!shouldSpoofArgs(param.args)) return
                param.result = emptyList<CellInfo>()
                XposedBridge.log("$tag Cleared MIUI blurry cell info result.")
            }
        })

        hookAll(miuiClass, "handleGpsLocationChangedLocked", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!shouldSpoofArgs(param.args)) return
                param.result = defaultReturnValue(param.method as? Method)
                XposedBridge.log("$tag Blocked MIUI GPS location refresh while spoofing.")
            }
        })
    }

    private fun hookReceiverCallbacks(classLoader: ClassLoader) {
        val receiverClass = findClass(
            classLoader,
            "com.android.server.location.LocationManagerService\$Receiver",
            "com.android.server.LocationManagerService\$Receiver"
        ) ?: return

        hookAll(receiverClass, "callLocationChangedLocked", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (collectPackageNames(param.thisObject).none(LocationUtil::shouldSpoofPackage)) return

                val locationArgIndex = param.args.indexOfFirst { it is Location }
                if (locationArgIndex == -1) return

                val original = param.args[locationArgIndex] as? Location
                param.args[locationArgIndex] = LocationUtil.createFakeLocation(original)
                XposedBridge.log("$tag Replaced Receiver.callLocationChangedLocked argument.")
            }
        })
    }

    private fun hookGnssRegistration(classLoader: ClassLoader) {
        val serviceClasses = listOfNotNull(
            findClass(classLoader, "com.android.server.location.gnss.GnssManagerService"),
            findClass(
                classLoader,
                "com.android.server.location.LocationManagerService",
                "com.android.server.LocationManagerService"
            )
        ).distinct()

        val methodsToBlock = listOf(
            "addGnssBatchingCallback",
            "addGnssMeasurementsListener",
            "addGnssNavigationMessageListener",
            "addGnssAntennaInfoListener",
            "registerGnssStatusCallback",
            "registerGnssNmeaCallback"
        )

        serviceClasses.forEach { serviceClass ->
            methodsToBlock.forEach { methodName ->
                hookAll(serviceClass, methodName, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!shouldSpoofArgs(param.args)) return
                        param.result = defaultReturnValue(param.method as? Method)
                        XposedBridge.log("$tag Blocked $methodName while spoofing is enabled.")
                    }
                })
            }
        }
    }

    private fun hookWifiServices(classLoader: ClassLoader) {
        val systemServiceManagerClass = findClass(
            classLoader,
            "com.android.server.SystemServiceManager"
        ) ?: return

        hookAll(systemServiceManagerClass, "loadClassFromLoader", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val serviceName = param.args.getOrNull(0) as? String ?: return
                if (serviceName != "com.android.server.wifi.WifiService") return

                val serviceClassLoader = param.args.getOrNull(1) as? PathClassLoader ?: return
                val wifiServiceClass = findClass(
                    serviceClassLoader,
                    "com.android.server.wifi.WifiServiceImpl"
                ) ?: return

                hookWifiServiceImpl(wifiServiceClass)
            }
        })
    }

    private fun hookWifiServiceImpl(wifiServiceClass: Class<*>) {
        hookAll(wifiServiceClass, "getScanResults", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!shouldSpoofArgs(param.args)) return
                param.result = emptyList<Any>()
                XposedBridge.log("$tag Cleared Wi-Fi scan results while spoofing.")
            }
        })

        hookAll(wifiServiceClass, "getConnectionInfo", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!shouldSpoofArgs(param.args)) return
                // TODO: These Wi-Fi identity values are hardcoded as a temporary fallback.
                // Expose them as user-configurable settings in the manager app.
                param.result = WifiInfo.Builder()
                    .setBssid("02:00:00:00:00:00")
                    .setSsid("AndroidAP".toByteArray())
                    .setRssi(-60)
                    .setNetworkId(0)
                    .build()
                XposedBridge.log("$tag Replaced Wi-Fi connection info while spoofing.")
            }
        })
    }

    private fun hookGeofence(classLoader: ClassLoader) {
        val serviceClass = findClass(
            classLoader,
            "com.android.server.location.LocationManagerService",
            "com.android.server.LocationManagerService"
        ) ?: return

        hookAll(serviceClass, "requestGeofence", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!shouldSpoofArgs(param.args)) return
                param.result = defaultReturnValue(param.method as? Method)
                XposedBridge.log("$tag Blocked geofence registration while spoofing is enabled.")
            }
        })
    }

    private fun isXiaomiFamilyDevice(): Boolean {
        val markers = listOf("xiaomi", "redmi", "poco")
        val buildInfo = listOf(
            Build.MANUFACTURER.orEmpty(),
            Build.BRAND.orEmpty(),
            Build.PRODUCT.orEmpty(),
            Build.DEVICE.orEmpty()
        )
        return buildInfo.any { info ->
            val lower = info.lowercase()
            markers.any(lower::contains)
        }
    }

    private fun hookAll(clazz: Class<*>, methodName: String, callback: XC_MethodHook) {
        try {
            val hooks = XposedBridge.hookAllMethods(clazz, methodName, callback)
            if (hooks.isNotEmpty()) {
                XposedBridge.log("$tag Hooked ${clazz.name}#$methodName (${hooks.size} overloads).")
            }
        } catch (e: Throwable) {
            XposedBridge.log("$tag Failed hooking ${clazz.name}#$methodName: ${e.message}")
        }
    }

    private fun findClass(classLoader: ClassLoader, vararg names: String): Class<*>? {
        names.forEach { name ->
            try {
                return XposedHelpers.findClass(name, classLoader)
            } catch (_: Throwable) {
                // Try the next framework class name. AOSP moved these across releases.
            }
        }
        XposedBridge.log("$tag None of these classes were found: ${names.joinToString()}")
        return null
    }

    private fun findField(clazz: Class<*>, fieldName: String): Field? {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        return null
    }

    private fun deliverLocationToRegistration(registration: Any?, locationResult: Any) {
        if (registration == null) return
        runCatching {
            val acceptMethod = registration.javaClass.methods.firstOrNull { it.name == "acceptLocationChange" }
                ?: registration.javaClass.declaredMethods.firstOrNull { it.name == "acceptLocationChange" }
                ?: return
            acceptMethod.isAccessible = true
            val operation = acceptMethod.invoke(registration, locationResult)

            val executeMethod = registration.javaClass.methods.firstOrNull { it.name == "executeOperation" }
                ?: registration.javaClass.declaredMethods.firstOrNull { it.name == "executeOperation" }
                ?: return
            executeMethod.isAccessible = true
            executeMethod.invoke(registration, operation)
        }.onFailure {
            XposedBridge.log("$tag Failed delivering spoofed provider location: ${it.message}")
        }
    }

    private fun shouldSpoofArgs(args: Array<Any?>?): Boolean {
        return args?.asSequence()
            ?.flatMap { collectPackageNames(it).asSequence() }
            ?.distinct()
            ?.any(LocationUtil::shouldSpoofPackage) == true
    }

    private fun collectPackageNames(value: Any?): Set<String> {
        return collectPackageNames(value, mutableSetOf(), 0)
    }

    private fun collectPackageNames(value: Any?, visited: MutableSet<Int>, depth: Int): Set<String> {
        if (value == null || depth > 5) return emptySet()
        if (value is String) return setOfNotNull(value.takeIf(::looksLikePackageName))

        val identity = System.identityHashCode(value)
        if (!visited.add(identity)) return emptySet()

        val packageNames = linkedSetOf<String>()

        if (value is Iterable<*>) {
            value.forEach { packageNames += collectPackageNames(it, visited, depth + 1) }
            return packageNames
        }

        if (value is Map<*, *>) {
            value.forEach { (key, mapValue) ->
                packageNames += collectPackageNames(key, visited, depth + 1)
                packageNames += collectPackageNames(mapValue, visited, depth + 1)
            }
            return packageNames
        }

        packageNames += collectWorkSourcePackageNames(value)

        listOf(
            "mPackageName",
            "packageName",
            "callingPackage",
            "mCallingPackage",
            "mCallerPackageName",
            "callerPackageName",
            "mOpPackageName",
            "opPackageName"
        ).forEach { fieldName ->
            val packageName = findField(value.javaClass, fieldName)?.get(value) as? String
            packageName?.takeIf(::looksLikePackageName)?.let(packageNames::add)
        }

        listOf(
            "getPackageName",
            "getCallingPackage",
            "getCallerPackageName",
            "getOpPackageName"
        ).forEach { methodName ->
            val packageName = runCatching {
                findMethod(value.javaClass, methodName)?.invoke(value) as? String
            }.getOrNull()
            packageName?.takeIf(::looksLikePackageName)?.let(packageNames::add)
        }

        listOf(
            "mIdentity",
            "mCallerIdentity",
            "callerIdentity",
            "identity",
            "mCallingIdentity",
            "callingIdentity",
            "mAttributionSource",
            "attributionSource",
            "mNext",
            "next",
            "mWorkSource",
            "workSource",
            "mRequest",
            "request",
            "mLocationRequest",
            "locationRequest"
        ).forEach { fieldName ->
            packageNames += collectPackageNames(findField(value.javaClass, fieldName)?.get(value), visited, depth + 1)
        }

        listOf("getAttributionSource", "getNext", "getWorkSource", "getLocationRequest").forEach { methodName ->
            val nestedValue = runCatching {
                findMethod(value.javaClass, methodName)?.invoke(value)
            }.getOrNull()
            packageNames += collectPackageNames(nestedValue, visited, depth + 1)
        }

        return packageNames
    }

    private fun collectWorkSourcePackageNames(value: Any): Set<String> {
        if (value.javaClass.name != "android.os.WorkSource") return emptySet()

        val packageNames = linkedSetOf<String>()
        val size = runCatching {
            findMethod(value.javaClass, "size")?.invoke(value) as? Int
        }.getOrNull() ?: return emptySet()

        repeat(size) { index ->
            val name = runCatching {
                findMethod(value.javaClass, "getName", Integer.TYPE)?.invoke(value, index) as? String
            }.getOrNull()
            name?.takeIf(::looksLikePackageName)?.let(packageNames::add)
        }

        return packageNames
    }

    private fun findMethod(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method? {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredMethod(methodName, *parameterTypes).apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                currentClass = currentClass.superclass
            }
        }

        return clazz.methods.firstOrNull {
            it.name == methodName && it.parameterTypes.contentEquals(parameterTypes)
        }?.apply { isAccessible = true }
    }

    private fun looksLikePackageName(value: String?): Boolean {
        return value != null && "." in value && !value.startsWith("android.location.")
    }

    private fun replaceLocationLikeResult(result: Any?, method: Method?): Any? {
        if (result is Location) {
            return LocationUtil.createFakeLocation(result)
        }

        if (result != null) {
            val locationsField = findField(result.javaClass, "mLocations")
            val originalLocations = locationsField?.get(result) as? List<*>
            val original = originalLocations?.firstOrNull() as? Location
            if (locationsField != null) {
                locationsField.set(result, arrayListOf(LocationUtil.createFakeLocation(original)))
                return result
            }

            if (result is List<*>) {
                val original = result.firstOrNull() as? Location
                return listOf(LocationUtil.createFakeLocation(original))
            }

            runCatching {
                val sizeMethod = result.javaClass.methods.firstOrNull { it.name == "size" && it.parameterTypes.isEmpty() }
                val getMethod = result.javaClass.methods.firstOrNull { it.name == "get" && it.parameterTypes.size == 1 }
                val size = sizeMethod?.invoke(result) as? Int ?: return@runCatching
                if (size > 0) {
                    val originalLocation = getMethod?.invoke(result, 0) as? Location ?: return@runCatching
                    val fakeLocation = LocationUtil.createFakeLocation(originalLocation)
                    originalLocation.latitude = fakeLocation.latitude
                    originalLocation.longitude = fakeLocation.longitude
                    originalLocation.altitude = fakeLocation.altitude
                    originalLocation.accuracy = fakeLocation.accuracy
                    originalLocation.speed = fakeLocation.speed
                }
            }.onFailure {
                XposedBridge.log("$tag Could not inspect MIUI location container: ${it.message}")
            }

            return result
        }

        return if (method?.returnType?.let { Location::class.java.isAssignableFrom(it) } == true) {
            LocationUtil.createFakeLocation(provider = LocationManager.FUSED_PROVIDER)
        } else {
            null
        }
    }

    private fun defaultReturnValue(method: Method?): Any? {
        return when (method?.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0F
            java.lang.Double.TYPE -> 0.0
            else -> null
        }
    }
}
