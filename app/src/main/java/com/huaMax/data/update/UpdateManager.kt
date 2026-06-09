package com.huaMax.data.update

import com.huaMax.BuildConfig
import com.huaMax.data.GITHUB_LATEST_RELEASE_API_URL
import com.huaMax.data.GITHUB_LATEST_RELEASE_URL
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object UpdateManager {
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 8_000

    sealed interface UpdateState {
        data class UpToDate(
            val currentVersionName: String,
            val latestVersionName: String,
            val releaseUrl: String
        ) : UpdateState

        data class UpdateAvailable(
            val currentVersionName: String,
            val latestVersionName: String,
            val releaseUrl: String
        ) : UpdateState

        data class Error(
            val currentVersionName: String,
            val releaseUrl: String,
            val message: String
        ) : UpdateState
    }

    private data class LatestRelease(
        val versionCode: Int,
        val versionName: String,
        val releaseUrl: String
    )

    suspend fun checkForUpdate(): UpdateState = withContext(Dispatchers.IO) {
        try {
            val latest = fetchLatestRelease()
            if (latest.versionCode > BuildConfig.VERSION_CODE) {
                UpdateState.UpdateAvailable(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    latestVersionName = latest.versionName,
                    releaseUrl = latest.releaseUrl
                )
            } else {
                UpdateState.UpToDate(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    latestVersionName = latest.versionName,
                    releaseUrl = latest.releaseUrl
                )
            }
        } catch (e: IOException) {
            UpdateState.Error(
                currentVersionName = BuildConfig.VERSION_NAME,
                releaseUrl = GITHUB_LATEST_RELEASE_URL,
                message = e.message.orEmpty()
            )
        } catch (e: RuntimeException) {
            UpdateState.Error(
                currentVersionName = BuildConfig.VERSION_NAME,
                releaseUrl = GITHUB_LATEST_RELEASE_URL,
                message = e.message.orEmpty()
            )
        }
    }

    private fun fetchLatestRelease(): LatestRelease {
        val connection = (URI.create(GITHUB_LATEST_RELEASE_API_URL).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            useCaches = false
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "LocationMax/${BuildConfig.VERSION_NAME}")
        }

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("GitHub HTTP $responseCode")
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseLatestRelease(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseLatestRelease(body: String): LatestRelease {
        val json = JSONObject(body)
        val tag = json.optString("tag_name", "")
        val releaseUrl = json.optString("html_url", GITHUB_LATEST_RELEASE_URL).ifBlank { GITHUB_LATEST_RELEASE_URL }
        val versionName = versionNameFromTag(tag)
        val versionCode = versionCodeFromTag(tag, versionName)

        if (versionName.isBlank() || versionCode <= 0) {
            throw IllegalArgumentException("Invalid release tag: $tag")
        }

        return LatestRelease(
            versionCode = versionCode,
            versionName = versionName,
            releaseUrl = releaseUrl
        )
    }

    private fun versionNameFromTag(tag: String): String {
        val cleanTag = tag.removePrefix("v")
        return if (cleanTag.contains("-")) {
            cleanTag.substringAfter("-")
        } else {
            cleanTag
        }
    }

    private fun versionCodeFromTag(tag: String, versionName: String): Int {
        val cleanTag = tag.removePrefix("v")
        val explicitCode = cleanTag.substringBefore("-").toIntOrNull()
        if (explicitCode != null && cleanTag.contains("-")) return explicitCode

        val core = versionName.substringBefore("-")
        val parts = core.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return major * 10000 + minor * 100 + patch
    }
}
