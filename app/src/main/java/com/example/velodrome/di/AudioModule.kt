package com.example.velodrome.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.example.velodrome.presentation.audio.AudioPlayerManager
import com.example.velodrome.presentation.audio.ScrobbleManager
import com.example.velodrome.util.CredentialsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton

private const val AUDIO_CACHE_SIZE_BYTES = 2L * 1024 * 1024 * 1024 // 2GB

@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideSimpleCache(
        @ApplicationContext context: Context
    ): SimpleCache {
        val cacheDir = File(context.filesDir, "audioCache").also { it.mkdirs() }
        val databaseProvider = StandaloneDatabaseProvider(context)
        val evictor = LeastRecentlyUsedCacheEvictor(AUDIO_CACHE_SIZE_BYTES)
        return SimpleCache(cacheDir, evictor, databaseProvider)
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
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Provides
    @Singleton
    fun provideAudioPlayerManager(
        @ApplicationContext context: Context,
        scrobbleManager: ScrobbleManager,
        credentialsManager: CredentialsManager,
        cacheDataSourceFactory: CacheDataSource.Factory
    ): AudioPlayerManager {
        return AudioPlayerManager(
            context = context,
            scrobbleManager = scrobbleManager,
            credentialsManager = credentialsManager,
            cacheDataSourceFactory = cacheDataSourceFactory
        )
    }
}
