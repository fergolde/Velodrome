package com.example.velodrome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.velodrome.data.worker.SyncLibraryWorker
import com.example.velodrome.domain.repository.SettingsRepository
import com.example.velodrome.presentation.VelodromeMainApp
import com.example.velodrome.presentation.player.SharedPlayerViewModel
import com.example.velodrome.ui.theme.VelodromeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Trigger WorkManager sync on app start
        triggerLibrarySync()

        setContent {
            VelodromeTheme(settingsRepository = settingsRepository) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Get SharedPlayerViewModel - must be done in compose context
                    val sharedPlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel<SharedPlayerViewModel>()
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