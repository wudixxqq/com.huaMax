package com.huaMax.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.huaMax.BuildConfig
import com.huaMax.data.KEY_IS_PLAYING
import com.huaMax.data.KEY_REMOTE_CONTROL_BLOCKED
import com.huaMax.data.KEY_REMOTE_CONTROL_LAST_CHECK_MILLIS
import com.huaMax.data.KEY_REMOTE_CONTROL_LATEST_VERSION_NAME
import com.huaMax.data.KEY_REMOTE_CONTROL_MESSAGE
import com.huaMax.data.KEY_REMOTE_CONTROL_REASON
import com.huaMax.data.KEY_REMOTE_CONTROL_UPDATE_URL
import com.huaMax.data.REMOTE_CONTROL_URL
import com.huaMax.data.SHARED_PREFS_FILE
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object RemoteControlManager {
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 8_000
    private const val FAIL_CLOSED_ON_NETWORK_ERROR = false

    sealed interface GateState {
        object Allowed : GateState

        data class Blocked(
            val reason: Reason,
            val message: String,
            val latestVersionName: String = "",
            val updateUrl: String = ""
        ) : GateState
    }

    enum class Reason {
        DISABLED,
        UPDATE_REQUIRED,
        NETWORK_ERROR
    }

    private data class RemoteConfig(
        val enabled: Boolean,
        val minVersionCode: Int,
        val latestVersionName: String,
        val updateUrl: String,
        val message: String
    )

    suspend fun checkAndApply(context: Context): GateState {
        val appContext = context.applicationContext
        val state = check(appContext)
        saveGateState(appContext, state)
        return state
    }

    fun syncLocalControlToRemote(context: Context, remotePrefs: SharedPreferences) {
        val localPrefs = localPrefs(context)
        remotePrefs.edit()
            .putBoolean(KEY_REMOTE_CONTROL_BLOCKED, localPrefs.getBoolean(KEY_REMOTE_CONTROL_BLOCKED, false))
            .putString(KEY_REMOTE_CONTROL_REASON, localPrefs.getString(KEY_REMOTE_CONTROL_REASON, ""))
            .putString(KEY_REMOTE_CONTROL_MESSAGE, localPrefs.getString(KEY_REMOTE_CONTROL_MESSAGE, ""))
            .putString(KEY_REMOTE_CONTROL_UPDATE_URL, localPrefs.getString(KEY_REMOTE_CONTROL_UPDATE_URL, ""))
            .putString(
                KEY_REMOTE_CONTROL_LATEST_VERSION_NAME,
                localPrefs.getString(KEY_REMOTE_CONTROL_LATEST_VERSION_NAME, "")
            )
            .putLong(KEY_REMOTE_CONTROL_LAST_CHECK_MILLIS, localPrefs.getLong(KEY_REMOTE_CONTROL_LAST_CHECK_MILLIS, 0L))
            .apply()
    }

    private suspend fun check(context: Context): GateState = withContext(Dispatchers.IO) {
        if (REMOTE_CONTROL_URL.isBlank()) return@withContext GateState.Allowed

        try {
            evaluate(fetchConfig())
        } catch (e: IOException) {
            cachedBlockingState(context) ?: if (FAIL_CLOSED_ON_NETWORK_ERROR) {
                GateState.Blocked(
                    reason = Reason.NETWORK_ERROR,
                    message = "无法连接控制服务器，请联网后重试。"
                )
            } else {
                GateState.Allowed
            }
        } catch (e: RuntimeException) {
            cachedBlockingState(context) ?: if (FAIL_CLOSED_ON_NETWORK_ERROR) {
                GateState.Blocked(
                    reason = Reason.NETWORK_ERROR,
                    message = "控制配置读取失败，请联网后重试。"
                )
            } else {
                GateState.Allowed
            }
        }
    }

    private fun fetchConfig(): RemoteConfig {
        val connection = (URI.create(REMOTE_CONTROL_URL).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("User-Agent", "LocationMax/${BuildConfig.VERSION_NAME}")
        }

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Remote control HTTP $responseCode")
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseConfig(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseConfig(body: String): RemoteConfig {
        val json = JSONObject(body)
        return RemoteConfig(
            enabled = json.optBoolean("enabled", true),
            minVersionCode = json.optInt("minVersionCode", 1),
            latestVersionName = json.optString("latestVersionName", ""),
            updateUrl = json.optString("updateUrl", ""),
            message = json.optString("message", "")
        )
    }

    private fun evaluate(config: RemoteConfig): GateState {
        if (!config.enabled) {
            return GateState.Blocked(
                reason = Reason.DISABLED,
                message = config.message.ifBlank { "LocationMax 已被管理员停用。" },
                latestVersionName = config.latestVersionName,
                updateUrl = config.updateUrl
            )
        }

        if (BuildConfig.VERSION_CODE < config.minVersionCode) {
            return GateState.Blocked(
                reason = Reason.UPDATE_REQUIRED,
                message = config.message.ifBlank { "当前版本过低，请更新后继续使用。" },
                latestVersionName = config.latestVersionName,
                updateUrl = config.updateUrl
            )
        }

        return GateState.Allowed
    }

    private fun cachedBlockingState(context: Context): GateState.Blocked? {
        val prefs = localPrefs(context)
        if (!prefs.getBoolean(KEY_REMOTE_CONTROL_BLOCKED, false)) return null

        val reason = runCatching {
            Reason.valueOf(prefs.getString(KEY_REMOTE_CONTROL_REASON, "").orEmpty())
        }.getOrDefault(Reason.DISABLED)

        return GateState.Blocked(
            reason = reason,
            message = prefs.getString(KEY_REMOTE_CONTROL_MESSAGE, null).orEmpty()
                .ifBlank { "LocationMax 当前不可用，请联系管理员。" },
            latestVersionName = prefs.getString(KEY_REMOTE_CONTROL_LATEST_VERSION_NAME, null).orEmpty(),
            updateUrl = prefs.getString(KEY_REMOTE_CONTROL_UPDATE_URL, null).orEmpty()
        )
    }

    private fun saveGateState(context: Context, state: GateState) {
        val prefs = localPrefs(context)
        val editor = prefs.edit()
            .putLong(KEY_REMOTE_CONTROL_LAST_CHECK_MILLIS, System.currentTimeMillis())

        when (state) {
            GateState.Allowed -> editor
                .putBoolean(KEY_REMOTE_CONTROL_BLOCKED, false)
                .remove(KEY_REMOTE_CONTROL_REASON)
                .remove(KEY_REMOTE_CONTROL_MESSAGE)
                .remove(KEY_REMOTE_CONTROL_UPDATE_URL)
                .remove(KEY_REMOTE_CONTROL_LATEST_VERSION_NAME)

            is GateState.Blocked -> editor
                .putBoolean(KEY_REMOTE_CONTROL_BLOCKED, true)
                .putString(KEY_REMOTE_CONTROL_REASON, state.reason.name)
                .putString(KEY_REMOTE_CONTROL_MESSAGE, state.message)
                .putString(KEY_REMOTE_CONTROL_UPDATE_URL, state.updateUrl)
                .putString(KEY_REMOTE_CONTROL_LATEST_VERSION_NAME, state.latestVersionName)
                .putBoolean(KEY_IS_PLAYING, false)
        }.apply()
    }

    private fun localPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
}
