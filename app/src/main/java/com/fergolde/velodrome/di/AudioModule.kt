package com.fergolde.velodrome.di

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.fergolde.velodrome.presentation.audio.AudioPlayerManager
import com.fergolde.velodrome.presentation.audio.ScrobbleManager
import com.fergolde.velodrome.util.ConfigurableLruCacheEvictor
import com.fergolde.velodrome.util.CredentialsManager
import com.fergolde.velodrome.util.NavidromeCacheKeyFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideMusicCacheEvictor(
        @Named("cache_prefs") sharedPreferences: SharedPreferences
    ): ConfigurableLruCacheEvictor {
        val limitGb = sharedPreferences.getInt("music_cache_size_gb", 2)
        return ConfigurableLruCacheEvictor(limitGb.toLong() * 1024 * 1024 * 1024)
    }

    @Provides
    @Singleton
    fun provideSimpleCache(
        @ApplicationContext context: Context,
        cacheEvictor: ConfigurableLruCacheEvictor
    ): SimpleCache {
        val cacheDir = File(context.filesDir, "audioCache").also { it.mkdirs() }
        val databaseProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }

    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        simpleCache: SimpleCache,
        okHttpClient: OkHttpClient
    ): CacheDataSource.Factory {
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setCacheKeyFactory(NavidromeCacheKeyFactory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .setEventListener(object : CacheDataSource.EventListener {
                override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {
                    android.util.Log.d(
                        "AudioCache",
                        "read=$cachedBytesRead cacheSize=$cacheSizeBytes"
                    )
                }

                override fun onCacheIgnored(reason: Int) {
                    android.util.Log.w("AudioCache", "cache ignored reason=$reason")
                }
            })
    }

    @Provides
    @Singleton
    fun provideAudioPlayerManager(
        @ApplicationContext context: Context,
        scrobbleManager: ScrobbleManager,
        credentialsManager: CredentialsManager
    ): AudioPlayerManager {
        return AudioPlayerManager(
            context = context,
            scrobbleManager = scrobbleManager,
            credentialsManager = credentialsManager
        )
    }
}
