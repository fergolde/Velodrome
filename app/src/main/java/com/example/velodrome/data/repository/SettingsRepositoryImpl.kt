package com.example.velodrome.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.velodrome.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property to create DataStore
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Implementation of SettingsRepository using DataStore Preferences.
 * Persists user settings locally on the device.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    // --- Preference Keys ---

    private object PreferencesKeys {
        val IMAGE_CACHE_SIZE_MB = intPreferencesKey("image_cache_size_mb")
        val MUSIC_CACHE_SIZE_GB = intPreferencesKey("music_cache_size_gb")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val SCROBBLE_ENABLED = booleanPreferencesKey("scrobble_enabled")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val LAST_SYNC_OFFSET = intPreferencesKey("last_sync_offset")
    }

    // --- Default Values ---

    companion object {
        const val DEFAULT_IMAGE_CACHE_SIZE_MB = 200
        const val DEFAULT_MUSIC_CACHE_SIZE_GB = 2
        const val DEFAULT_ACCENT_COLOR = "#B6A0FF"
        const val DEFAULT_SCROBBLE_ENABLED = false
        const val DEFAULT_LAST_SYNC_TIMESTAMP = 0L
        const val DEFAULT_LAST_SYNC_OFFSET = 0
    }

    // --- Cache Settings ---

    override val imageCacheSizeMb: Flow<Int> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IMAGE_CACHE_SIZE_MB] ?: DEFAULT_IMAGE_CACHE_SIZE_MB
        }

    override val musicCacheSizeGb: Flow<Int> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.MUSIC_CACHE_SIZE_GB] ?: DEFAULT_MUSIC_CACHE_SIZE_GB
        }

    // --- Appearance Settings ---

    override val accentColor: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] ?: DEFAULT_ACCENT_COLOR
        }

    // --- Scrobble Settings ---

    override val scrobbleEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SCROBBLE_ENABLED] ?: DEFAULT_SCROBBLE_ENABLED
        }

    // --- Sync State ---

    override val lastSyncTimestamp: Flow<Long> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: DEFAULT_LAST_SYNC_TIMESTAMP
        }

    override val lastSyncOffset: Flow<Int> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_OFFSET] ?: DEFAULT_LAST_SYNC_OFFSET
        }

    // --- Actions ---

    override suspend fun setImageCacheSizeMb(sizeMb: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.IMAGE_CACHE_SIZE_MB] = sizeMb.coerceIn(0, 500) // Max 500 MB
        }
    }

    override suspend fun setMusicCacheSizeGb(sizeGb: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.MUSIC_CACHE_SIZE_GB] = sizeGb.coerceIn(0, 20) // Max 20 GB
        }
    }

    override suspend fun setAccentColor(hexColor: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] = hexColor
        }
    }

    override suspend fun setScrobbleEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SCROBBLE_ENABLED] = enabled
        }
    }

    override suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    override suspend fun setLastSyncOffset(offset: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_OFFSET] = offset
        }
    }
}