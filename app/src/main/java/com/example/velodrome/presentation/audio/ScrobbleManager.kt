package com.example.velodrome.presentation.audio

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.velodrome.data.worker.ScrobbleWorker
import com.example.velodrome.domain.repository.ScrobbleRepository
import com.example.velodrome.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling scrobble functionality.
 * Saves scrobbles to Room first, then enqueues WorkManager for reliable delivery.
 */
@Singleton
class ScrobbleManager @Inject constructor(
    private val scrobbleRepository: ScrobbleRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workManager by lazy { WorkManager.getInstance(context) }

    // Track the current track being played to avoid duplicate submissions
    private var currentScrobbleTrackId: String? = null

    /**
     * Check if scrobbling should happen at the current position.
     * Should be called periodically during playback.
     */
    fun checkAndScrobble(trackId: String, currentPositionMs: Long, durationMs: Long) {
        if (durationMs <= 0) return
        // Ya fue scrobbleada esta pista — no repetir
        if (trackId == currentScrobbleTrackId) return

        scope.launch {
            try {
                val scrobbleEnabled = settingsRepository.scrobbleEnabled.first()
                if (!scrobbleEnabled) return@launch

                // Last.fm spec: scrobblear al 50% de duración o a los 4 minutos, lo que ocurra antes
                val scrobbleThreshold = minOf(durationMs / 2, 4 * 60 * 1000L)

                if (currentPositionMs >= scrobbleThreshold) {
                    // Marcar inmediatamente para evitar llamadas duplicadas del polling
                    currentScrobbleTrackId = trackId
                    scrobble(trackId)
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Reset scrobble state for a new track.
     */
    fun onTrackChanged() {
        currentScrobbleTrackId = null
    }

    /**
     * Send "now playing" notification when track starts.
     * This is optional - some servers use it to show what's currently playing.
     */
    fun sendNowPlaying(trackId: String) {
        scope.launch {
            try {
                val scrobbleEnabled = settingsRepository.scrobbleEnabled.first()
                if (!scrobbleEnabled) {
                    return@launch
                }

                // Send now playing (submission = false)
                scrobbleRepository.scrobble(trackId, System.currentTimeMillis(), submission = false)
            } catch (_: Exception) { }
        }
    }

    /**
     * Perform the scrobble — save to Room and enqueue WorkManager.
     */
    private suspend fun scrobble(trackId: String) {
        val timestamp = System.currentTimeMillis()
        try {
            // Step 1: Save to Room immediately
            scrobbleRepository.savePendingScrobble(trackId, timestamp)

            // Step 2: Enqueue WorkManager for reliable delivery
            enqueueScrobbleWork()

            // Mark as tracked (don't set currentScrobbleTrackId until WorkManager succeeds)
        } catch (_: Exception) { }
    }

    /**
     * Enqueue a OneTimeWorkRequest to process pending scrobbles.
     */
    private fun enqueueScrobbleWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ScrobbleWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            SCROBBLE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    companion object {
        const val SCROBBLE_WORK_NAME = "scrobble_pending_work"
    }
}