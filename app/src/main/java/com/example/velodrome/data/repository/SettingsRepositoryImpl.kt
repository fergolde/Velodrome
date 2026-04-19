package com.example.velodrome.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
    }

    // --- Default Values ---

    companion object {
        const val DEFAULT_IMAGE_CACHE_SIZE_MB = 100
        const val DEFAULT_MUSIC_CACHE_SIZE_GB = 1
        const val DEFAULT_ACCENT_COLOR = "#B6A0FF"
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
}