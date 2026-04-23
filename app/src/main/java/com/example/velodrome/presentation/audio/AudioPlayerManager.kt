package com.example.velodrome.presentation.audio

import android.content.ComponentName
import android.util.Log
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.velodrome.domain.model.Track
import com.example.velodrome.util.CredentialsManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for audio playback with MediaController.
 * Uses companion object for static access from AudioPlayerService.
 */
@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scrobbleManager: ScrobbleManager,
    private val credentialsManager: CredentialsManager,
    private val cacheDataSourceFactory: CacheDataSource.Factory
) {
    companion object {
        @Volatile private var self: AudioPlayerManager? = null
        
        fun onPlaybackStateChanged(isPlaying: Boolean) {
            self?.onPlaybackStateChangedInternal(isPlaying)
        }
        fun onReady(durationMs: Long) {
            self?.onReadyInternal(durationMs)
        }
        fun onPlaybackCompleted() {
            self?.onPlaybackCompletedInternal()
        }
        fun onBuffering() {
            self?.onBufferingInternal()
        }
        fun onMediaItemChanged(mediaItem: MediaItem) {
            // no-op for static
        }
    }
    
    init { self = this }

    private val TAG = "AudioPlayerManager"

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
    var isLoadingMoreCallbackInvoked = false

    private val playerScope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var loadMoreCallback: (() -> Unit)? = null
    var positionUpdateIntervalMs: Long = 1000L

    private val positionHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var lastScrobbleCheckTime: Long = 0
    private val scrobbleCheckIntervalMs: Long = 10000L

    init {
        Log.d(TAG, "init: building MediaController...")
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                Log.d(TAG, "init: MediaController ready: $mediaController")
                setupControllerListener()
            } catch (e: Exception) {
                Log.e(TAG, "init: failed to get MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private val positionPollRunnable = object : Runnable {
        override fun run() {
            mediaController?.let { controller ->
                if (controller.isPlaying) {
                    _currentPosition.value = controller.currentPosition
                    _bufferedPosition.value = controller.bufferedPosition
                    _duration.value = controller.duration
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastScrobbleCheckTime >= scrobbleCheckIntervalMs) {
                        lastScrobbleCheckTime = currentTime
                        val trackId = _currentTrackId.value
                        val duration = _duration.value
                        if (trackId != null && duration > 0) {
                            scrobbleManager.checkAndScrobble(trackId, _currentPosition.value, duration)
                        }
                    }
                }
            }
            positionHandler.postDelayed(this, positionUpdateIntervalMs)
        }
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) positionHandler.post(positionPollRunnable)
                else positionHandler.removeCallbacks(positionPollRunnable)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> { _isBuffering.value = false; _duration.value = mediaController?.duration ?: 0L }
                    Player.STATE_BUFFERING -> { _isBuffering.value = true }
                    Player.STATE_ENDED -> { _isPlaying.value = false; positionHandler.removeCallbacks(positionPollRunnable); handlePlaybackEnded() }
                    Player.STATE_IDLE -> { _isBuffering.value = false; positionHandler.removeCallbacks(positionPollRunnable) }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let {
                    updateCurrentTrackFromMediaItem(it)
                    checkIfNeedMoreSongs() // <--- ¡AÑADIR ESTA LÍNEA AQUÍ!
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                _currentPosition.value = newPosition.positionMs
            }
        })
    }

    // Añade esta función privada a la misma clase AudioPlayerManager
    private fun checkIfNeedMoreSongs() {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex
        val totalItems = controller.mediaItemCount
        val remaining = totalItems - currentIndex

        Log.d(TAG, "Remaining tracks: $remaining")

        // Si quedan menos de 3 y no hemos disparado el callback aún
        if (remaining <= 3 && !isLoadingMoreCallbackInvoked) {
            isLoadingMoreCallbackInvoked = true
            Log.d(TAG, "Threshold reached, requesting more songs...")
            loadMoreCallback?.invoke()
        }
    }

    fun onPlaybackStateChangedInternal(isPlaying: Boolean) { _isPlaying.value = isPlaying }
    fun onReadyInternal(durationMs: Long) { _duration.value = durationMs; _isBuffering.value = false }
    fun onPlaybackCompletedInternal() { _isPlaying.value = false }
    fun onBufferingInternal() { _isBuffering.value = true }

    private fun updateCurrentTrackFromMediaItem(mediaItem: MediaItem) {
        val playlist = _playlist.value
        val index = mediaItem.mediaId.toIntOrNull() ?: -1
        if (index in playlist.indices) {
            _currentIndex.value = index
            _currentTrack.value = playlist[index]
            _currentTrackId.value = playlist[index].id
            val previousId = _currentTrackId.value
            if (previousId != playlist[index].id) {
                scrobbleManager.onTrackChanged(playlist[index].id)
                scrobbleManager.sendNowPlaying(playlist[index].id)
                lastScrobbleCheckTime = 0
            }
        }
    }

