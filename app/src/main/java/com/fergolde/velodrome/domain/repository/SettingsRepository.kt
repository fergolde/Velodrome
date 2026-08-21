package com.fergolde.velodrome.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app settings.
 * Defines the contract for managing user preferences and cache configuration.
 */
interface SettingsRepository {

    // --- Cache Settings ---

    /**
     * Image cache size in megabytes (MB).
     * Default: 200 MB
     */
    val imageCacheSizeMb: Flow<Int>

    /**
     * Music cache size in gigabytes (GB).
     * Default: 2 GB
     */
    val musicCacheSizeGb: Flow<Int>

    // --- Appearance Settings ---

    /**
     * Accent color as a hex string (e.g., "#B6A0FF").
     * Default: "#B6A0FF" (AccentPurple)
     */
    val accentColor: Flow<String>

    // --- Scrobble Settings ---

    /**
     * Whether scrobbling to last.fm is enabled.
     * Default: false
     */
    val scrobbleEnabled: Flow<Boolean>

    // --- Sync State ---

    /**
     * Timestamp of last successful sync (millis since epoch).
     * Default: 0L (no sync performed yet)
     */
    val lastSyncTimestamp: Flow<Long>

    /**
     * Offset for resuming interrupted album sync.
     * Default: 0
     */
    val lastSyncOffset: Flow<Int>

    // --- Actions ---

    suspend fun setImageCacheSizeMb(sizeMb: Int)

    suspend fun setMusicCacheSizeGb(sizeGb: Int)

    suspend fun setAccentColor(hexColor: String)

    suspend fun setScrobbleEnabled(enabled: Boolean)

    suspend fun setLastSyncTimestamp(timestamp: Long)

    suspend fun setLastSyncOffset(offset: Int)
}