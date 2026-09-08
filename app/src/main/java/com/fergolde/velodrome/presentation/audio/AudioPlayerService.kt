package com.fergolde.velodrome.presentation.audio

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.fergolde.velodrome.MainActivity
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.queue.QueueSnapshot
import com.fergolde.velodrome.data.local.queue.QueueSnapshotStore
import com.fergolde.velodrome.data.local.queue.toDomain
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.util.CacheManager
import com.fergolde.velodrome.util.CredentialsManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.flow.first
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Servicio en primer plano para la reproducción de audio.
 * Se han ELIMINADO todas las llamadas estáticas a AudioPlayerManager para evitar fugas de memoria.
 */
@UnstableApi
@AndroidEntryPoint
class AudioPlayerService : MediaSessionService() {

    @Inject
    lateinit var cacheDataSourceFactory: CacheDataSource.Factory

    @Inject
    lateinit var scrobbleManager: ScrobbleManager

    @Inject
    lateinit var trackDao: TrackDao

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var cacheManager: CacheManager

    @Inject
    lateinit var queueSnapshotStore: QueueSnapshotStore

    @Inject
    lateinit var credentialsManager: CredentialsManager

    private var equalizerEngine: EqualizerEngine? = null

    @Volatile
    private var eqEnabled = false

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private val precacheJobs = ConcurrentHashMap<String, Job>()