fun playTrack(track: Track, playlist: List<Track>, startIndex: Int = 0) {
        _playlist.value = playlist
        _currentIndex.value = startIndex
        _currentTrack.value = track
        _currentTrackId.value = track.id
        scrobbleManager.onTrackChanged(track.id)
        scrobbleManager.sendNowPlaying(track.id)
        lastScrobbleCheckTime = 0
        isLoadingMoreCallbackInvoked = false

        val mediaItems = playlist.mapIndexed { index, t ->
            val streamUrl = getStreamUrl(t)
            val coverUrl = t.coverArtId?.let { credentialsManager.getCoverArtUrl(it, 400) }
            MediaItem.Builder().setMediaId(index.toString()).setUri(streamUrl)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artistName).setAlbumTitle(t.albumName)
                    .apply { coverUrl?.let { setArtworkUri(Uri.parse(it)) } }.build()).build()
        }

        doPlayWithController(mediaItems, startIndex)
    }

    private fun doPlayWithController(mediaItems: List<MediaItem>, startIndex: Int) {
        // Try to get the controller directly if it's ready
        mediaController?.let { controller ->
            Log.d(TAG, "doPlayWithController: using existing controller, ${mediaItems.size} tracks")
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
            Log.d(TAG, "Playback started with ${mediaItems.size} tracks")
            return
        }

        // If not ready, wait for it
        val future = controllerFuture
        if (future == null) {
            Log.e(TAG, "doPlayWithController: controllerFuture is null, cannot play")
            return
        }

        Log.d(TAG, "doPlayWithController: future isDone=${future.isDone}, mediaController=$mediaController")

        if (future.isDone) {
            try {
                Log.d(TAG, "doPlayWithController: future is done, getting controller...")
                mediaController = future.get()
                Log.d(TAG, "doPlayWithController: got controller=$mediaController")
                doPlayWithController(mediaItems, startIndex)
            } catch (e: Exception) {
                Log.e(TAG, "doPlayWithController: Failed to get MediaController", e)
            }
            return
        }

        // Wait for the future to complete, then retry
        Log.d(TAG, "doPlayWithController: future not done, adding listener")
        future.addListener({
            try {
                Log.d(TAG, "doPlayWithController: future completed, getting controller...")
                mediaController = future.get()
                Log.d(TAG, "doPlayWithController: listener got controller=$mediaController")
                doPlayWithController(mediaItems, startIndex)
            } catch (e: Exception) {
                Log.e(TAG, "doPlayWithController: Failed to get MediaController in callback", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun getStreamUrl(track: Track): String {
        // SimpleCache handles caching automatically via CacheDataSource.Factory
        // ExoPlayer will intercept streaming and cache data on-the-fly
        return credentialsManager.getStreamUrl(track.id)
    }

    fun appendToPlaylist(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        _playlist.value += tracks
        val startIndex = _playlist.value.size - tracks.size
        val mediaItems = tracks.mapIndexed { index, t ->
            val streamUrl = getStreamUrl(t)
            val coverUrl = t.coverArtId?.let { credentialsManager.getCoverArtUrl(it, 400) }
            MediaItem.Builder().setMediaId((startIndex + index).toString()).setUri(streamUrl)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artistName).setAlbumTitle(t.albumName)
                    .apply { coverUrl?.let { setArtworkUri(Uri.parse(it)) } }.build()).build()
        }

        // Try to add directly if controller is ready
        mediaController?.let { controller ->
            Log.d(TAG, "appendToPlaylist: adding ${mediaItems.size} items directly")
            controller.addMediaItems(mediaItems)
            isLoadingMoreCallbackInvoked = false
            return
        }

        // Wait for controller to be ready, then add
        val future = controllerFuture
        if (future == null || !future.isDone) {
            Log.w(TAG, "appendToPlaylist: controller not ready, skipping append (will retry on next track)")
            isLoadingMoreCallbackInvoked = false
            return
        }

        try {
            mediaController = future.get()
            mediaController?.addMediaItems(mediaItems)
            Log.d(TAG, "appendToPlaylist: added ${mediaItems.size} items after wait")
        } catch (e: Exception) {
            Log.e(TAG, "appendToPlaylist: failed to get controller", e)
        }
        isLoadingMoreCallbackInvoked = false
    }

    fun play() { mediaController?.play() }
    fun togglePlayPause() { mediaController?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seekTo(positionMs: Long) { mediaController?.seekTo(positionMs); _currentPosition.value = positionMs }
    fun getCurrentPositionMs(): Long = mediaController?.currentPosition ?: 0L
    fun next(): Boolean = mediaController?.hasNextMediaItem()?.also { mediaController?.seekToNextMediaItem() } ?: false
    fun previous(): Boolean = mediaController?.hasPreviousMediaItem()?.also { mediaController?.seekToPreviousMediaItem() } ?: false

    private fun handlePlaybackEnded() {
        val has = mediaController?.hasNextMediaItem() == true
        if (!has) { isLoadingMoreCallbackInvoked = true; loadMoreCallback?.invoke() }
    }

    fun setPlaylist(playlist: List<Track>) { _playlist.value = playlist }
    fun clearPlaylist() { _playlist.value = emptyList(); _currentTrack.value = null; _currentTrackId.value = null; _currentIndex.value = 0; mediaController?.clearMediaItems() }
    fun release() { controllerFuture?.let { MediaController.releaseFuture(it) }; mediaController = null }

    fun setLoadMoreCallback(callback: () -> Unit) { loadMoreCallback = callback }

    /**
     * Get the CacheDataSource.Factory for ExoPlayer configuration in AudioPlayerService.
     * Uses the injected factory from constructor.
     */
    fun getCacheDataSourceFactory(): CacheDataSource.Factory = cacheDataSourceFactory
}