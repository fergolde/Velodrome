package com.fergolde.velodrome.util

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton


private const val STREAMING_BITRATE_ORIGINAL = 999


@Singleton
class CredentialsManager @Inject constructor(
    private val encryptedPrefs: SharedPreferences
) {


    private var cachedToken: String? = null
    private var cachedSalt: String? = null
    private var lastAuthTimestamp: Long = 0L
    private val SESSION_DURATION_MS = 3600_000L // 1 hora

    private val KEY_USERNAME = "username"
    private val KEY_PASSWORD = "password"
    private val KEY_SERVER_URL = "server_url"

    // -------------------------
    // SESSION MANAGEMENT
    // -------------------------

    fun getValidAuthParams(): Triple<String, String, String>? {
        val username = getUsername() ?: return null
        val password = getPassword() ?: return null
        val now = System.currentTimeMillis()

        if (cachedToken != null && cachedSalt != null && (now - lastAuthTimestamp) < SESSION_DURATION_MS) {
            return Triple(username, cachedToken!!, cachedSalt!!)
        }

        val salt = NavidromeAuth.generateSalt()
        val token = NavidromeAuth.calculateToken(password, salt)

        cachedToken = token
        cachedSalt = salt
        lastAuthTimestamp = now

        return Triple(username, token, salt)
    }

    fun invalidateAuth() {
        cachedToken = null
        cachedSalt = null
        lastAuthTimestamp = 0
    }

    // -------------------------
    // PERSISTENCE
    // -------------------------

    fun saveCredentials(username: String, password: String, serverUrl: String) {
        encryptedPrefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_SERVER_URL, serverUrl)
            .apply()
        invalidateAuth() // Importante: al cambiar credenciales, limpiar caché
    }

    fun clearCredentials() {
        encryptedPrefs.edit().clear().apply()
        invalidateAuth() // Importante: al salir, limpiar caché
    }

    // --- (Mantén tus getters de strings igual) ---
    fun getUsername(): String? = encryptedPrefs.getString(KEY_USERNAME, null)
    fun getPassword(): String? = encryptedPrefs.getString(KEY_PASSWORD, null)
    fun getServerUrl(): String? = encryptedPrefs.getString(KEY_SERVER_URL, null)
    fun hasCredentials(): Boolean = !getUsername().isNullOrBlank() && !getPassword().isNullOrBlank() && !getServerUrl().isNullOrBlank()

    // -------------------------
    // URL HELPERS (Refactorizadas para usar la caché)
    // -------------------------

    fun getCoverArtUrl(coverArtId: String?, size: Int): String? {
        if (coverArtId.isNullOrBlank()) return null
        val serverUrl = getServerUrl() ?: return null

        // AHORA usamos getValidAuthParams en lugar de generateAuthParams
        val auth = getValidAuthParams() ?: return null
        val (username, token, salt) = auth

        return "${serverUrl.trimEnd('/')}/rest/getCoverArt.view" +
                "?id=$coverArtId&size=$size" +
                "&u=$username&t=$token&s=$salt" +
                "&v=1.16.1&c=Velodrome"
    }

    fun getStreamUrl(trackId: String): String { // Eliminamos maxBitRate del argumento
        val serverUrl = getServerUrl() ?: return ""
        val auth = getValidAuthParams() ?: return ""
        val (username, token, salt) = auth

        return "${serverUrl.trimEnd('/')}/rest/stream.view" +
                "?id=$trackId" +
                "&u=$username&t=$token&s=$salt" +
                "&v=1.16.1&c=Velodrome" +
                "&maxBitRate=$STREAMING_BITRATE_ORIGINAL" // Fuerza calidad original
    }
}