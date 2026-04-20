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

    override fun onCreate() {
        super.onCreate()
        // Initialize secure credential storage
        CredentialsManager.init(this)
        // Initialize audio player manager
        AudioPlayerManager.initialize(this)
        // Set scrobble manager reference
        AudioPlayerManager.scrobbleManager = scrobbleManager

        // Initialize cache services
        CacheService.initialize(
            imageCache = imageCacheDataSource,
            musicCache = musicCacheDataSource
        )

        // Set music cache for AudioPlayerManager
        AudioPlayerManager.musicCacheDataSource = musicCacheDataSource
    }
}