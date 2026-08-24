package com.fergolde.velodrome.data.repository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.fergolde.velodrome.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cachePrefs: SharedPreferences
) : SettingsRepository {

    // --- Preference Keys ---
    private object PreferencesKeys {
        val IMAGE_CACHE_SIZE_MB = intPreferencesKey("image_cache_size_mb")
        val MUSIC_CACHE_SIZE_GB = intPreferencesKey("music_cache_size_gb")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val SCROBBLE_ENABLED = booleanPreferencesKey("scrobble_enabled")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val BASS_BOOST_ENABLED = booleanPreferencesKey("bass_boost_enabled")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val LAST_SYNC_OFFSET = intPreferencesKey("last_sync_offset")
        val LAST_SERVER_CHECK_AT = longPreferencesKey("last_server_check_at")
    }

    companion object {
        const val DEFAULT_IMAGE_CACHE_SIZE_MB = 200
        const val DEFAULT_MUSIC_CACHE_SIZE_GB = 2
        const val DEFAULT_ACCENT_COLOR = "#B6A0FF"
        const val DEFAULT_SCROBBLE_ENABLED = false
        const val DEFAULT_EQ_ENABLED = false
        const val DEFAULT_BASS_BOOST_ENABLED = false
        const val DEFAULT_LAST_SYNC_TIMESTAMP = 0L
        const val DEFAULT_LAST_SYNC_OFFSET = 0
    }

    // --- Cache Settings ---

    override val imageCacheSizeMb: Flow<Int> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.IMAGE_CACHE_SIZE_MB] ?: DEFAULT_IMAGE_CACHE_SIZE_MB }

    override val musicCacheSizeGb: Flow<Int> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.MUSIC_CACHE_SIZE_GB] ?: DEFAULT_MUSIC_CACHE_SIZE_GB }

    override val accentColor: Flow<String> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.ACCENT_COLOR] ?: DEFAULT_ACCENT_COLOR }

    override val scrobbleEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SCROBBLE_ENABLED] ?: DEFAULT_SCROBBLE_ENABLED }

    override val eqEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.EQ_ENABLED] ?: DEFAULT_EQ_ENABLED }

    override val bassBoostEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.BASS_BOOST_ENABLED] ?: DEFAULT_BASS_BOOST_ENABLED }

    override val lastSyncTimestamp: Flow<Long> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: DEFAULT_LAST_SYNC_TIMESTAMP }

    override val lastSyncOffset: Flow<Int> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LAST_SYNC_OFFSET] ?: DEFAULT_LAST_SYNC_OFFSET }

    override val lastServerCheckAt: Flow<Long> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LAST_SERVER_CHECK_AT] ?: DEFAULT_LAST_SYNC_TIMESTAMP }

    // --- Actions ---

    override suspend fun setImageCacheSizeMb(sizeMb: Int) {
        val safeSize = sizeMb.coerceIn(0, 1000)
        dataStore.edit { it[PreferencesKeys.IMAGE_CACHE_SIZE_MB] = safeSize }

        // Sincronizar con SharedPreferences para el arranque de Coil
        cachePrefs.edit(commit = true) { putInt("image_cache_size_mb", safeSize) }
    }

    override suspend fun setMusicCacheSizeGb(sizeGb: Int) {
        val safeSize = sizeGb.coerceIn(0, 20)
        // 1. Persistimos en DataStore
        dataStore.edit { it[PreferencesKeys.MUSIC_CACHE_SIZE_GB] = safeSize }

        // 2. Persistimos en SharedPreferences para AudioModule
        cachePrefs.edit(commit = true) { putInt("music_cache_size_gb", safeSize) }
    }

    override suspend fun setAccentColor(hexColor: String) {
        dataStore.edit { it[PreferencesKeys.ACCENT_COLOR] = hexColor }
    }

    override suspend fun setScrobbleEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SCROBBLE_ENABLED] = enabled }
    }

    override suspend fun setEqEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.EQ_ENABLED] = enabled }
    }

    override suspend fun setBassBoostEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.BASS_BOOST_ENABLED] = enabled }
    }

    override suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { it[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp }
    }

    override suspend fun setLastSyncOffset(offset: Int) {
        dataStore.edit { it[PreferencesKeys.LAST_SYNC_OFFSET] = offset }
    }

    override suspend fun setLastServerCheckAt(timestamp: Long) {
        dataStore.edit { it[PreferencesKeys.LAST_SERVER_CHECK_AT] = timestamp }
    }
}