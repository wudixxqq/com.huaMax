package com.huaMax.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.gson.JsonParser
import com.huaMax.data.AUTH_VALIDITY_DAYS
import com.huaMax.data.KEY_AUTH_CODE
import com.huaMax.data.KEY_AUTH_EXPIRES_AT_MILLIS
import com.huaMax.data.KEY_AUTH_LAST_SEEN_MILLIS
import com.huaMax.data.KEY_IS_PLAYING
import com.huaMax.data.SHARED_PREFS_FILE
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import kotlin.math.abs

object AuthorizationManager {
    private const val CODE_PREFIX = "LM1"
    private const val SUBJECT = "LocationMax"
    private const val PUBLIC_KEY_BASE64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArgnfooA1VoXqhqWbFcATi6hAaBkBvFNY5+EwUJYUJDwoA6XYqPjrsv8JC6h/No3AxOoSDCBRMoSKAc1YAlTyRArQ0sTv9dkIJaZhHsTjeANvehqmAoi58Revge+VRpf/+snzNblJQhqO4pMx+6bosDW5xX9M8V8aiXw/XuE0lJ45ms8gq4VFgikOg6qKoMFgiSBceYvPIrhg4MuhfBJGJjo2d1v9idTmxuJq+MiqxP9WFM+8lV0HEt7mGwpWrB9CnfDleL7cJgbfQxSxYdE/szWdFUuavm11GupaLkZ+w1HDISxX4D+Q1WA1pNLVrAoDDbOKEN6dUR2pV01oSXsG3QIDAQAB"
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    private const val CLOCK_ROLLBACK_GRACE_MILLIS = 10L * 60L * 1000L
    private const val LAST_SEEN_UPDATE_INTERVAL_MILLIS = 60L * 60L * 1000L
    private const val MAX_VALIDITY_SKEW_MILLIS = 5L * 60L * 1000L
    private const val MAX_CODE_AGE_MILLIS = AUTH_VALIDITY_DAYS * MILLIS_PER_DAY + MAX_VALIDITY_SKEW_MILLIS

    sealed interface ValidationResult {
        data class Valid(
            val code: String,
            val issuedAtMillis: Long,
            val expiresAtMillis: Long
        ) : ValidationResult

        data class Invalid(val reason: Reason) : ValidationResult
    }

    enum class Reason {
        EMPTY,
        FORMAT,
        SIGNATURE,
        SUBJECT,
        EXPIRED,
        TOO_LONG,
        CLOCK_ROLLBACK,
        ERROR
    }

    fun getStatus(
        prefs: SharedPreferences,
        nowMillis: Long = System.currentTimeMillis(),
        updateLastSeen: Boolean = true
    ): ValidationResult {
        val code = prefs.getString(KEY_AUTH_CODE, null).orEmpty()
        val result = validateCode(code, nowMillis)
        if (result is ValidationResult.Valid) {
            val lastSeen = prefs.getLong(KEY_AUTH_LAST_SEEN_MILLIS, 0L)
            if (lastSeen > 0L && nowMillis + CLOCK_ROLLBACK_GRACE_MILLIS < lastSeen) {
                return ValidationResult.Invalid(Reason.CLOCK_ROLLBACK)
            }
            if (updateLastSeen && nowMillis - lastSeen >= LAST_SEEN_UPDATE_INTERVAL_MILLIS) {
                prefs.edit()
                    .putLong(KEY_AUTH_EXPIRES_AT_MILLIS, result.expiresAtMillis)
                    .putLong(KEY_AUTH_LAST_SEEN_MILLIS, nowMillis)
                    .apply()
            }
        }
        return result
    }

