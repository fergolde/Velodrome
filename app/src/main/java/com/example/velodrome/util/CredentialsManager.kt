package com.example.velodrome.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.spght.encryptedprefs.EncryptedSharedPreferences
import dev.spght.encryptedprefs.MasterKey
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicReference

/**
 * Secure credential storage using EncryptedSharedPreferences.
 * Uses companion object pattern to allow both static access (CredentialsManager.x())
 * and injectable access (@Inject private val credentialsManager: CredentialsManager).
 */
@Singleton
class CredentialsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val instanceRef = AtomicReference<CredentialsManager?>(null)
        
        fun hasCredentials(): Boolean = instanceRef.get()?.hasCredentials() ?: false
        fun getUsername(): String? = instanceRef.get()?.getUsername()
        fun getPassword(): String? = instanceRef.get()?.getPassword()
        fun getServerUrl(): String? = instanceRef.get()?.getServerUrl()
        fun saveCredentials(username: String, password: String, serverUrl: String) = 
            instanceRef.get()?.saveCredentials(username, password, serverUrl)
        fun clearCredentials() = instanceRef.get()?.clearCredentials()
        fun generateAuthParams(): Triple<String, String, String>? = instanceRef.get()?.generateAuthParams()
        fun getCoverArtUrl(coverArtId: String?, size: Int): String? = instanceRef.get()?.getCoverArtUrl(coverArtId, size)
        fun getStreamUrl(trackId: String, maxBitRate: Int): String = instanceRef.get()?.getStreamUrl(trackId, maxBitRate) ?: ""
    }
    
    init {
        instanceRef.set(this)
    }

    private val PREFS_NAME = "velodrome_secure_prefs"
    private val KEY_USERNAME = "username"
    private val KEY_PASSWORD = "password"
    private val KEY_SERVER_URL = "server_url"

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(username: String, password: String, serverUrl: String) {
        encryptedPrefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_SERVER_URL, serverUrl)
            .apply()
    }

    fun getUsername(): String? = encryptedPrefs.getString(KEY_USERNAME, null)
    fun getPassword(): String? = encryptedPrefs.getString(KEY_PASSWORD, null)
    fun getServerUrl(): String? = encryptedPrefs.getString(KEY_SERVER_URL, null)
    fun hasCredentials(): Boolean = getUsername() != null && getPassword() != null && getServerUrl() != null
    fun clearCredentials() = encryptedPrefs.edit().clear().apply()

    fun generateAuthParams(): Triple<String, String, String>? {
        val username = getUsername() ?: return null
        val password = getPassword() ?: return null
        val salt = NavidromeAuth.generateSalt()
        val token = NavidromeAuth.calculateToken(password, salt)
        return Triple(username, token, salt)
    }

    fun getCoverArtUrl(coverArtId: String?, size: Int): String? {
        if (coverArtId.isNullOrBlank()) return null
        val serverUrl = getServerUrl() ?: return null
        val authParams = generateAuthParams() ?: return null
        val (username, token, salt) = authParams
        return "${serverUrl.trimEnd('/')}/rest/getCoverArt.view?id=$coverArtId&size=$size&u=$username&t=$token&s=$salt&v=1.16.1&c=Velodrome"
    }

    fun getStreamUrl(trackId: String, maxBitRate: Int = 320): String {
        val serverUrl = getServerUrl() ?: return ""
        val authParams = generateAuthParams() ?: return ""
        val (username, token, salt) = authParams
        return "${serverUrl.trimEnd('/')}/rest/stream.view?id=$trackId&u=$username&t=$token&s=$salt&v=1.16.1&c=Velodrome&maxBitRate=$maxBitRate"
    }
}