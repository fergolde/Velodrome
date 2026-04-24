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
        Log.d(TAG, "VelodromeApp onCreate")
    }

    override fun newImageLoader(): ImageLoader {
        // Image cache: usa setting del usuario (en MB)
        val imageLimitMb = runBlocking {
            settingsRepository.imageCacheSizeMb.first()
        }.toLong()

        return ImageLoader.Builder(applicationContext)
            // Memory cache: 25% RAM (solo acepta porcentaje, no bytes)
            .memoryCache {
                MemoryCache.Builder(applicationContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            // Disk cache: usa setting imageCacheSizeMb del usuario (convertido a bytes)
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(imageLimitMb * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}