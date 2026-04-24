package com.example.velodrome

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.velodrome.domain.repository.SettingsRepository
import com.example.velodrome.presentation.audio.ScrobbleManager
import com.example.velodrome.util.CredentialsManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject

private const val TAG = "VelodromeApp"

@HiltAndroidApp
class VelodromeApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var scrobbleManager: ScrobbleManager

    @Inject
    lateinit var credentialsManager: CredentialsManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VelodromeApp onCreate start")
        Log.d(TAG, "VelodromeApp onCreate end")
    }

    override fun newImageLoader(): ImageLoader {
        // Obtener límite de configuración
        val limitMb = runBlocking {
            settingsRepository.imageCacheSizeMb.first()
        }.toLong()

        return ImageLoader.Builder(applicationContext)
            .memoryCache {
                MemoryCache.Builder(applicationContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
    }
}