    // Track currently being played (used to scrobble the previous track on transition)
    private var currentTrackId: String? = null
    private var currentDuration: Long = 0L


    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(cacheDataSourceFactory)
            )
            .build()

        exoPlayer?.addAnalyticsListener(analyticsListener)
        exoPlayer?.addListener(playerListener)

        setupEqualizer()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionActivity(pendingIntent)
            .setId(SESSION_ID)
            .setCallback(sessionCallback)
            .build()

        restoreQueueForExternalControl()
    }

    /**
     * Attaches the session audio effects (genre auto-preset EQ + optional bass
     * boost) to ExoPlayer's audio session and keeps them in sync with settings.
     * Best-effort: devices without effect support just stay silent.
     */
    private fun setupEqualizer() {
        val player = exoPlayer ?: return
        val engine = EqualizerEngine(player.audioSessionId)
        equalizerEngine = engine

        // lifecycleScope runs on Main: ExoPlayer must only be touched on its
        // creation thread. DataStore does its own I/O off-thread, so collecting
        // here costs nothing.
        lifecycleScope.launch {
            settingsRepository.eqEnabled.collect { enabled ->
                eqEnabled = enabled
                engine.setEnabled(enabled)
                if (enabled) {
                    applyPresetForCurrentTrack()
                }
            }
        }
        lifecycleScope.launch {
            settingsRepository.bassBoostEnabled.collect { enabled ->
                engine.setBassBoostEnabled(enabled)
            }
        }
    }

    private fun applyPresetForCurrentTrack() {
        // Main thread: capture the current item id before hopping to the IO
        // scope for the Room genre lookup.
        val trackId = exoPlayer?.currentMediaItem?.mediaId ?: return
        serviceScope.launch {
            equalizerEngine?.applyGenrePreset(genreForTrack(trackId))
        }
    }

    private suspend fun genreForTrack(trackId: String): String? =
        trackDao.findGenreByTrackId(trackId)

    // El sistema llama a este método cuando un MediaController (como el de AudioPlayerManager) intenta conectarse
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * Callback explícito: acepta TODO controlador —incluidos los no confiables
     * como apps compañeras de reloj (Garmin Connect)— con el set completo de
     * comandos. Sin esto, un controlador externo puede recibir una máscara sin
     * pause/next/prev según el estado transitorio del player, mientras los
     * botones del sistema (auriculares BT) siguen funcionando.
     */
    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            Log.i(
                TAG,
                "Controller connect package=${controller.packageName} " +
                    "trusted=${controller.isTrusted} playerCommands=${exoPlayer?.availableCommands?.size()}"
            )
            // AcceptedResultBuilder(session) parte de los comandos de sesión por
            // defecto; luego se abre el set de player al completo. El set final
            // se intersecta con lo que ExoPlayer expone, así que solo amplía.
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(Player.Commands.Builder().addAllCommands().build())
                .build()
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            command: Int
        ): Int {
            Log.i(TAG, "External command=$command from=${controller.packageName}")
            // El stub interpreta este retorno como SessionResult.Code: solo
            // RESULT_SUCCESS ejecuta el comando. Retornar el código player
            // (pattern antiguo) rechaza TODO y deja la app muda.
            return SessionResult.RESULT_SUCCESS
        }

        /**
         * Resumption en frío: el sistema (o un reloj) pide reproducir cuando el
         * proceso nació sin cola. Restaura el snapshot antes de responder para
         * que play/next/prev tengan sobre qué actuar.
         */
        override fun onPlaybackResumption(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            Log.i(TAG, "Playback resumption from=${controller.packageName}")
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                try {
                    val items = withContext(Dispatchers.Main) {
                        val player = exoPlayer
                        if (player != null && player.mediaItemCount > 0) {
                            (0 until player.mediaItemCount).map { player.getMediaItemAt(it) } to
                                (player.currentMediaItemIndex to player.currentPosition)
                        } else {
                            null
                        }
                    }
                    if (items != null) {
                        val (mediaItems, indexToPos) = items
                        future.set(
                            MediaSession.MediaItemsWithStartPosition(
                                mediaItems, indexToPos.first, indexToPos.second
                            )
                        )
                        return@launch
                    }
                    val restored = loadResumptionMediaItems()
                    if (restored != null) {
                        withContext(Dispatchers.Main) {
                            val player = exoPlayer
                            if (player != null && player.mediaItemCount == 0) {
                                player.setMediaItems(
                                    restored.items, restored.startIndex, restored.startPositionMs
                                )
                                player.prepare()
                            }
                        }
                        future.set(
                            MediaSession.MediaItemsWithStartPosition(
                                restored.items, restored.startIndex, restored.startPositionMs
                            )
                        )
                    } else {
                        future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0))
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w(TAG, "Resumption failed", error)
                    future.setException(error)
                }
            }
            return future
        }
    }

    /**
     * Materializa la cola persistida al nacer el servicio (preparada, en pausa)
     * para que controles externos funcionen sin abrir antes la UI. El manager
     * reusa el mismo snapshot al primer toque, sin cambios visibles.
     */
    private fun restoreQueueForExternalControl() {
        lifecycleScope.launch {
            val player = exoPlayer ?: return@launch
            if (player.mediaItemCount > 0) return@launch
            val restored = withContext(Dispatchers.IO) { loadResumptionMediaItems() } ?: return@launch
            if (player.mediaItemCount == 0) {
                player.setMediaItems(restored.items, restored.startIndex, restored.startPositionMs)
                player.prepare()
            }
        }
    }

    private suspend fun loadResumptionMediaItems(): ResumptionMediaItems? {
        val snapshot = queueSnapshotStore.load() ?: return null
        val plan = computeResumptionPlan(snapshot, playerHasItems = false) ?: return null
        val items = plan.tracks.map { buildMediaItem(it) }
        if (items.isEmpty()) return null
        return ResumptionMediaItems(items, plan.startIndex, plan.startPositionMs)
    }

    private fun buildMediaItem(track: Track): MediaItem {
        val streamUrl = credentialsManager.getStreamUrl(track.id)
        val coverUrl = track.coverArtId?.let { credentialsManager.getCoverArtUrl(it, 400) }
        return MediaItem.Builder().setMediaId(track.id).setUri(streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(track.title).setArtist(track.artistName)
                    .setAlbumTitle(track.albumName)
                    .apply { coverUrl?.let { setArtworkUri(it.toUri()) } }.build()
            ).build()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        precacheJobs.clear()
        serviceJob.cancel()
        equalizerEngine?.release()
        equalizerEngine = null
        super.onDestroy()
    }

    /**
     * Listener interno del reproductor.
     * NOTA: Se han eliminado las llamadas a AudioPlayerManager.onXXX.
     * El AudioPlayerManager recibirá estas actualizaciones automáticamente a través de su MediaController.
     */
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val previousTrackId = currentTrackId
            val previousDuration = currentDuration

            mediaItem?.let {
                currentTrackId = it.mediaId
                currentDuration = exoPlayer?.duration?.takeIf { d -> d > 0 } ?: 0L
                if (eqEnabled) applyPresetForCurrentTrack()
            }

            // Fin natural de pista (auto-avance o repeat-one): marcar la anterior
            // como reproducida. Un salto manual (SEEK) no acredita la anterior.
            if ((reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) &&
                previousTrackId != null && previousDuration > 0
            ) {
                scrobbleManager.markTrackPlayed(previousTrackId)
            }

            precacheNextTrack()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            precacheNextTrack()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    // Duración real disponible solo al estar listo para reproducir.
                    exoPlayer?.duration?.takeIf { it > 0 }?.let { currentDuration = it }
                }
                Player.STATE_ENDED -> {
                    // Playlist terminada sin transición posterior: marcar la última.
                    val trackId = currentTrackId
                    if (trackId != null && currentDuration > 0) {
                        scrobbleManager.markTrackPlayed(trackId)
                    }
                }
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val trackId = exoPlayer?.currentMediaItem?.mediaId ?: "unknown"
            Log.e(TAG, "Playback failed track=$trackId code=${error.errorCodeName}", error)
        }
    }

    private fun precacheNextTrack() {
        val player = exoPlayer ?: return
        val nextIndex = player.currentMediaItemIndex + 1
        if (nextIndex !in 0 until player.mediaItemCount) return

        val mediaItem = player.getMediaItemAt(nextIndex)
        val uri = mediaItem.localConfiguration?.uri ?: return
        val trackId = mediaItem.mediaId
        if (precacheJobs[trackId]?.isActive == true) return

        val job = serviceScope.launch {
            try {
                // Skip tracks that are already fully cached: CacheWriter would
                // resolve spans and download nothing, pure churn per event.
                val expectedSize = trackDao.getTrackById(trackId)?.sizeBytes ?: 0L
                if (cacheManager.isTrackFullyCached(trackId, expectedSize)) return@launch
                // Near the quota a new download would only evict other tracks'
                // LRU spans for marginal benefit.
                if (cacheManager.isAtOrAboveQuota(PRECACHE_QUOTA_FRACTION)) return@launch
                repeat(PRECACHE_ATTEMPTS) { attempt ->
                    try {
                        val dataSource = cacheDataSourceFactory.createDataSourceForDownloading()
                        val writer = CacheWriter(
                            dataSource,
                            DataSpec.Builder().setUri(uri).build(),
                            null,
                            null
                        )
                        writer.cache()
                        return@launch
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        if (attempt == PRECACHE_ATTEMPTS - 1) {
                            throw error
                        }
                        delay(PRECACHE_RETRY_DELAY_MS * (attempt + 1))
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Precache failed track=$trackId", error)
            } finally {
                precacheJobs.remove(trackId)
            }
        }
        precacheJobs[trackId] = job
    }

    /**
     * AnalyticsListener para notificar "now playing" al scrobbler.
     * Room is intentionally NOT written here: synced library tracks already
     * live in the tracks table with real album/genre data, and server-only
     * tracks (radio top-ups, search) stream fine without a row — a metadata
     * placeholder here used to pollute the table with albumId="" rows and
     * full-URL coverArtIds that broke the EQ genre lookup and artwork.
     */
    private val analyticsListener = object : AnalyticsListener {
        override fun onMediaItemTransition(
            eventTime: AnalyticsListener.EventTime,
            mediaItem: MediaItem?,
            reason: Int
        ) {
            mediaItem?.let {
                val trackId = it.mediaId
                scrobbleManager.onTrackChanged()
                scrobbleManager.sendNowPlaying(trackId)
            }
        }
    }

    companion object {
        private const val TAG = "AudioPlayerService"
        private const val SESSION_ID = "velodrome-playback"
        private const val PRECACHE_ATTEMPTS = 3
        private const val PRECACHE_RETRY_DELAY_MS = 500L
        private const val PRECACHE_QUOTA_FRACTION = 0.9
    }
}

/** Plan puro de resumption: sin tipos Media3 para seguir testeable en JVM. */
internal data class ResumptionPlan(
    val tracks: List<Track>,
    val startIndex: Int,
    val startPositionMs: Long
)

internal data class ResumptionMediaItems(
    val items: List<MediaItem>,
    val startIndex: Int,
    val startPositionMs: Long
)

/**
 * Decide desde qué snapshot retomar. Null = nada que restaurar (el player ya
 * tiene cola o no hay snapshot útil); el llamador deja todo como está.
 */
internal fun computeResumptionPlan(snapshot: QueueSnapshot?, playerHasItems: Boolean): ResumptionPlan? {
    if (playerHasItems) return null
    if (snapshot == null || snapshot.tracks.isEmpty()) return null
    val tracks = snapshot.tracks.map { it.toDomain() }
    val index = snapshot.currentIndex.coerceIn(0, tracks.lastIndex)
    return ResumptionPlan(tracks, index, snapshot.positionMs.coerceAtLeast(0L))
}
