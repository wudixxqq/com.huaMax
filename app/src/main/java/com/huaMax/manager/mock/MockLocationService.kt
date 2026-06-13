package com.huaMax.manager.mock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.huaMax.R
import com.huaMax.data.DEFAULT_ACCURACY
import com.huaMax.data.KEY_ACCURACY
import com.huaMax.data.KEY_AUTH_EXPIRES_AT_MILLIS
import com.huaMax.data.KEY_AUTH_LAST_SEEN_MILLIS
import com.huaMax.data.KEY_IS_PLAYING
import com.huaMax.data.KEY_LAST_CLICKED_LOCATION
import com.huaMax.data.KEY_REMOTE_CONTROL_BLOCKED
import com.huaMax.data.KEY_USE_ACCURACY
import com.huaMax.data.SHARED_PREFS_FILE
import com.huaMax.data.auth.AuthorizationManager
import com.huaMax.data.model.LastClickedLocation

class MockLocationService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val gson = Gson()
    private lateinit var locationManager: LocationManager

    private val tick = object : Runnable {
        override fun run() {
            val state = readState()
            if (!state.canSpoof || state.location == null) {
                stopMockProviders()
                stopSelf()
                return
            }

            if (pushMockLocation(state.location, state.accuracy)) {
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            } else {
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LocationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMockProviders()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                handler.removeCallbacks(tick)
                handler.post(tick)
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        stopMockProviders()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun readState(): MockState {
        val prefs = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
        val authStatus = AuthorizationManager.getStatus(prefs, updateLastSeen = false)
        val authorized = authStatus is AuthorizationManager.ValidationResult.Valid &&
            System.currentTimeMillis() <= authStatus.expiresAtMillis
        val clockOk = prefs.getLong(KEY_AUTH_LAST_SEEN_MILLIS, 0L).let { lastSeen ->
            lastSeen <= 0L || System.currentTimeMillis() + CLOCK_ROLLBACK_GRACE_MILLIS >= lastSeen
        }
        val canSpoof = prefs.getBoolean(KEY_IS_PLAYING, false) &&
            !prefs.getBoolean(KEY_REMOTE_CONTROL_BLOCKED, false) &&
            authorized &&
            clockOk

        val location = prefs.getString(KEY_LAST_CLICKED_LOCATION, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { gson.fromJson(it, LastClickedLocation::class.java) }.getOrNull() }

        val accuracy = if (prefs.getBoolean(KEY_USE_ACCURACY, false)) {
            readDouble(prefs, KEY_ACCURACY, DEFAULT_ACCURACY).toFloat()
        } else {
            DEFAULT_ACCURACY.toFloat()
        }.takeIf { it > 0f } ?: DEFAULT_MOCK_ACCURACY_METERS

        return MockState(canSpoof = canSpoof, location = location, accuracy = accuracy)
    }

    private fun pushMockLocation(location: LastClickedLocation, accuracy: Float): Boolean {
        var pushed = false
        TARGET_PROVIDERS.forEach { provider ->
            val providerReady = ensureMockProvider(provider)
            if (providerReady) {
                runCatching {
                    locationManager.setTestProviderLocation(provider, buildLocation(provider, location, accuracy))
                    pushed = true
                }.onFailure {
                    Log.w(TAG, "Could not set mock location for $provider: ${it.message}")
                }
            }
        }
        return pushed
    }

    @Suppress("DEPRECATION")
    private fun ensureMockProvider(provider: String): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.addTestProvider(provider, providerProperties())
            } else {
                locationManager.addTestProvider(
                    provider,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
                    true,
                    android.location.Criteria.POWER_LOW,
                    android.location.Criteria.ACCURACY_FINE
                )
            }
        }.recoverCatching {
            // Existing mock providers throw on add; enabling and setting below is enough.
        }.mapCatching {
            locationManager.setTestProviderEnabled(provider, true)
            true
        }.onFailure {
            Log.e(TAG, "Mock provider $provider is not available: ${it.message}")
        }.getOrDefault(false)
    }

    private fun providerProperties(): ProviderProperties {
        return ProviderProperties.Builder()
            .setAccuracy(ProviderProperties.ACCURACY_FINE)
            .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
            .setHasAltitudeSupport(true)
            .setHasSpeedSupport(true)
            .setHasBearingSupport(true)
            .build()
    }

    private fun buildLocation(
        provider: String,
        location: LastClickedLocation,
        accuracy: Float
    ): Location {
        return Location(provider).apply {
            latitude = location.latitude
            longitude = location.longitude
            this.accuracy = accuracy
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
    }

    private fun stopMockProviders() {
        TARGET_PROVIDERS.forEach { provider ->
            runCatching { locationManager.removeTestProvider(provider) }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LocationMax mock location",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("LocationMax")
        .setContentText("Mock location provider is running")
        .setOngoing(true)
        .setSilent(true)
        .build()

    private fun readDouble(
        prefs: android.content.SharedPreferences,
        key: String,
        default: Double
    ): Double {
        val bits = prefs.getLong(key, java.lang.Double.doubleToRawLongBits(default))
        return java.lang.Double.longBitsToDouble(bits)
    }

    private data class MockState(
        val canSpoof: Boolean,
        val location: LastClickedLocation?,
        val accuracy: Float
    )

    companion object {
        private const val TAG = "MockLocationService"
        private const val ACTION_START = "com.huaMax.action.MOCK_PROVIDER_START"
        private const val ACTION_STOP = "com.huaMax.action.MOCK_PROVIDER_STOP"
        private const val CHANNEL_ID = "locationmax_mock_provider"
        private const val NOTIFICATION_ID = 2001
        private const val UPDATE_INTERVAL_MS = 1_000L
        private const val DEFAULT_MOCK_ACCURACY_METERS = 5f
        private const val CLOCK_ROLLBACK_GRACE_MILLIS = 10L * 60L * 1000L

        private val TARGET_PROVIDERS = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.FUSED_PROVIDER
        )

        fun sync(context: Context, enabled: Boolean) {
            val appContext = context.applicationContext
            val action = if (enabled) ACTION_START else ACTION_STOP
            val intent = Intent(appContext, MockLocationService::class.java).setAction(action)
            try {
                if (enabled) {
                    ContextCompat.startForegroundService(appContext, intent)
                } else {
                    appContext.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not sync mock provider service: ${e.message}")
            }
        }
    }
}
