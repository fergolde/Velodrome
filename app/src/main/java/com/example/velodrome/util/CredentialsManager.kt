package com.example.velodrome.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dev.spght.encryptedprefs.EncryptedSharedPreferences
import dev.spght.encryptedprefs.MasterKey
import com.example.velodrome.util.NavidromeAuth.calculateToken
import com.example.velodrome.util.NavidromeAuth.generateSalt

/**
 * Secure credential storage using EncryptedSharedPreferences (fork post-deprecation)
 * Stores username and password securely. NO token storage (per requirements).
 */
object CredentialsManager {

    private const val PREFS_NAME = "velodrome_secure_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_SERVER_URL = "server_url"

    private var encryptedPrefs: SharedPreferences? = null

    /**
     * Initialize EncryptedSharedPreferences
     * Must be called from Application context
     */
    fun init(context: Context) {
        if (encryptedPrefs == null) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            Log.d("CredentialsManager", "EncryptedSharedPreferences initialized (fork)")
        }
    }

    /**
     * Save credentials securely
     * NO token storage - only username and password
     */
    fun saveCredentials(username: String, password: String, serverUrl: String) {
        encryptedPrefs?.let { prefs ->
            prefs.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_PASSWORD, password)
                .putString(KEY_SERVER_URL, serverUrl)
                .apply()
            Log.d("CredentialsManager", "Credentials saved for user: $username")
        } ?: Log.e("CredentialsManager", "EncryptedSharedPreferences not initialized")
    }

    /**
     * Get stored username
     */
    fun getUsername(): String? {
        return encryptedPrefs?.getString(KEY_USERNAME, null)
    }

    /**
     * Get stored password
     */
    fun getPassword(): String? {
        return encryptedPrefs?.getString(KEY_PASSWORD, null)
    }

    /**
     * Get stored server URL
     */
    fun getServerUrl(): String? {
        return encryptedPrefs?.getString(KEY_SERVER_URL, null)
    }

    /**
     * Check if credentials exist
     */
    fun hasCredentials(): Boolean {
        return getUsername() != null && getPassword() != null && getServerUrl() != null
    }

    /**
     * Clear all credentials (for logout or auth failure)
     */
    fun clearCredentials() {
        encryptedPrefs?.edit()?.clear()?.apply()
        Log.d("CredentialsManager", "Credentials cleared")
    }

    /**
     * Generate auth params for a request
     * Generates NEW salt and token for each request (per requirements)
     * @return Triple of (username, token, salt)
     */
    fun generateAuthParams(): Triple<String, String, String>? {
        val username = getUsername() ?: return null
        val password = getPassword() ?: return null
        val salt = generateSalt()
        val token = calculateToken(password, salt)
        return Triple(username, token, salt)
    }

    /**
     * Build cover art URL from coverArt ID
     * @param coverArtId The coverArt ID from API (e.g., "al-123")
     * @param size Size in pixels (default 300)
     */
    fun getCoverArtUrl(coverArtId: String?, size: Int = 300): String? {
        Log.d("CredMgr", "getCoverArtUrl called: coverArtId='$coverArtId', size=$size")
        
        if (coverArtId.isNullOrBlank()) {
            Log.w("CredMgr", "coverArtId is null or blank")
            return null
        }
        
        val serverUrl = getServerUrl()
        Log.d("CredMgr", "serverUrl='$serverUrl'")
        
        if (serverUrl == null) {
            Log.w("CredMgr", "No server URL found")
            return null
        }
        
        val authParams = generateAuthParams()
        if (authParams == null) {
            Log.e("CredMgr", "No auth params (not logged in)")
            return null
        }
        
        val (username, token, salt) = authParams
        
        // Ensure serverUrl ends with "/" before appending "rest/"
        val normalizedUrl = serverUrl.trimEnd('/') + "/"
        val url = "${normalizedUrl}rest/getCoverArt.view?id=$coverArtId&size=$size&u=$username&t=$token&s=$salt&v=1.16.1&c=Velodrome"
        Log.d("CredMgr", "Generated cover URL: ${url.take(100)}...")
        return url
    }

    /**
     * Build stream URL for a track from Navidrome server
     * @param trackId The track ID from API (e.g., "12345")
     * @param maxBitRate Maximum bitrate for streaming (default 320)
     */
    fun getStreamUrl(trackId: String, maxBitRate: Int = 320): String {
        Log.d("CredMgr", "getStreamUrl called: trackId='$trackId', maxBitRate=$maxBitRate")

        val serverUrl = getServerUrl()
        Log.d("CredMgr", "serverUrl='$serverUrl'")

        if (serverUrl == null) {
            Log.w("CredMgr", "No server URL found")
            return ""
        }

        val authParams = generateAuthParams()
        if (authParams == null) {
            Log.e("CredMgr", "No auth params (not logged in)")
            return ""
        }

        val (username, token, salt) = authParams

        // Ensure serverUrl ends with "/" before appending "rest/"
        val normalizedUrl = serverUrl.trimEnd('/') + "/"
        val url = "${normalizedUrl}rest/stream.view?id=$trackId&u=$username&t=$token&s=$salt&v=1.16.1&c=Velodrome&maxBitRate=$maxBitRate"
        Log.d("CredMgr", "Generated stream URL: ${url.take(100)}...")
        return url
    }
}