package com.example.velodrome.data.datasource

import android.content.Context
import android.util.Log
import com.example.velodrome.domain.repository.SettingsRepository
import com.example.velodrome.util.CacheManager
import com.example.velodrome.util.CredentialsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataSource for managing music cache.
 * Implements read-through cache pattern:
 * 1. Check if track exists in local cache
 * 2. If exists, return local file
 * 3. If not, download, save to cache, return local file
 * 4. If cache exceeds limit, clean oldest files (LRU)
 */
@Singleton
class MusicCacheDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val cacheManager: CacheManager,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "MusicCacheDataSource"
        private val scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    }

    /**
     * Get music file from cache or download if not exists.
     * Returns the local file path.
     *
     * @param trackId The track ID from Navidrome
     * @param artistName Artist name for filename (optional)
     * @param title Track title for filename (optional)
     * @return Local file path if successful, null otherwise
     */
    suspend fun getMusic(
        trackId: String,
        artistName: String? = null,
        title: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile(trackId, artistName, title)

            // Step 1: Check if exists in local cache
            if (cacheFile.exists() && cacheFile.length() > 0) {
                Log.d(TAG, "Music found in cache: ${cacheFile.absolutePath}")
                // Update last modified for LRU
                cacheFile.setLastModified(System.currentTimeMillis())
                return@withContext cacheFile.absolutePath
            }

            // Step 2: Get stream URL and download
            val streamUrl = CredentialsManager.getStreamUrl(trackId, 320) // 320kbps default
            if (streamUrl.isBlank()) {
                Log.w(TAG, "Could not get stream URL for track: $trackId")
                return@withContext null
            }

            Log.d(TAG, "Downloading music: $trackId from $streamUrl")
            val success = downloadMusic(streamUrl, cacheFile)

            if (success) {
                Log.d(TAG, "Music downloaded and cached: ${cacheFile.absolutePath}")

                // Step 3: Clean cache if exceeds limit (LRU)
                try {
                    val limitGb = settingsRepository.musicCacheSizeGb.first()
                    cacheManager.cleanMusicCacheIfNeeded(limitGb)
                    Log.d(TAG, "Cache cleanup done, limit: $limitGb GB")
                } catch (e: Exception) {
                    Log.e(TAG, "Cache cleanup failed: ${e.message}")
                }

                cacheFile.absolutePath
            } else {
                Log.w(TAG, "Failed to download music: $trackId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting music: ${e.message}", e)
            null
        }
    }

    /**
     * Check if track is already cached.
     */
    fun isCached(trackId: String): Boolean {
        val cacheFile = getCacheFile(trackId, null, null)
        return cacheFile.exists() && cacheFile.length() > 0
    }

    /**
     * Download music from URL and save to cache file.
     */
    private suspend fun downloadMusic(streamUrl: String, cacheFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(streamUrl)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Download failed: ${response.code}")
                    return@withContext false
                }

                response.body?.let { body ->
                    // Ensure parent directory exists
                    cacheFile.parentFile?.mkdirs()

                    // Save to file
                    FileOutputStream(cacheFile).use { output ->
                        output.write(body.bytes())
                    }
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading music: ${e.message}", e)
            false
        }
    }

    /**
     * Generate cache file path from track ID using MD5 hash.
     * Format: {artist} - {title}.mp3 or {hash}.mp3
     */
    private fun getCacheFile(trackId: String, artistName: String?, title: String?): File {
        val cacheDir = cacheManager.musicCacheDir

        // Try to create a meaningful filename
        val fileName = if (!artistName.isNullOrBlank() && !title.isNullOrBlank()) {
            // Sanitize filename
            val sanitizedArtist = artistName.replace(Regex("[^a-zA-Z0-9\\s]"), "").take(30)
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\s]"), "").take(30)
            "$sanitizedArtist - $sanitizedTitle.mp3"
        } else {
            // Use MD5 hash as filename
            val md5 = MessageDigest.getInstance("MD5")
            val hashBytes = md5.digest(trackId.toByteArray())
            val hashString = hashBytes.joinToString("") { "%02x".format(it) }
            "$hashString.mp3"
        }

        return File(cacheDir, fileName)
    }

    /**
     * Get cache file for a track ID (without downloading).
     */
    fun getCacheFilePath(trackId: String): String {
        return getCacheFile(trackId, null, null).absolutePath
    }

    /**
     * Clear all music cache.
     */
    fun clearCache() {
        cacheManager.clearMusicCache()
        Log.d(TAG, "Music cache cleared")
    }

    /**
     * Get current cache size in bytes.
     */
    fun getCacheSizeBytes(): Long = cacheManager.getMusicCacheSizeBytes()

    /**
     * Get formatted cache size string.
     */
    fun getCacheSizeFormatted(): String = cacheManager.getMusicCacheSizeFormatted()

    /**
     * Get list of cached track IDs.
     */
    fun getCachedTracks(): List<String> {
        val cacheDir = cacheManager.musicCacheDir
        if (!cacheDir.exists()) return emptyList()

        return cacheDir.listFiles()
            ?.filter { it.isFile && it.extension == "mp3" }
            ?.mapNotNull { file ->
                // Extract track ID from filename or return null
                file.nameWithoutExtension.takeIf { it.length == 32 } // MD5 hash is 32 chars
            } ?: emptyList()
    }

    /**
     * Get all cached music files.
     */
    fun getCachedMusicFiles(): List<File> {
        val cacheDir = cacheManager.musicCacheDir
        if (!cacheDir.exists()) return emptyList()

        return cacheDir.listFiles()
            ?.filter { it.isFile && it.extension == "mp3" }
            ?.toList()
            ?: emptyList()
    }
}