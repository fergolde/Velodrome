package com.example.velodrome

import android.app.Application
import com.example.velodrome.presentation.audio.AudioPlayerManager
import com.example.velodrome.util.CredentialsManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VelodromeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize secure credential storage
        CredentialsManager.init(this)
        // Initialize audio player manager
        AudioPlayerManager.initialize(this)
    }
}