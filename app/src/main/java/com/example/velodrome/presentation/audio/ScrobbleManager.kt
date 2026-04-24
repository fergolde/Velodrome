package com.example.velodrome.presentation.audio

import android.util.Log
import com.example.velodrome.domain.repository.ScrobbleRepository
import com.example.velodrome.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling scrobble functionality.
 * Sends scrobble info to the server when enabled.
 */
@Singleton
class ScrobbleManager @Inject constructor(
    private val scrobbleRepository: ScrobbleRepository,
    private val settingsRepository: SettingsRepository
) {
    private val TAG = "ScrobbleManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track the current track being played to avoid duplicate submissions
    private var currentScrobbleTrackId: String? = null

    /**
     * Check if scrobbling should happen at the current position.
     * Should be called periodically during playback.
     */
    fun checkAndScrobble(trackId: String, currentPositionMs: Long, durationMs: Long) {
        if (durationMs <= 0) return
        // Ignore if this is a different track than what we're tracking
        if (trackId != currentScrobbleTrackId && currentScrobbleTrackId != null) return

        scope.launch {
            try {
                val scrobbleEnabled = settingsRepository.scrobbleEnabled.first()
                if (!scrobbleEnabled) {
                    return@launch
                }

                // Already scrobbled this track
                if (trackId == currentScrobbleTrackId) {
                    return@launch
                }

                // Calculate halfway point (50% of duration)
                val halfwayPoint = durationMs / 2

                // If we've passed the halfway point, scrobble
                if (currentPositionMs >= halfwayPoint) {
                    // Mark as about to scrobble to prevent duplicates
                    currentScrobbleTrackId = trackId
                    Log.d(TAG, "Halfway point reached for track: $trackId, position: $currentPositionMs/$durationMs")
                    scrobble(trackId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking scrobble: ${e.message}")
            }
        }
    }

    /**
     * Reset scrobble state for a new track.
     */
    fun onTrackChanged(trackId: String) {
        currentScrobbleTrackId = null
        Log.d(TAG, "Reset scrobble state for new track: $trackId")
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
                val result = scrobbleRepository.scrobble(trackId, System.currentTimeMillis(), submission = false)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Now playing sent for track: $trackId")
                    },
                    onFailure = { error ->
                        Log.w(TAG, "Failed to send now playing: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error sending now playing: ${e.message}")
            }
        }
    }

    /**
     * Perform the scrobble.
     */
    private suspend fun scrobble(trackId: String) {
        try {
            val result = scrobbleRepository.scrobble(trackId, System.currentTimeMillis(), submission = true)
            result.fold(
                onSuccess = {
                    currentScrobbleTrackId = trackId
                    Log.d(TAG, "Successfully scrobbled track: $trackId")
                },
                onFailure = { error ->
                    // On failure, reset so we can try again
                    currentScrobbleTrackId = null
                    Log.e(TAG, "Failed to scrobble track: $trackId, error: ${error.message}")
                }
            )
        } catch (e: Exception) {
            currentScrobbleTrackId = null
            Log.e(TAG, "Exception during scrobble: ${e.message}")
        }
    }
}