package com.example.velodrome.util

import android.content.Context
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
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
        get() = File(context.filesDir, "audioCache").also { it.mkdirs() } // era "musicCache"

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