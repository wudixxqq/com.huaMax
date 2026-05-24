// MainHook.kt
package com.noobexon.xposedfakelocation.xposed

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.noobexon.xposedfakelocation.data.MANAGER_APP_PACKAGE_NAME
import com.noobexon.xposedfakelocation.xposed.hooks.LocationApiHooks
import com.noobexon.xposedfakelocation.xposed.hooks.PhoneServicesHooks
import com.noobexon.xposedfakelocation.xposed.hooks.SystemServicesHooks
import com.noobexon.xposedfakelocation.xposed.utils.LocationUtil
import com.noobexon.xposedfakelocation.xposed.utils.PreferencesUtil
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage {
    private val tag = "[MainHook]"

    lateinit var context: Context

    private var locationApiHooks: LocationApiHooks? = null
    private var systemServicesHooks: SystemServicesHooks? = null
    private var phoneServicesHooks: PhoneServicesHooks? = null

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        val useInAppTargetApps = PreferencesUtil.getUseInAppTargetApps()
        when (lpparam.packageName) {
            "android" -> {
                if (useInAppTargetApps) {
                    XposedBridge.log("$tag Mode=IN_APP_TARGET_LIST | Installing system-server location hooks (android).")
                    systemServicesHooks = SystemServicesHooks(lpparam).also { it.initHooks() }
                } else {
                    XposedBridge.log("$tag Mode=LSPOSED_SCOPE_ONLY | Skipping system-server hooks (android). Selection is driven solely by LSPosed scope.")
                }
                return
            }
            "com.android.phone" -> {
                if (useInAppTargetApps) {
                    XposedBridge.log("$tag Mode=IN_APP_TARGET_LIST | Installing phone-process location side-channel hooks (com.android.phone).")
                    phoneServicesHooks = PhoneServicesHooks(lpparam).also { it.initHooks() }
                } else {
                    XposedBridge.log("$tag Mode=LSPOSED_SCOPE_ONLY | Skipping phone-process hooks (com.android.phone).")
                }
                return
            }
            MANAGER_APP_PACKAGE_NAME -> return
            else -> {
                XposedBridge.log("$tag Mode=${if (useInAppTargetApps) "IN_APP_TARGET_LIST" else "LSPOSED_SCOPE_ONLY"} | Installing in-process location hooks for ${lpparam.packageName}")
                initHookingLogic(lpparam)
            }
        }
    }

    private fun initHookingLogic(lpparam: LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "android.app.Instrumentation",
            lpparam.classLoader,
            "callApplicationOnCreate",
            Application::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    context = (param.args[0] as Application).applicationContext.also {
                        XposedBridge.log("$tag Target App's context has been acquired (${lpparam.packageName}).")
                        if (!PreferencesUtil.getHideFakeLocationToast() &&
                            LocationUtil.shouldSpoofPackage(lpparam.packageName)
                        ) {
                            Toast.makeText(it, "Fake Location Is Active!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    locationApiHooks = LocationApiHooks(lpparam).also { it.initHooks() }
                }
            }
        )
    }
}
