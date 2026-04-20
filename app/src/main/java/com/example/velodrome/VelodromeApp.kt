package com.example.velodrome

import android.app.Application
import com.example.velodrome.presentation.audio.AudioPlayerManager
import com.example.velodrome.presentation.audio.ScrobbleManager
import com.example.velodrome.util.CredentialsManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VelodromeApp : Application() {

    @Inject
    lateinit var scrobbleManager: ScrobbleManager

    override fun onCreate() {
        super.onCreate()
        // Initialize secure credential storage
        CredentialsManager.init(this)
        // Initialize audio player manager
        AudioPlayerManager.initialize(this)
        // Set scrobble manager reference
        AudioPlayerManager.scrobbleManager = scrobbleManager
    }
}