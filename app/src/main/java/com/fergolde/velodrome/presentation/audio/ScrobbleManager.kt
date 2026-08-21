package com.fergolde.velodrome.presentation.audio

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fergolde.velodrome.data.worker.ScrobbleWorker
import com.fergolde.velodrome.domain.repository.ScrobbleRepository
import com.fergolde.velodrome.domain.repository.SettingsRepository
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
    @param:ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workManager by lazy { WorkManager.getInstance(context) }

    // Track the current track being played to avoid duplicate submissions
    private var currentScrobbleTrackId: String? = null

    /**
     * Mark a track as fully played (natural end or repeat transition).
     * Skips the threshold logic: by definition the track was heard completely.
     */
    fun markTrackPlayed(trackId: String) {
        if (trackId == currentScrobbleTrackId) return

        // Marcar de inmediato para que eventos solapados no dupliquen el envío
        currentScrobbleTrackId = trackId

        scope.launch {
            try {
                val scrobbleEnabled = settingsRepository.scrobbleEnabled.first()
                if (!scrobbleEnabled) return@launch

                scrobble(trackId)
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