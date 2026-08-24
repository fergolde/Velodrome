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
import com.fergolde.velodrome.data.local.queue.QueueSnapshot
import com.fergolde.velodrome.data.local.queue.QueueSnapshotStore
import com.fergolde.velodrome.data.local.queue.toDto
import com.fergolde.velodrome.data.local.queue.toDomain
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.util.CredentialsManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manager for audio playback with MediaController.
 * Injected by Hilt - singleton ensures single instance.
 */
/**
 * Pure index math for queue reordering: where does the current item's index
 * land after moving [from] -> [to]? Top-level so it stays unit-testable
 * without instantiating AudioPlayerManager (its constructor connects a real
 * MediaController session).
 */
internal fun adjustedCurrentIndex(current: Int, from: Int, to: Int): Int = when {
    current == from -> to
    from < current && to >= current -> current - 1
    from > current && to <= current -> current + 1
    else -> current
}

@Singleton
class AudioPlayerManager @OptIn(UnstableApi::class)
@Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val credentialsManager: CredentialsManager,
    private val queueSnapshotStore: QueueSnapshotStore,
) {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()
    private val _currentTrackId = MutableStateFlow<String?>(null)
    val currentTrackId: StateFlow<String?> = _currentTrackId.asStateFlow()

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

    private val playerScope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var loadMoreCallback: (() -> Unit)? = null
    private val retryAttempts = mutableMapOf<String, Int>()

    @Volatile
    private var persistDirty = false
    private var persistJob: Job? = null

    /**
     * Snapshot loaded at cold start. While non-null the UI shows the restored
     * queue (paused) but MediaController has no items yet; the first playback
     * interaction rebuilds it via [consumePendingRestore].
     */
    private var pendingRestore: QueueSnapshot? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
                restoreQueueMetadata()
            } catch (error: Exception) {
                Log.e(TAG, "Unable to connect MediaController", error)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                // Pausing is the best moment to capture an exact position.
                if (!isPlaying) persistQueue()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> { checkIfNeedMoreSongs() }
                    Player.STATE_ENDED -> { _isPlaying.value = false; handlePlaybackEnded() }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let {
                    retryAttempts.remove(it.mediaId)
                    updateCurrentTrackFromMediaItem(it)
                    persistQueue()
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
     * Position polling. Writes _currentPosition only while at least one collector
     * is subscribed (MiniPlayer / PlayerScreen visible). When nobody listens —
     * hidden player or backgrounded app — the loop parks and burns zero CPU.
     * seekTo() still writes directly for instant slider feedback.
     */
    private fun startPositionPolling() {
        playerScope.launch {
            _currentPosition.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { hasSubscribers ->
                    if (hasSubscribers) {
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

        // Si quedan menos de 3, no hay shuffle/repeat activo, disparar callback
        if (remaining <= 3 && !shuffle && !repeat) {
            loadMoreCallback?.let { callback ->
                callback()
            }
        }
    }

    private fun updateCurrentTrackFromMediaItem(mediaItem: MediaItem) {
        val playlist = _playlist.value
        val index = playlist.indexOfFirst { it.id == mediaItem.mediaId }
        if (index in playlist.indices) {
            _currentIndex.value = index
            _currentTrack.value = playlist[index]
            _currentTrackId.value = playlist[index].id
        }
    }

    // ─── Queue persistence (local, event-driven, debounced) ─────────────────

    /**
     * Schedules a queue snapshot write. Rapid bursts (transition -> pause ->
     * seek callbacks) coalesce into a single write ~300ms after the last event,
     * and JSON serialization + disk I/O run on IO so the main thread only
     * flips a flag. The snapshot is built AFTER the quiet window from the
     * current state, so it is never stale.
     */
    private fun persistQueue() {
        persistDirty = true
        if (persistJob?.isActive == true) return
        persistJob = playerScope.launch {
            while (persistDirty) {
                persistDirty = false
                delay(PERSIST_DEBOUNCE_MS.milliseconds)
            }
            withContext(Dispatchers.IO) {
                val tracks = _playlist.value
                if (tracks.isNotEmpty()) {
                    queueSnapshotStore.save(
                        QueueSnapshot(
                            tracks = tracks.map { it.toDto() },
                            currentIndex = _currentIndex.value.coerceIn(0, tracks.lastIndex),
                            positionMs = _currentPosition.value
                        )
                    )
                }
            }
        }
    }

    /** Cancels any pending debounced write so it cannot resurrect a cleared queue. */
    private fun clearPersistedQueue() {
        persistDirty = false
        persistJob?.cancel()
        playerScope.launch { queueSnapshotStore.clear() }
    }

    /** Hydrates the UI states from the stored snapshot; MediaController stays untouched. */
    private fun restoreQueueMetadata() {
        playerScope.launch {
            val snapshot = queueSnapshotStore.load() ?: return@launch
            if (snapshot.tracks.isEmpty()) return@launch
            pendingRestore = snapshot
            val index = snapshot.currentIndex.coerceIn(0, snapshot.tracks.lastIndex)
            _playlist.value = snapshot.tracks.map { it.toDomain() }
            _currentIndex.value = index
            _currentTrack.value = snapshot.tracks.getOrNull(index)?.toDomain()
            _currentTrackId.value = _currentTrack.value?.id
        }
    }

    /**
     * First playback interaction after a cold start: materialize the restored
     * queue inside MediaController at the saved index/position, prepared but
     * paused. Returns true if a restore was consumed.
     */
    private fun consumePendingRestore(): Boolean {
        val snapshot = pendingRestore ?: return false
        pendingRestore = null
        val controller = mediaController ?: return false
        val items = snapshot.tracks.map { buildMediaItem(it.toDomain()) }
        val startIndex = snapshot.currentIndex.coerceIn(0, items.lastIndex)
        controller.setMediaItems(items, startIndex, snapshot.positionMs.coerceAtLeast(0L))
        controller.prepare()
        return true
    }

    fun playTrack(track: Track, playlist: List<Track>, startIndex: Int = 0) {
        pendingRestore = null
        _playlist.value = playlist
        // No actualizamos currentIndex/currentTrack aquí - el listener onMediaItemTransition 
        // del MediaController es la única fuente de verdad

        val mediaItems = playlist.map { buildMediaItem(it) }

        doPlayWithController(mediaItems, startIndex)
        persistQueue()
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
        consumePendingRestore()
        val currentPlaylist = _playlist.value.toMutableList()
        currentPlaylist.addAll(index, tracks)
        _playlist.value = currentPlaylist

        val mediaItems = tracks.map { buildMediaItem(it) }

        persistQueue()
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
        consumePendingRestore()
        _playlist.value += tracks

        val mediaItems = tracks.map { buildMediaItem(it) }

        persistQueue()
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
        if (tracks.isEmpty()) return
        consumePendingRestore()
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
            persistQueue()
            if (controller.playbackState == Player.STATE_ENDED ||
                controller.playbackState == Player.STATE_IDLE
            ) {
                controller.prepare()
                controller.play()
            }
        } catch (error: Exception) {
            Log.e(TAG, "Unable to append ${tracks.size} tracks", error)
        }
    }

    fun togglePlayPause() {
        // First interaction after cold start: materialize the restored queue,
        // prepared at the saved index/position, then resume playback.
        if (pendingRestore != null) {
            consumePendingRestore()
            mediaController?.play()
            return
        }
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

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
    fun seekTo(positionMs: Long) {
        consumePendingRestore()
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
        persistQueue()
    }

    fun next(): Boolean {
        consumePendingRestore()
        val controller = mediaController ?: return false
        return controller.hasNextMediaItem().also { hasNext ->
            if (hasNext) controller.seekToNextMediaItem()
        }
    }

    fun previous(): Boolean {
        consumePendingRestore()
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

        if (canLoadMore) {
            loadMoreCallback?.let { callback ->
                callback()
            }
        }
    }

    fun removeFromPlaylist(index: Int) {
        consumePendingRestore()
        val currentList = _playlist.value.toMutableList()
        if (index !in currentList.indices) return

        currentList.removeAt(index)
        _playlist.value = currentList

        // If playlist is empty, stop playback and forget the persisted queue
        if (currentList.isEmpty()) {
            mediaController?.stop()
            _currentIndex.value = 0
            _currentTrack.value = null
            _currentTrackId.value = null
            _isPlaying.value = false
            pendingRestore = null
            clearPersistedQueue()
            return
        }
        persistQueue()
        
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

    fun setPlaylist(playlist: List<Track>) {
        pendingRestore = null
        _playlist.value = playlist
    }

    /**
     * Reorders the queue moving item at [from] to [to]. Playback continues
     * through the move (Media3 keeps the current item playing). The mirrored
     * index is adjusted via [adjustedCurrentIndex]; MediaController tracks its
     * own current position internally.
     */
    fun moveInPlaylist(from: Int, to: Int) {
        consumePendingRestore()
        val currentList = _playlist.value.toMutableList()
        if (from !in currentList.indices || to !in currentList.indices || from == to) return

        val moved = currentList.removeAt(from)
        currentList.add(to, moved)
        _playlist.value = currentList

        _currentIndex.value = adjustedCurrentIndex(_currentIndex.value, from, to)

        persistQueue()
        mediaController?.moveMediaItem(from, to)
    }

    fun setLoadMoreCallback(callback: () -> Unit) { loadMoreCallback = callback }

    private companion object {
        const val TAG = "AudioPlayerManager"
        const val PERSIST_DEBOUNCE_MS = 300L
    }

}
