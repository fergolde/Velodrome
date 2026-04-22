package com.example.velodrome

import android.app.Application
import com.example.velodrome.data.datasource.CacheService
import com.example.velodrome.data.datasource.ImageCacheDataSource
import com.example.velodrome.data.datasource.MusicCacheDataSource
import com.example.velodrome.presentation.audio.AudioPlayerManager
import com.example.velodrome.presentation.audio.ScrobbleManager
import com.example.velodrome.util.CredentialsManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VelodromeApp : Application() {

    @Inject
    lateinit var scrobbleManager: ScrobbleManager

    @Inject
    lateinit var imageCacheDataSource: ImageCacheDataSource

    @Inject
    lateinit var musicCacheDataSource: MusicCacheDataSource

    @Inject
    lateinit var credentialsManager: CredentialsManager

    override fun onCreate() {
        super.onCreate()
        
        // Initialize cache services
        CacheService.initialize(
            imageCache = imageCacheDataSource,
            musicCache = musicCacheDataSource
        )
    }
}