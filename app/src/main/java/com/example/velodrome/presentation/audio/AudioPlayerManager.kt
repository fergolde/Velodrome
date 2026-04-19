package com.example.velodrome.presentation.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.velodrome.domain.model.Track
import com.example.velodrome.util.CredentialsManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton manager for audio playback.
 * Provides a clean interface between UI and AudioPlayerService.
 * Manages MediaController connection and exposes state to observers.
 */
object AudioPlayerManager {

    private const val TAG = "AudioPlayerManager"

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Position polling handler - runs when playing to keep UI updated
    private val positionHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val positionPollRunnable = object : Runnable {
        override fun run() {
            mediaController?.let { controller ->
                if (controller.isPlaying) {
                    _currentPosition.value = controller.currentPosition
                    _bufferedPosition.value = controller.bufferedPosition
                }
            }
            positionHandler.postDelayed(this, 500)
        }
    }

    /**
     * Initialize the AudioPlayerManager with application context.
     * Should be called once from Application class.
     */
    fun initialize(context: Context) {
        Log.d(TAG, "Initializing AudioPlayerManager")

        val sessionToken = SessionToken(
            context,
            ComponentName(context, AudioPlayerService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
                Log.d(TAG, "MediaController connected successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    /**
     * Setup listener for controller events
     */
    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                Log.d(TAG, "onIsPlayingChanged: $isPlaying")
                // Start/stop position polling
                if (isPlaying) {
                    positionHandler.post(positionPollRunnable)
                } else {
                    positionHandler.removeCallbacks(positionPollRunnable)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "onPlaybackStateChanged: $playbackState")
                when (playbackState) {
                    Player.STATE_READY -> {
                        _isBuffering.value = false
                        _duration.value = mediaController?.duration ?: 0L
                    }
                    Player.STATE_BUFFERING -> {
                        _isBuffering.value = true
                    }
                    Player.STATE_ENDED -> {
                        _isPlaying.value = false
                        positionHandler.removeCallbacks(positionPollRunnable)
                        handlePlaybackEnded()
                    }
                    Player.STATE_IDLE -> {
                        _isBuffering.value = false
                        positionHandler.removeCallbacks(positionPollRunnable)
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.d(TAG, "onMediaItemTransition: ${mediaItem?.mediaMetadata?.title}")
                mediaItem?.let { updateCurrentTrackFromMediaItem(it) }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                _currentPosition.value = newPosition.positionMs
            }
        })
    }

    /**
     * Update current track from MediaItem metadata
     */
    private fun updateCurrentTrackFromMediaItem(mediaItem: MediaItem) {
        val playlist = _playlist.value
        val index = mediaItem.mediaId.toIntOrNull() ?: -1
        if (index in playlist.indices) {
            _currentIndex.value = index
            _currentTrack.value = playlist[index]
        }
    }

    /**
     * Start playback of a track with a given playlist
     */
    fun playTrack(track: Track, playlist: List<Track>, startIndex: Int = 0) {
        Log.d(TAG, "playTrack: ${track.title}, playlist size: ${playlist.size}, startIndex: $startIndex")
        _playlist.value = playlist
        _currentIndex.value = startIndex
        _currentTrack.value = track

        // Build media items with proper IDs for tracking
        val mediaItems = playlist.mapIndexed { index, t ->
            val streamUrl = CredentialsManager.getStreamUrl(t.id)
            val coverUrl = t.coverArtId?.let { CredentialsManager.getCoverArtUrl(it, 400) }

            MediaItem.Builder()
                .setMediaId(index.toString())
                .setUri(streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(t.title)
                        .setArtist(t.artistName)
                        .setAlbumTitle(t.albumName)
                        .apply {
                            coverUrl?.let { setArtworkUri(Uri.parse(it)) }
                        }
                        .build()
                )
                .build()
        }

        mediaController?.let { controller ->
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
        }
    }

    /**
     * Play the currently loaded track
     */
    fun play() {
        mediaController?.play()
    }

    /**
     * Pause playback
     */
    fun pause() {
        mediaController?.pause()
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    /**
     * Seek to position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    /**
     * Get current position in milliseconds directly from MediaController
     * Also updates the stateflow so UI gets the update
     */
    fun getCurrentPositionMs(): Long {
        val position = mediaController?.currentPosition ?: 0L
        _currentPosition.value = position
        return position
    }

    /**
     * Skip to next track
     */
    fun next(): Boolean {
        return if (mediaController?.hasNextMediaItem() == true) {
            mediaController?.seekToNextMediaItem()
            true
        } else {
            false
        }
    }

    /**
     * Skip to previous track
     */
    fun previous(): Boolean {
        return if (mediaController?.hasPreviousMediaItem() == true) {
            mediaController?.seekToPreviousMediaItem()
            true
        } else {
            mediaController?.seekToPreviousMediaItem() // Also handles replaying current track
            false
        }
    }

    /**
     * Handle playback ended - move to next or stop
     */
    private fun handlePlaybackEnded() {
        if (mediaController?.hasNextMediaItem() == true) {
            // Will auto-advance to next
        } else {
            // End of playlist
            _isPlaying.value = false
        }
    }

    /**
     * Set playlist without starting playback
     */
    fun setPlaylist(playlist: List<Track>) {
        _playlist.value = playlist
    }

    /**
     * Clear current playlist
     */
    fun clearPlaylist() {
        _playlist.value = emptyList()
        _currentTrack.value = null
        _currentIndex.value = 0
        mediaController?.clearMediaItems()
    }

    /**
     * Release resources
     */
    fun release() {
        Log.d(TAG, "Releasing AudioPlayerManager")
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }

    // Callbacks for service events (can be used if needed)
    fun onPlaybackStateChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun onReady(durationMs: Long) {
        _duration.value = durationMs
        _isBuffering.value = false
    }

    fun onPlaybackCompleted() {
        _isPlaying.value = false
    }

    fun onBuffering() {
        _isBuffering.value = true
    }

    fun onMediaItemChanged(mediaItem: MediaItem) {
        // MediaController handles this via listener
    }
}