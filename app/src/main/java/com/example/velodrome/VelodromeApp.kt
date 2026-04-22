package com.example.velodrome

import android.app.Application
import android.util.Log
import com.example.velodrome.data.datasource.CacheService
import com.example.velodrome.data.datasource.ImageCacheDataSource
import com.example.velodrome.data.datasource.MusicCacheDataSource
import com.example.velodrome.presentation.audio.AudioPlayerManager
import com.example.velodrome.presentation.audio.ScrobbleManager
import com.example.velodrome.util.CredentialsManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

private const val TAG = "VelodromeApp"

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
        Log.d(TAG, "VelodromeApp onCreate start")

        try {
            // Initialize cache services
            CacheService.initialize(
                imageCache = imageCacheDataSource,
                musicCache = musicCacheDataSource
            )
            Log.d(TAG, "VelodromeApp onCreate end - cache initialized")
        } catch (e: Exception) {
            Log.e(TAG, "VelodromeApp onCreate failed", e)
        }
    }
}