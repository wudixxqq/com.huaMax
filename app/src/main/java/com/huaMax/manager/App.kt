package com.huaMax.manager

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import com.huaMax.data.KEY_ENABLE_SYSTEM_HOOKS
import com.huaMax.data.KEY_IS_PLAYING
import com.huaMax.data.REMOTE_PREFS_GROUP
import com.huaMax.data.SHARED_PREFS_FILE
import com.huaMax.data.SYSTEM_HOOK_PACKAGES
import com.huaMax.data.auth.AuthorizationManager
import com.huaMax.data.remote.RemoteControlManager
import com.huaMax.manager.mock.MockLocationService
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class App : Application(), XposedServiceHelper.OnServiceListener {
    companion object {
        private const val TAG = "LocationMaxApp"
        private val _serviceState = MutableStateFlow<XposedService?>(null)
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        @Volatile private var isRepairingSystemScope = false
        val serviceState: StateFlow<XposedService?> = _serviceState.asStateFlow()
        val service: XposedService? get() = _serviceState.value   // keep existing callers working
    }

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)   // exactly once
        if (getSharedPreferences(SHARED_PREFS_FILE, MODE_PRIVATE).getBoolean(KEY_IS_PLAYING, false)) {
            MockLocationService.sync(this, true)
        }
    }

    override fun onServiceBind(service: XposedService) {
        _serviceState.value = service
        val remotePrefs = service.getRemotePreferences(REMOTE_PREFS_GROUP)
        AuthorizationManager.syncLocalAuthorizationToRemote(this, remotePrefs)
        RemoteControlManager.syncLocalControlToRemote(this, remotePrefs)
        repairMissingSystemScopeIfEnabled(service, remotePrefs)
    }

    override fun onServiceDied(service: XposedService) {
        _serviceState.value = null
    }

    private fun repairMissingSystemScopeIfEnabled(
        service: XposedService,
        remotePrefs: SharedPreferences
    ) {
        if (!remotePrefs.getBoolean(KEY_ENABLE_SYSTEM_HOOKS, false)) return
        if (isRepairingSystemScope) return

        isRepairingSystemScope = true
        applicationScope.launch {
            val missingPackages = try {
                val currentScope = service.scope.toSet()
                SYSTEM_HOOK_PACKAGES.filterNot(currentScope::contains)
            } catch (e: XposedService.ServiceException) {
                isRepairingSystemScope = false
                Log.w(TAG, "Failed to read LSPosed scope for system hook repair", e)
                return@launch
            }

            if (missingPackages.isEmpty()) {
                isRepairingSystemScope = false
                return@launch
            }

            Log.i(TAG, "Requesting missing system hook scopes: $missingPackages")
            val callback = object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: List<String>) {
                    isRepairingSystemScope = false
                    Log.i(TAG, "System hook scope repair approved: $approved")
                }

                override fun onScopeRequestFailed(message: String) {
                    isRepairingSystemScope = false
                    Log.w(TAG, "System hook scope repair failed: $message")
                }
            }

            try {
                service.requestScope(missingPackages, callback)
            } catch (e: XposedService.ServiceException) {
                isRepairingSystemScope = false
                Log.w(TAG, "Failed to request missing system hook scopes", e)
            }
        }
    }
}
