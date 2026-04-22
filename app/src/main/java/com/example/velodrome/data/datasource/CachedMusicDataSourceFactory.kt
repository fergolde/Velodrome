package com.example.velodrome.data.datasource

import android.content.Context
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.example.velodrome.util.CacheManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating cached DataSource instances for Media3.
 * Uses SimpleCache for progressive caching during streaming.
 */
@Singleton
class CachedMusicDataSourceFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cacheManager: CacheManager
) {
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getSimpleCache(): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = cacheManager.musicCacheDir
            cacheDir.mkdirs()
            // Use LRU evictor with 100MB cache
            val evictor = LeastRecentlyUsedCacheEvictor(100L * 1024 * 1024)
            simpleCache = SimpleCache(cacheDir, evictor)
        }
        return simpleCache!!
    }

    /**
     * Create a factory for use with ExoPlayer's MediaSource.
     */
    fun createFactory(): CacheDataSource.Factory {
        val cache = getSimpleCache()
        val upstreamFactory = DefaultHttpDataSource.Factory()

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
    }

    /**
     * Release the cache resources.
     */
    fun release() {
        simpleCache?.release()
        simpleCache = null
    }
}