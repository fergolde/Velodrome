package com.example.velodrome

import android.app.Application
import android.util.Log
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
    lateinit var credentialsManager: CredentialsManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VelodromeApp onCreate start")
        // Cache services are now handled automatically by Media3 SimpleCache (audio)
        // and Coil (images)
        Log.d(TAG, "VelodromeApp onCreate end")
    }
}