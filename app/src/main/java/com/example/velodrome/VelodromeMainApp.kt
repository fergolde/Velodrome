package com.example.velodrome

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.velodrome.domain.repository.SettingsRepository
import com.example.velodrome.presentation.audio.ScrobbleManager
import com.example.velodrome.util.CredentialsManager
import com.example.velodrome.util.NavidromeImageInterceptor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class VelodromeApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject
    lateinit var scrobbleManager: ScrobbleManager

    @Inject
    lateinit var credentialsManager: CredentialsManager

    @Inject
    lateinit var navidromeImageInterceptor: NavidromeImageInterceptor

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            check(::workerFactory.isInitialized) {
                "workerFactory no inicializado — posible ciclo de dependencias"
            }
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun newImageLoader(context: Context): ImageLoader {
        // Lectura síncrona desde SharedPreferences
        val prefs = context.getSharedPreferences("velodrome_cache_prefs", Context.MODE_PRIVATE)
        val imageLimitMb = prefs.getInt("image_cache_size_mb", 200).toLong()

        return ImageLoader.Builder(context)
            .components {
                // Añadir el interceptor de autenticación para coverart
                add(navidromeImageInterceptor)
                // Añadir el fetcher de red con OkHttp
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { okHttpClient }
                    )
                )
            }
            // Memory cache: 25% RAM
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            // Disk cache: usa setting imageCacheSizeMb del usuario (convertido a bytes)
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache").toOkioPath())
                    .maxSizeBytes(imageLimitMb * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}