    fun validateCode(
        rawCode: String,
        nowMillis: Long = System.currentTimeMillis()
    ): ValidationResult {
        val normalizedCode = rawCode.trim().replace("\n", "").replace("\r", "")
        if (normalizedCode.isBlank()) return ValidationResult.Invalid(Reason.EMPTY)

        return runCatching {
            val parts = normalizedCode.split(".")
            if (parts.size != 3 || parts[0] != CODE_PREFIX) {
                return ValidationResult.Invalid(Reason.FORMAT)
            }

            val payloadPart = parts[1]
            val signature = decodeUrlPart(parts[2])
            val signedBytes = payloadPart.toByteArray(StandardCharsets.US_ASCII)
            if (!verifySignature(signedBytes, signature)) {
                return ValidationResult.Invalid(Reason.SIGNATURE)
            }

            val payloadJson = String(decodeUrlPart(payloadPart), StandardCharsets.UTF_8)
            val payload = JsonParser.parseString(payloadJson).asJsonObject
            if (payload.get("sub")?.asString != SUBJECT) {
                return ValidationResult.Invalid(Reason.SUBJECT)
            }

            val issuedAtMillis = payload.get("iat")?.asLong?.times(1000L)
                ?: return ValidationResult.Invalid(Reason.FORMAT)
            val expiresAtMillis = payload.get("exp")?.asLong?.times(1000L)
                ?: return ValidationResult.Invalid(Reason.FORMAT)

            if (expiresAtMillis - issuedAtMillis > MAX_CODE_AGE_MILLIS) {
                return ValidationResult.Invalid(Reason.TOO_LONG)
            }
            if (nowMillis > expiresAtMillis) {
                return ValidationResult.Invalid(Reason.EXPIRED)
            }
            if (abs(nowMillis - issuedAtMillis) > MAX_CODE_AGE_MILLIS && nowMillis < issuedAtMillis) {
                return ValidationResult.Invalid(Reason.CLOCK_ROLLBACK)
            }

            ValidationResult.Valid(
                code = normalizedCode,
                issuedAtMillis = issuedAtMillis,
                expiresAtMillis = expiresAtMillis
            )
        }.getOrElse {
            ValidationResult.Invalid(Reason.ERROR)
        }
    }

    fun saveValidCode(
        prefs: SharedPreferences,
        valid: ValidationResult.Valid,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putString(KEY_AUTH_CODE, valid.code)
            .putLong(KEY_AUTH_EXPIRES_AT_MILLIS, valid.expiresAtMillis)
            .putLong(KEY_AUTH_LAST_SEEN_MILLIS, nowMillis)
            .apply()
    }

    fun clearAuthorization(prefs: SharedPreferences) {
        prefs.edit()
            .remove(KEY_AUTH_CODE)
            .remove(KEY_AUTH_EXPIRES_AT_MILLIS)
            .remove(KEY_AUTH_LAST_SEEN_MILLIS)
            .putBoolean(KEY_IS_PLAYING, false)
            .apply()
    }

    fun syncLocalAuthorizationToRemote(context: Context, remotePrefs: SharedPreferences) {
        val localPrefs = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
        when (val status = getStatus(localPrefs, updateLastSeen = true)) {
            is ValidationResult.Valid -> saveValidCode(remotePrefs, status)
            is ValidationResult.Invalid -> clearAuthorization(remotePrefs)
        }
    }

    fun reasonMessage(reason: Reason): String = when (reason) {
        Reason.EMPTY -> "授权码不能为空"
        Reason.FORMAT -> "授权码格式不正确"
        Reason.SIGNATURE -> "授权码签名无效"
        Reason.SUBJECT -> "授权码不属于 LocationMax"
        Reason.EXPIRED -> "授权码已过期，请重新输入新的授权码"
        Reason.TOO_LONG -> "授权码有效期超过 $AUTH_VALIDITY_DAYS 天，已拒绝"
        Reason.CLOCK_ROLLBACK -> "检测到系统时间异常，请校准时间后重新授权"
        Reason.ERROR -> "授权码校验失败"
    }

    private fun decodeUrlPart(value: String): ByteArray =
        Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun verifySignature(data: ByteArray, signatureBytes: ByteArray): Boolean {
        val publicKeyBytes = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT)
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(publicKeyBytes))
        return Signature.getInstance("SHA256withRSA").run {
            initVerify(publicKey)
            update(data)
            verify(signatureBytes)
        }
    }
}
