package com.fergolde.velodrome.presentation.audio

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.util.CredentialsManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manager for audio playback with MediaController.
 * Injected by Hilt - singleton ensures single instance.
 */
@Singleton
class AudioPlayerManager @OptIn(UnstableApi::class)
@Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val scrobbleManager: ScrobbleManager,
    private val credentialsManager: CredentialsManager,
) {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

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

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var isLoadingMoreCallbackInvoked = false

    private val playerScope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var loadMoreCallback: (() -> Unit)? = null
    private val retryAttempts = mutableMapOf<String, Int>()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
            } catch (error: Exception) {
                Log.e(TAG, "Unable to connect MediaController", error)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        _isBuffering.value = false
                        _duration.value = mediaController?.duration ?: 0L
                        checkIfNeedMoreSongs()
                    }
                    Player.STATE_BUFFERING -> { _isBuffering.value = true }
                    Player.STATE_ENDED -> { _isPlaying.value = false; handlePlaybackEnded() }
                    Player.STATE_IDLE -> { _isBuffering.value = false }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let {
                    retryAttempts.remove(it.mediaId)
                    updateCurrentTrackFromMediaItem(it)
                    checkIfNeedMoreSongs()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val controller = mediaController ?: return
                val trackId = controller.currentMediaItem?.mediaId ?: "unknown"
                val attempts = retryAttempts[trackId] ?: 0
                Log.e(TAG, "Playback failed track=$trackId code=${error.errorCodeName}", error)

                if (attempts == 0) {
                    retryAttempts[trackId] = 1
                    controller.prepare()
                    controller.play()
                } else if (controller.hasNextMediaItem()) {
                    retryAttempts.remove(trackId)
                    controller.seekToNextMediaItem()
                    controller.play()
                } else {
                    retryAttempts.remove(trackId)
                    handlePlaybackEnded()
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                _currentPosition.value = newPosition.positionMs
            }

        })
        startPositionPolling()

    }

    /**
     * Position polling + scrobble check. Runs every 1s while playing.
     * Dual purpose: updates currentPosition for UI progress bar AND checks scrobble threshold.
     * Changing interval affects both UI responsiveness and scrobble accuracy.
     */
    private fun startPositionPolling() {
        playerScope.launch {
            isPlaying.collectLatest { playing ->
                if (playing) {
                    while (true) {
                        mediaController?.let { controller ->
                            _currentPosition.value = controller.currentPosition
                        }
                        kotlinx.coroutines.delay(1000L.milliseconds)
                    }
                }
            }
        }
    }

    // Añade esta función privada a la misma clase AudioPlayerManager
    private fun checkIfNeedMoreSongs() {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex
        val totalItems = controller.mediaItemCount
        val remaining = totalItems - currentIndex
        val shuffle = _isShuffleEnabled.value
        val repeat = _isRepeatEnabled.value

        Log.d(TAG, "checkIfNeedMoreSongs: idx=$currentIndex total=$totalItems remaining=$remaining shuffle=$shuffle repeat=$repeat callback=${loadMoreCallback != null}")

        // Si quedan menos de 3, no hay shuffle/repeat activo, disparar callback
        if (remaining <= 3 && !shuffle && !repeat) {
            loadMoreCallback?.let { callback ->
                Log.d(TAG, "checkIfNeedMoreSongs: triggering loadMoreCallback")
                isLoadingMoreCallbackInvoked = true
                callback()
            }
        }
    }

    private fun updateCurrentTrackFromMediaItem(mediaItem: MediaItem) {
        val playlist = _playlist.value
        val index = playlist.indexOfFirst { it.id == mediaItem.mediaId }
        if (index in playlist.indices) {
            val previousId = _currentTrackId.value  // guardar ANTES de actualizar
            _currentIndex.value = index
            _currentTrack.value = playlist[index]
            _currentTrackId.value = playlist[index].id
            // Notificar scrobble solo si realmente cambió la canción
            if (previousId != playlist[index].id) {
                scrobbleManager.onTrackChanged()
                scrobbleManager.sendNowPlaying(playlist[index].id)
            }
        }
    }

    fun playTrack(track: Track, playlist: List<Track>, startIndex: Int = 0) {
        _playlist.value = playlist
        // No actualizamos currentIndex/currentTrack aquí - el listener onMediaItemTransition 
        // del MediaController es la única fuente de verdad
        isLoadingMoreCallbackInvoked = false

        val mediaItems = playlist.map { buildMediaItem(it) }

        doPlayWithController(mediaItems, startIndex)
    }

    private fun doPlayWithController(mediaItems: List<MediaItem>, startIndex: Int) {
        // Guardar estados actuales de shuffle/repeat antes de setMediaItems
        val currentShuffle = _isShuffleEnabled.value
        val currentRepeat = _isRepeatEnabled.value

        // Try to get the controller directly if it's ready
        mediaController?.let { controller ->
            controller.setMediaItems(mediaItems, startIndex, 0L)
            // Restaurar estados
            if (currentShuffle) {
                controller.shuffleModeEnabled = true
                controller.repeatMode = Player.REPEAT_MODE_ALL
            } else if (currentRepeat) {
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_ALL
            } else {
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_OFF
            }
            controller.prepare()
            controller.play()
            return
        }

        // If not ready, wait for it
        val future = controllerFuture ?: return


        if (future.isDone) {
            try {
                mediaController = future.get()
                doPlayWithController(mediaItems, startIndex)
            } catch (error: Exception) {
                Log.e(TAG, "Unable to prepare playlist", error)
            }
            return
        }

        // Wait for the future to complete, then retry
        future.addListener({
            try {
                mediaController = future.get()
                doPlayWithController(mediaItems, startIndex)
            } catch (error: Exception) {
                Log.e(TAG, "Unable to connect MediaController for playlist", error)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun getStreamUrl(track: Track): String {
        // SimpleCache handles caching automatically via CacheDataSource.Factory
        // ExoPlayer will intercept streaming and cache data on-the-fly
        return credentialsManager.getStreamUrl(track.id)
    }

    /**
     * Construye un [MediaItem] para el MediaController a partir de un [Track].
     */
    private fun buildMediaItem(track: Track): MediaItem {
        val streamUrl = getStreamUrl(track)
        val coverUrl = track.coverArtId?.let { credentialsManager.getCoverArtUrl(it, 400) }
        return MediaItem.Builder().setMediaId(track.id).setUri(streamUrl)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.artistName).setAlbumTitle(track.albumName)
                .apply { coverUrl?.let { setArtworkUri(it.toUri()) } }.build()).build()
    }

    /**
     * Inserta tracks en la playlist en el índice especificado.
     * Actualiza TANTO el StateFlow local como la lista interna del MediaController.
     */
    fun insertIntoPlaylist(index: Int, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val currentPlaylist = _playlist.value.toMutableList()
        currentPlaylist.addAll(index, tracks)
        _playlist.value = currentPlaylist
        isLoadingMoreCallbackInvoked = false

        val mediaItems = tracks.map { buildMediaItem(it) }

        mediaController?.let { controller ->
            controller.addMediaItems(index, mediaItems)
            return
        }

        val future = controllerFuture
        if (future == null || !future.isDone) return
        try {
            mediaController = future.get()
            mediaController?.addMediaItems(index, mediaItems)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to insert tracks", error)
        }
    }

    /**
     * Agrega tracks al final de la playlist.
     * Actualiza TANTO el StateFlow local como la lista interna del MediaController.
     */
    fun addToPlaylist(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        _playlist.value += tracks

        val mediaItems = tracks.map { buildMediaItem(it) }

        mediaController?.let { controller ->
            controller.addMediaItems(mediaItems)
            return
        }

        val future = controllerFuture
        if (future == null || !future.isDone) return
        try {
            mediaController = future.get()
            mediaController?.addMediaItems(mediaItems)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to add tracks", error)
        }
    }

    fun appendToPlaylist(tracks: List<Track>) {
        Log.d(TAG, "appendToPlaylist: received ${tracks.size} tracks")
        if (tracks.isEmpty()) {
            isLoadingMoreCallbackInvoked = false
            return
        }
        _playlist.value += tracks
        val mediaItems = tracks.map { buildMediaItem(it) }

        try {
            val controller = mediaController ?: run {
                val future = controllerFuture
                if (future == null || !future.isDone) return
                mediaController = future.get()
                mediaController ?: return
            }

            controller.addMediaItems(mediaItems)
            isLoadingMoreCallbackInvoked = false
            Log.d(TAG, "appendToPlaylist: appended ${tracks.size} tracks, total=${controller.mediaItemCount}")
            if (controller.playbackState == Player.STATE_ENDED ||
                controller.playbackState == Player.STATE_IDLE
            ) {
                controller.prepare()
                controller.play()
            }
        } catch (error: Exception) {
            Log.e(TAG, "Unable to append ${tracks.size} tracks", error)
        } finally {
            isLoadingMoreCallbackInvoked = false
        }
    }

    fun togglePlayPause() { mediaController?.let { if (it.isPlaying) it.pause() else it.play() } }

    fun toggleShuffle() {
        if (!_isShuffleEnabled.value) {
            // Activar shuffle
            _isRepeatEnabled.value = false
            mediaController?.let {
                it.shuffleModeEnabled = true
                it.repeatMode = Player.REPEAT_MODE_ALL
            }
            _isShuffleEnabled.value = true
        } else {
            // Desactivar shuffle
            mediaController?.let {
                it.shuffleModeEnabled = false
                it.repeatMode = Player.REPEAT_MODE_OFF
            }
            _isShuffleEnabled.value = false
        }
    }

    fun toggleRepeat() {
        if (!_isRepeatEnabled.value) {
            // Activar repeat
            _isShuffleEnabled.value = false
            mediaController?.let {
                it.shuffleModeEnabled = false
                it.repeatMode = Player.REPEAT_MODE_ALL
            }
            _isRepeatEnabled.value = true
        } else {
            // Desactivar repeat
            mediaController?.let {
                it.repeatMode = Player.REPEAT_MODE_OFF
            }
            _isRepeatEnabled.value = false
        }
    }
    fun seekTo(positionMs: Long) { mediaController?.seekTo(positionMs); _currentPosition.value = positionMs }

    fun next(): Boolean {
        val controller = mediaController ?: return false
        return controller.hasNextMediaItem().also { hasNext ->
            if (hasNext) controller.seekToNextMediaItem()
        }
    }

    fun previous(): Boolean {
        val controller = mediaController ?: return false
        return controller.hasPreviousMediaItem().also { hasPrevious ->
            if (hasPrevious) controller.seekToPreviousMediaItem()
        }
    }

    private fun handlePlaybackEnded() {
        val hasNext = mediaController?.hasNextMediaItem() == true
        val shuffle = _isShuffleEnabled.value
        val repeat = _isRepeatEnabled.value
        val canLoadMore = !hasNext && !shuffle && !repeat
        Log.d(TAG, "handlePlaybackEnded: hasNext=$hasNext canLoadMore=$canLoadMore")

        if (canLoadMore) {
            loadMoreCallback?.let { callback ->
                isLoadingMoreCallbackInvoked = true
                callback()
            }
        }
    }

    fun removeFromPlaylist(index: Int) {
        val currentList = _playlist.value.toMutableList()
        if (index !in currentList.indices) return
        
        currentList.removeAt(index)
        _playlist.value = currentList
        
        // If playlist is empty, stop playback
        if (currentList.isEmpty()) {
            mediaController?.stop()
            _currentIndex.value = 0
            _currentTrack.value = null
            _currentTrackId.value = null
            _isPlaying.value = false
            return
        }
        
        // Remove from MediaController
        mediaController?.removeMediaItem(index)
        
        // Adjust current index if needed
        val currentIdx = _currentIndex.value
        if (index < currentIdx) {
            _currentIndex.value = currentIdx - 1
        } else if (index == currentIdx) {
            // MediaController handles transition, but we need to ensure consistency
            val newIdx = currentIdx.coerceAtMost(currentList.size - 1)
            _currentIndex.value = newIdx
            _currentTrack.value = currentList.getOrNull(newIdx)
            _currentTrackId.value = currentList.getOrNull(newIdx)?.id
        }
    }

    fun setPlaylist(playlist: List<Track>) { _playlist.value = playlist }

    fun setLoadMoreCallback(callback: () -> Unit) { loadMoreCallback = callback }

    private companion object {
        const val TAG = "AudioPlayerManager"
    }

}
