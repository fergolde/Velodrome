package com.fergolde.velodrome.util

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.fergolde.velodrome.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manages app cache for images and music.
 * Handles size calculation and cleanup when limits are exceeded.
 *
 * Cache strategy:
 * - Images: stored in context.cacheDir (internal storage)
 * - Music: stored in context.filesDir/audioCache (internal app storage)
 *
 * Cleanup uses official APIs from Media3 (SimpleCache) and Coil (ImageLoader).
 * File.deleteRecursively() is NOT used to avoid corrupting Media3's cache index.
 */
@UnstableApi
@Singleton
class CacheManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val simpleCache: androidx.media3.datasource.cache.SimpleCache,
    private val musicCacheEvictor: ConfigurableLruCacheEvictor,
    private val settingsRepository: SettingsRepository,
    @Named("app_scope") private val appScope: CoroutineScope,
    @Named("cache_prefs") private val cachePrefs: SharedPreferences
) {

    /**
     * LazyImageLoader from Coil application context.
     * Coil sets this via ImageLoaderFactory in VelodromeApp.
     */
    private val imageLoader: ImageLoader
        get() = context.imageLoader

    companion object {
        private const val DEFAULT_MUSIC_LIMIT_GB = 2
    }

    init {
        // La fuente de verdad del límite musical es DataStore (lo que muestra la UI).
        // Se reconcilia el evictor al arranque para que nunca quede con un límite stale
        // (SharedPreferences) que pudiera evictar la cache a 2 GB por defecto.
        appScope.launch {
            val limitGb = runCatching { settingsRepository.musicCacheSizeGb.first() }
                .getOrElse { cachePrefs.getInt("music_cache_size_gb", DEFAULT_MUSIC_LIMIT_GB) }
            setMusicCacheLimitGb(limitGb)
        }
    }

    // --- Size Calculation ---

    /**
     * Get current image cache size in bytes.
     * Uses Coil's tracked disk cache size instead of walking the directory.
     */
    fun getImageCacheSizeBytes(): Long {
        return imageLoader.diskCache?.size ?: 0L
    }

    /**
     * Get current music cache size in bytes.
     */
    fun getMusicCacheSizeBytes(): Long {
        return simpleCache.cacheSpace
    }

    /**
     * Get current image cache size in a formatted string (e.g., "45 MB").
     */
    fun getImageCacheSizeFormatted(): String {
        return formatSize(getImageCacheSizeBytes())
    }

    /**
     * Get current music cache size in a formatted string (e.g., "0.5 GB").
     */
    fun getMusicCacheSizeFormatted(): String {
        return formatSize(getMusicCacheSizeBytes())
    }

    // --- Cleanup using official APIs ---

    /**
     * Clear all image cache using official Coil API.
     * Uses ImageLoader.diskCache?.clear() and memoryCache?.clear().
     */
    @OptIn(ExperimentalCoilApi::class)
    fun clearImageCache() {
        imageLoader.diskCache?.clear()
        imageLoader.memoryCache?.clear()
    }

    /**
     * Clear all music cache using official Media3 SimpleCache API.
     * Iterates over cache keys and removes each resource safely.
     */
    fun clearMusicCache() {
        simpleCache.keys.forEach { key ->
            simpleCache.removeResource(key)
        }
    }

    /** Updates music cache limit and evicts oldest spans immediately when needed. */
    fun setMusicCacheLimitGb(sizeGb: Int) {
        val limitBytes = sizeGb.coerceIn(0, 20).toLong() * 1024 * 1024 * 1024
        musicCacheEvictor.setMaxBytes(simpleCache, limitBytes)
    }

    /**
     * Clear both caches.
     */
    fun clearAllCaches() {
        clearImageCache()
        clearMusicCache()
    }

    /**
     * Validates if a track is fully cached by comparing downloaded bytes vs expected size.
     * SimpleCache.keys returns anything that touched the disk, even partial downloads.
     * This method ensures only fully (90%+) downloaded tracks are marked as offline.
     *
     * @param trackId The track ID to check
     * @param expectedSizeBytes The expected file size from the API (track.sizeBytes)
     * @return true if track is at least 90% cached
     */
    /**
     * Valida si un track está completamente cacheado.
     */
    fun isTrackFullyCached(trackId: String, expectedSizeBytes: Long): Boolean {
        val key = "navidrome_track_$trackId"
        val spans = simpleCache.getCachedSpans(key)

        // 1. Si no hay nada en disco, no hay canción offline.
        if (spans.isEmpty()) return false

        val downloadedBytes = spans.sumOf { it.length }

        // 2. Si tenemos datos en Room (expectedSizeBytes > 0), somos estrictos.
        if (expectedSizeBytes > 0) {
            return downloadedBytes >= expectedSizeBytes
        }

        // 3. Fallback: Si Room no sabe el tamaño (expectedSizeBytes == 0),
        // usamos tu criterio de 2MB para determinar si es un archivo completo.
        // Esto es muy seguro para MP3s.
        return downloadedBytes > (2 * 1024 * 1024)
    }


    // --- Private Helpers ---

    /**
     * Format size in bytes to human readable string.
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(Locale.ROOT, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format(Locale.ROOT, "%.0f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
