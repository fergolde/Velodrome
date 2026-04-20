package com.example.velodrome.data.datasource

import android.util.Log
import com.example.velodrome.data.datasource.ImageCacheDataSource
import com.example.velodrome.data.datasource.MusicCacheDataSource

/**
 * Singleton holder for cache data sources.
 * Initialized from VelodromeApplication on app start.
 * Provides global access to cache services without DI in composables.
 */
object CacheService {
    private const val TAG = "CacheService"

    var imageCacheDataSource: ImageCacheDataSource? = null
        private set

    var musicCacheDataSource: MusicCacheDataSource? = null
        private set

    /**
     * Initialize cache services from Hilt.
     * Called from VelodromeApplication.onCreate()
     */
    fun initialize(
        imageCache: ImageCacheDataSource,
        musicCache: MusicCacheDataSource
    ) {
        this.imageCacheDataSource = imageCache
        this.musicCacheDataSource = musicCache
        Log.d(TAG, "Cache services initialized")
    }

    /**
     * Check if cache services are initialized.
     */
    fun isInitialized(): Boolean {
        return imageCacheDataSource != null && musicCacheDataSource != null
    }
}