package com.example.velodrome.util

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheSpan
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app cache for images and music.
 * Handles size calculation and cleanup when limits are exceeded.
 *
 * Cache strategy:
 * - Images: stored in context.cacheDir (internal storage)
 * - Music: stored in context.filesDir/musicCache (app-specific external storage)
 *
 * Cleanup uses official APIs from Media3 (SimpleCache) and Coil (ImageLoader).
 * File.deleteRecursively() is NOT used to avoid corrupting Media3's cache index.
 */
@UnstableApi
@Singleton
class CacheManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val simpleCache: androidx.media3.datasource.cache.SimpleCache
) {

    /**
     * LazyImageLoader from Coil application context.
     * Coil sets this via ImageLoaderFactory in VelodromeApp.
     */
    private val imageLoader: ImageLoader
        get() = context.imageLoader

    companion object {
        private const val MUSIC_CACHE_DIR = "audioCache"
        private const val IMAGE_CACHE_DIR = "image_cache"
    }

    // --- Directory Access ---

    /**
     * Directory for image cache (internal storage).
     * Managed by the system and can be cleared at any time.
     */
    val imageCacheDir: File
        get() = File(context.cacheDir, IMAGE_CACHE_DIR).also { it.mkdirs() }

    /**
     * Directory for music cache (app-specific external storage).
     * Persists until explicitly cleared or when storage is low.
     */
    val musicCacheDir: File
        get() = File(context.filesDir, "audioCache").also { it.mkdirs() }

    // --- Size Calculation ---

    /**
     * Get current image cache size in bytes.
     */
    fun getImageCacheSizeBytes(): Long {
        return calculateDirectorySize(imageCacheDir)
    }

    /**
     * Get current music cache size in bytes.
     */
    fun getMusicCacheSizeBytes(): Long {
        return calculateDirectorySize(musicCacheDir)
    }

    /**
     * Get current image cache size in MB.
     */
    fun getImageCacheSizeMb(): Int {
        return (getImageCacheSizeBytes() / (1024 * 1024)).toInt()
    }

    /**
     * Get current music cache size in GB.
     */
    fun getMusicCacheSizeGb(): Int {
        return (getMusicCacheSizeBytes() / (1024 * 1024 * 1024)).toInt()
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

    /**
     * Clear both caches.
     */
    fun clearAllCaches() {
        clearImageCache()
        clearMusicCache()
    }

    /**
     * Returns the set of cached audio keys from Media3 SimpleCache.
     * Used to determine which tracks are available offline.
     */
    fun getCachedKeys(): Set<String> = simpleCache.keys

    /**
     * Validates if a track is fully cached by comparing downloaded bytes vs expected size.
     * SimpleCache.keys returns anything that touched the disk, even partial downloads.
     * This method ensures only fully (90%+) downloaded tracks are marked as offline.
     *
     * @param trackId The track ID to check
     * @param expectedSizeBytes The expected file size from the API (track.sizeBytes)
     * @return true if track is at least 90% cached
     */
    fun isTrackFullyCached(trackId: String, expectedSizeBytes: Long): Boolean {
        val key = "navidrome_track_$trackId"
        val spans = simpleCache.getCachedSpans(key)
        if (spans.isEmpty()) return false

        // Calculate total downloaded bytes for this key
        val downloadedBytes = spans.sumOf { it.length }

        // If sizeBytes is 0 (API didn't provide it), use 1MB as minimum threshold
        val actualExpectedSize = if (expectedSizeBytes == 0L) 1024 * 1024L else expectedSizeBytes

        // Consider fully cached if we have 90% or more of the file
        return downloadedBytes >= (actualExpectedSize * 0.90)
    }

    /**
     * Returns the File objects for all cached audio tracks.
     * Used to determine which tracks are available for offline playback.
     * SimpleCache stores files with hashed names (no extension), so we scan the whole directory.
     */
    fun getCachedAudioFiles(): List<File> {
        val cacheDir = File(context.filesDir, "audioCache")
        if (!cacheDir.exists()) return emptyList()
        return cacheDir.walkTopDown()
            .filter { it.isFile }
            .toList()
    }

    // --- Private Helpers ---

    /**
     * Calculate total size of a directory in bytes.
     */
    private fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0L

        return directory.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    /**
     * Format size in bytes to human readable string.
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.0f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}