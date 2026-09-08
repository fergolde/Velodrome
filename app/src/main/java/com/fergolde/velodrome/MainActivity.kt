package com.fergolde.velodrome

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fergolde.velodrome.data.worker.SyncLibraryWorker
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.presentation.VelodromeMainApp
import com.fergolde.velodrome.presentation.player.SharedPlayerViewModel
import com.fergolde.velodrome.presentation.screen.settings.parseHexColor
import com.fergolde.velodrome.ui.theme.VelodromeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    // Reloj y sistema leen la sesión vía la notificación multimedia: sin este
    // permiso (Android 13+) el reloj queda ciego. Solo se pide una vez.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()

        // Phone: locked to portrait. Tablet (sw600dp+): allows sensor-based rotation.
        requestedOrientation = if (resources.getBoolean(R.bool.allow_rotation)) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        enableEdgeToEdge()

        // Trigger WorkManager sync on app start
        triggerLibrarySync()

        setContent {
            // Leer accent color del repositorio en tiempo real
            val accentColorHex by settingsRepository.accentColor
                .collectAsStateWithLifecycle(initialValue = "#C8FF00")
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun triggerLibrarySync() {
        SyncLibraryWorker.enqueueImmediate(this)
        SyncLibraryWorker.enqueuePeriodic(this)
    }
}