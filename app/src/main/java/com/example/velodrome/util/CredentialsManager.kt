package com.example.velodrome.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.spght.encryptedprefs.EncryptedSharedPreferences
import dev.spght.encryptedprefs.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: SharedPreferences
) {

    private val KEY_USERNAME = "username"
    private val KEY_PASSWORD = "password"
    private val KEY_SERVER_URL = "server_url"

    // -------------------------
    // SAVE / READ CREDENTIALS
    // -------------------------

    fun saveCredentials(username: String, password: String, serverUrl: String) {
        encryptedPrefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_SERVER_URL, serverUrl)
            .apply()
    }

    fun getUsername(): String? =
        encryptedPrefs.getString(KEY_USERNAME, null)

    fun getPassword(): String? =
        encryptedPrefs.getString(KEY_PASSWORD, null)

    fun getServerUrl(): String? =
        encryptedPrefs.getString(KEY_SERVER_URL, null)

    fun hasCredentials(): Boolean =
        !getUsername().isNullOrBlank() &&
                !getPassword().isNullOrBlank() &&
                !getServerUrl().isNullOrBlank()

    fun clearCredentials() {
        encryptedPrefs.edit().clear().apply()
    }

    // -------------------------
    // AUTH HELPERS
    // -------------------------

    fun generateAuthParams(): Triple<String, String, String>? {
        val username = getUsername() ?: return null
        val password = getPassword() ?: return null

        val salt = NavidromeAuth.generateSalt()
        val token = NavidromeAuth.calculateToken(password, salt)

        return Triple(username, token, salt)
    }

    // -------------------------
    // URL HELPERS
    // -------------------------

    fun getCoverArtUrl(coverArtId: String?, size: Int): String? {
        if (coverArtId.isNullOrBlank()) return null

        val serverUrl = getServerUrl() ?: return null
        val auth = generateAuthParams() ?: return null

        val (username, token, salt) = auth

        return "${serverUrl.trimEnd('/')}/rest/getCoverArt.view" +
                "?id=$coverArtId&size=$size" +
                "&u=$username&t=$token&s=$salt" +
                "&v=1.16.1&c=Velodrome"
    }

    fun getStreamUrl(trackId: String, maxBitRate: Int = 320): String {
        val serverUrl = getServerUrl() ?: return ""

        val auth = generateAuthParams() ?: return ""
        val (username, token, salt) = auth

        return "${serverUrl.trimEnd('/')}/rest/stream.view" +
                "?id=$trackId" +
                "&u=$username&t=$token&s=$salt" +
                "&v=1.16.1&c=Velodrome" +
                "&maxBitRate=$maxBitRate"
    }
}