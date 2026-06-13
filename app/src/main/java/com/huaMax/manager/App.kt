package com.huaMax.manager

import android.app.Application
import com.huaMax.data.REMOTE_PREFS_GROUP
import com.huaMax.data.KEY_IS_PLAYING
import com.huaMax.data.SHARED_PREFS_FILE
import com.huaMax.data.auth.AuthorizationManager
import com.huaMax.data.remote.RemoteControlManager
import com.huaMax.manager.mock.MockLocationService
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class App : Application(), XposedServiceHelper.OnServiceListener {
    companion object {
        private val _serviceState = MutableStateFlow<XposedService?>(null)
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
    }

    override fun onServiceDied(service: XposedService) {
        _serviceState.value = null
    }
}
