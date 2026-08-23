package com.fergolde.velodrome.util

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit


private const val STREAMING_BITRATE_ORIGINAL = 999


@Singleton
class CredentialsManager @Inject constructor(
    private val encryptedPrefs: SharedPreferences
) {


    private var cachedToken: String? = null
    private var cachedSalt: String? = null
    private var lastAuthTimestamp: Long = 0L
    private val SESSION_DURATION_MS = 3600_000L // 1 hora

    // Memoized credential fields: EncryptedSharedPreferences decrypts (AES) on
    // every getString, so a 200-cover grid would pay ~600 AES ops per screen.
    // Loaded once per invalidation instead.
    private var cachedUsername: String? = null
    private var cachedPassword: String? = null
    private var cachedServerUrl: String? = null
    private var credentialsLoaded = false

    private val KEY_USERNAME = "username"
    private val KEY_PASSWORD = "password"
    private val KEY_SERVER_URL = "server_url"

    // -------------------------
    // SESSION MANAGEMENT
    // -------------------------

    @Synchronized
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
        // Force re-read of credentials from prefs on next access. This is the
        // single reset point; saveCredentials/clearCredentials both call it.
        credentialsLoaded = false
    }

    // -------------------------
    // PERSISTENCE
    // -------------------------

    fun saveCredentials(username: String, password: String, serverUrl: String) {
        encryptedPrefs.edit {
            putString(KEY_USERNAME, username)
                .putString(KEY_PASSWORD, password)
                .putString(KEY_SERVER_URL, serverUrl)
        }
        invalidateAuth() // Importante: al cambiar credenciales, limpiar caché
    }

    fun clearCredentials() {
        encryptedPrefs.edit { clear() }
        invalidateAuth() // Importante: al salir, limpiar caché
    }

    private fun ensureCredentialsLoaded() {
        if (credentialsLoaded) return
        synchronized(this) {
            if (credentialsLoaded) return
            cachedUsername = encryptedPrefs.getString(KEY_USERNAME, null)
            cachedPassword = encryptedPrefs.getString(KEY_PASSWORD, null)
            cachedServerUrl = encryptedPrefs.getString(KEY_SERVER_URL, null)
            credentialsLoaded = true
        }
    }

    // --- Getters servidos desde caché (re-lee solo tras invalidateAuth) ---
    fun getUsername(): String? {
        ensureCredentialsLoaded()
        return cachedUsername
    }

    fun getPassword(): String? {
        ensureCredentialsLoaded()
        return cachedPassword
    }

    fun getServerUrl(): String? {
        ensureCredentialsLoaded()
        return cachedServerUrl
    }

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
