package com.example.velodrome.util

import android.content.Context
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
 * Cleanup is LRU (Least Recently Used) - oldest files are deleted first.
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val MUSIC_CACHE_DIR = "musicCache"
        private const val IMAGE_CACHE_DIR = "imageCache"
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
        get() = File(context.filesDir, MUSIC_CACHE_DIR).also { it.mkdirs() }

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

    // --- Cleanup ---

    /**
     * Clean image cache if it exceeds the limit.
     * Deletes oldest files first (LRU strategy).
     */
    fun cleanImageCacheIfNeeded(limitMb: Int) {
        val limitBytes = limitMb.toLong() * 1024 * 1024
        trimCacheIfNeeded(imageCacheDir, limitBytes)
    }

    /**
     * Clean music cache if it exceeds the limit.
     * Deletes oldest files first (LRU strategy).
     */
    fun cleanMusicCacheIfNeeded(limitGb: Int) {
        val limitBytes = limitGb.toLong() * 1024 * 1024 * 1024
        trimCacheIfNeeded(musicCacheDir, limitBytes)
    }

    /**
     * Clean both caches if they exceed their limits.
     */
    fun cleanCachesIfNeeded(imageLimitMb: Int, musicLimitGb: Int) {
        cleanImageCacheIfNeeded(imageLimitMb)
        cleanMusicCacheIfNeeded(musicLimitGb)
    }

    /**
     * Clear all image cache immediately.
     */
    fun clearImageCache() {
        deleteRecursively(imageCacheDir)
        imageCacheDir.mkdirs()
    }

    /**
     * Clear all music cache immediately.
     */
    fun clearMusicCache() {
        deleteRecursively(musicCacheDir)
        musicCacheDir.mkdirs()
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

    /**
     * Trim cache directory to be under limit bytes by deleting oldest files first.
     * Uses lastModified() as proxy for "recently used".
     */
    private fun trimCacheIfNeeded(directory: File, limitBytes: Long) {
        if (!directory.exists()) return

        val currentSize = calculateDirectorySize(directory)
        if (currentSize <= limitBytes) return

        // Get all files sorted by last modified (oldest first)
        val files = directory.listFiles()
            ?.filter { it.isFile }
            ?.sortedBy { it.lastModified() }
            ?: return

        var bytesToDelete = currentSize - limitBytes
        for (file in files) {
            if (bytesToDelete <= 0) break
            val fileSize = file.length()
            if (file.delete()) {
                bytesToDelete -= fileSize
            }
        }
    }

    /**
     * Recursively delete a directory and all its contents.
     */
    private fun deleteRecursively(directory: File) {
        if (!directory.exists()) return
        
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteRecursively(file)
            }
            file.delete()
        }
    }
}