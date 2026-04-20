package com.example.velodrome.presentation.audio

import android.content.ComponentName
import android.util.Log
import android.content.Context
import android.net.Uri
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
    private val _currentTrackId = MutableStateFlow<String?>(null)
    val currentTrackId: StateFlow<String?> = _currentTrackId.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Scrobble manager reference
    var scrobbleManager: ScrobbleManager? = null
    
    // Callback for loading more tracks when playlist runs out
    private var loadMoreCallback: (() -> Unit)? = null

    // Position polling interval in milliseconds
    var positionUpdateIntervalMs: Long = 1000L  // 1 second for progress bar

    // Position polling handler - runs when playing to keep UI updated
    private val positionHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var lastScrobbleCheckTime: Long = 0
    private val scrobbleCheckIntervalMs: Long = 10000L  // 10 seconds for scrobble check

    private val positionPollRunnable = object : Runnable {
        override fun run() {
            mediaController?.let { controller ->
                if (controller.isPlaying) {
                    _currentPosition.value = controller.currentPosition
                    _bufferedPosition.value = controller.bufferedPosition
                    _duration.value = controller.duration

                    // Check for scrobble every 10 seconds
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastScrobbleCheckTime >= scrobbleCheckIntervalMs) {
                        lastScrobbleCheckTime = currentTime
                        scrobbleManager?.let { sm ->
                            val trackId = _currentTrackId.value
                            val duration = _duration.value
                            if (trackId != null && duration > 0) {
                                sm.checkAndScrobble(trackId, _currentPosition.value, duration)
                            }
                        }
                    }
                }
            }
            positionHandler.postDelayed(this, positionUpdateIntervalMs)
        }
    }

    /**
     * Initialize the AudioPlayerManager with application context.
     * Should be called once from Application class.
     */
    fun initialize(context: Context) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, AudioPlayerService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
            } catch (e: Exception) {
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
                // Start/stop position polling
                if (isPlaying) {
                    positionHandler.post(positionPollRunnable)
                } else {
                    positionHandler.removeCallbacks(positionPollRunnable)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
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
            val newTrackId = playlist[index].id
            val previousTrackId = _currentTrackId.value

            _currentIndex.value = index
            _currentTrack.value = playlist[index]
            _currentTrackId.value = newTrackId

            // Only reset scrobble state if track actually changed
            if (previousTrackId != newTrackId) {
                scrobbleManager?.onTrackChanged(newTrackId)
                // Send "now playing" notification when track changes
                scrobbleManager?.sendNowPlaying(newTrackId)
                lastScrobbleCheckTime = 0  // Reset scrobble timer
            }
        }
    }

    /**
     * Start playback of a track with a given playlist
     */
    fun playTrack(track: Track, playlist: List<Track>, startIndex: Int = 0) {
        _playlist.value = playlist
        _currentIndex.value = startIndex
        _currentTrack.value = track
        _currentTrackId.value = track.id

        // Reset scrobble state for new track and send now playing
        scrobbleManager?.onTrackChanged(track.id)
        scrobbleManager?.sendNowPlaying(track.id)
        lastScrobbleCheckTime = 0  // Reset scrobble timer

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
     * Append tracks to the current playlist in MediaController
     */
    fun appendToPlaylist(tracks: List<Track>) {
        if (tracks.isEmpty()) return

        // Add to local playlist
        _playlist.value = _playlist.value + tracks

        // Build media items for new tracks
        val startIndex = _playlist.value.size - tracks.size
        val mediaItems = tracks.mapIndexed { index, t ->
            val streamUrl = CredentialsManager.getStreamUrl(t.id)
            val coverUrl = t.coverArtId?.let { CredentialsManager.getCoverArtUrl(it, 400) }

            MediaItem.Builder()
                .setMediaId((startIndex + index).toString())
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

        // Append to MediaController playlist
        mediaController?.let { controller ->
            controller.addMediaItems(mediaItems)
            Log.d(TAG, "Appended ${tracks.size} tracks to MediaController, total: ${controller.mediaItemCount}")
        }
    }

    /**
     * Play the currently loaded track
     */
    fun play() {
        mediaController?.play()
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
            
            // Check if we're at the end of playlist - call callback to load more
            val currentIndex = mediaController?.currentMediaItemIndex ?: 0
            val totalItems = mediaController?.mediaItemCount ?: 0
            if (currentIndex >= totalItems - 3) {
                Log.d(TAG, "Near end of playlist, triggering loadMore callback")
                loadMoreCallback?.invoke()
            }
            true
        } else {
            // No more items - trigger callback to load more
            Log.d(TAG, "No more items in playlist, triggering loadMore callback")
            loadMoreCallback?.invoke()
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
            // Will auto-advance to next - check if we need more songs
            val currentIndex = mediaController?.currentMediaItemIndex ?: 0
            val totalItems = mediaController?.mediaItemCount ?: 0
            if (currentIndex >= totalItems - 3) {
                Log.d(TAG, "Near end during auto-advance, triggering loadMore callback")
                loadMoreCallback?.invoke()
            }
        } else {
            // End of playlist - trigger callback to load more
            Log.d(TAG, "Playlist ended, triggering loadMore callback")
            loadMoreCallback?.invoke()
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
        _currentTrackId.value = null
        _currentIndex.value = 0
        mediaController?.clearMediaItems()
    }

    /**
     * Release resources
     */
    fun release() {
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

    /**
     * Set callback for loading more tracks when playlist runs out
     */
    fun setLoadMoreCallback(callback: () -> Unit) {
        loadMoreCallback = callback
        Log.d(TAG, "LoadMoreCallback set")
    }
}