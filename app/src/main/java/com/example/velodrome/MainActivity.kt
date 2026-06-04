package com.example.velodrome

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.velodrome.data.worker.SyncLibraryWorker
import com.example.velodrome.domain.repository.SettingsRepository
import com.example.velodrome.presentation.VelodromeMainApp
import com.example.velodrome.presentation.player.SharedPlayerViewModel
import com.example.velodrome.presentation.screen.settings.parseHexColor
import com.example.velodrome.ui.theme.VelodromeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Phone: locked to portrait. Tablet (sw600dp+): allows sensor-based rotation.
        if (resources.getBoolean(R.bool.allow_rotation)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        enableEdgeToEdge()

        // Trigger WorkManager sync on app start
        triggerLibrarySync()

        setContent {
            // Leer accent color del repositorio en tiempo real
            val accentColorHex by settingsRepository.accentColor
                .collectAsState(initial = "#C8FF00")
            val accentColor = parseHexColor(accentColorHex)

            VelodromeTheme(accentColor = accentColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val sharedPlayerViewModel = hiltViewModel<SharedPlayerViewModel>()
                    VelodromeMainApp(sharedPlayerViewModel = sharedPlayerViewModel)
                }
            }
        }
    }

    private fun triggerLibrarySync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncLibraryWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "sync_library",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
    }
}