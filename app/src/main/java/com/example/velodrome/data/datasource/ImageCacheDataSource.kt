package com.example.velodrome.data.datasource

import android.content.Context
import android.util.Log
import com.example.velodrome.domain.repository.SettingsRepository
import com.example.velodrome.util.CacheManager
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
 * DataSource for managing image cache.
 * Implements read-through cache pattern:
 * 1. Check if image exists in local cache
 * 2. If exists, return local file
 * 3. If not, download, save to cache, return local file
 * 4. If cache exceeds limit, clean oldest files (LRU)
 */
@Singleton
class ImageCacheDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val cacheManager: CacheManager,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "ImageCacheDataSource"
        private val scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    }

    /**
     * Get image from cache or download if not exists.
     * Returns the local file path.
     *
     * @param imageUrl The URL of the image to fetch
     * @return Local file path if successful, null otherwise
     */
    suspend fun getImage(imageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile(imageUrl)

            // Step 1: Check if exists in local cache
            if (cacheFile.exists() && cacheFile.length() > 0) {
                Log.d(TAG, "Image found in cache: ${cacheFile.absolutePath}")
                // Update last modified for LRU
                cacheFile.setLastModified(System.currentTimeMillis())
                return@withContext cacheFile.absolutePath
            }

            // Step 2: Download image
            Log.d(TAG, "Downloading image: $imageUrl")
            val success = downloadImage(imageUrl, cacheFile)

            if (success) {
                Log.d(TAG, "Image downloaded and cached: ${cacheFile.absolutePath}")

                // Step 3: Clean cache if exceeds limit (LRU)
                try {
                    val limitMb = settingsRepository.imageCacheSizeMb.first()
                    cacheManager.cleanImageCacheIfNeeded(limitMb)
                    Log.d(TAG, "Cache cleanup done, limit: $limitMb MB")
                } catch (e: Exception) {
                    Log.e(TAG, "Cache cleanup failed: ${e.message}")
                }

                cacheFile.absolutePath
            } else {
                Log.w(TAG, "Failed to download image: $imageUrl")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting image: ${e.message}", e)
            null
        }
    }

    /**
     * Download image from URL and save to cache file.
     */
    private suspend fun downloadImage(imageUrl: String, cacheFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(imageUrl)
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
            Log.e(TAG, "Error downloading image: ${e.message}", e)
            false
        }
    }

    /**
     * Generate cache file path from URL using MD5 hash.
     */
    private fun getCacheFile(imageUrl: String): File {
        val md5 = MessageDigest.getInstance("MD5")
        val hashBytes = md5.digest(imageUrl.toByteArray())
        val hashString = hashBytes.joinToString("") { "%02x".format(it) }
        val extension = getExtension(imageUrl)

        return File(cacheManager.imageCacheDir, "$hashString.$extension")
    }

    /**
     * Extract file extension from URL.
     */
    private fun getExtension(url: String): String {
        val path = url.substringAfterLast("?").substringBeforeLast(".")
        return when {
            path.endsWith("jpg") || path.endsWith("jpeg") -> "jpg"
            path.endsWith("png") -> "png"
            path.endsWith("webp") -> "webp"
            path.endsWith("gif") -> "gif"
            else -> "jpg" // Default to jpg
        }
    }

    /**
     * Clear all image cache.
     */
    fun clearCache() {
        cacheManager.clearImageCache()
        Log.d(TAG, "Image cache cleared")
    }

    /**
     * Get current cache size in bytes.
     */
    fun getCacheSizeBytes(): Long = cacheManager.getImageCacheSizeBytes()

    /**
     * Get formatted cache size string.
     */
    fun getCacheSizeFormatted(): String = cacheManager.getImageCacheSizeFormatted()
}
