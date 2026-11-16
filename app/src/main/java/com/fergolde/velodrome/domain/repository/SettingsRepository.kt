package com.fergolde.velodrome.domain.repository

import kotlinx.coroutines.flow.Flow

/** Accent color used until the user picks a different one. */
const val DEFAULT_ACCENT_COLOR = "#B6A0FF"

/**
 * Clave del orden de álbumes del detalle de artista por defecto: año
 * ascendente, que es el orden que el loader ya aplicaba antes de S1.
 */
const val DEFAULT_ARTIST_ALBUM_SORT_ORDER = "year_asc"

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

    // --- Artist Detail Settings ---

    /**
     * Orden elegido para los álbumes del detalle de artista, guardado como
     * clave estable (`year_asc`, `year_desc`, `title_asc`, `most_played`,
     * `random`).
     * Default: [DEFAULT_ARTIST_ALBUM_SORT_ORDER] (año ascendente)
     */
    val artistAlbumSortOrder: Flow<String>

    // --- Scrobble Settings ---

    /**
     * Whether scrobbling to last.fm is enabled.
     * Default: false
     */
    val scrobbleEnabled: Flow<Boolean>

    /**
     * Automatic genre-based equalizer on the audio session.
     * Default: false
     */
    val eqEnabled: Flow<Boolean>

    /**
     * Bass boost effect on the audio session.
     * Default: false
     */
    val bassBoostEnabled: Flow<Boolean>

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

    /**
     * Timestamp of the last lightweight "has server changed?" probe (millis).
     * Used to throttle that check to once per window instead of every app open.
     * Default: 0L (never checked)
     */
    val lastServerCheckAt: Flow<Long>

    /**
     * Last Navidrome version reported by the server (`serverVersion` attribute).
     * Default: null (never recorded)
     */
    val lastServerVersion: Flow<String?>

    // --- Actions ---

    suspend fun setImageCacheSizeMb(sizeMb: Int)

    suspend fun setMusicCacheSizeGb(sizeGb: Int)

    suspend fun setAccentColor(hexColor: String)

    /** Guarda el orden elegido para los álbumes del detalle de artista. */
    suspend fun setArtistAlbumSortOrder(orderKey: String)

    suspend fun setScrobbleEnabled(enabled: Boolean)

    suspend fun setEqEnabled(enabled: Boolean)

    suspend fun setBassBoostEnabled(enabled: Boolean)

    suspend fun setLastSyncTimestamp(timestamp: Long)

    suspend fun setLastSyncOffset(offset: Int)

    suspend fun setLastServerCheckAt(timestamp: Long)

    /** Passing null clears the stored version, e.g. when the account is discarded on logout. */
    suspend fun setLastServerVersion(version: String?)
}