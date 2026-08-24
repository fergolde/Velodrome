package com.fergolde.velodrome.presentation.audio

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
import com.fergolde.velodrome.MainActivity
import com.fergolde.velodrome.data.local.dao.AlbumDao
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.domain.repository.SettingsRepository
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
    lateinit var albumDao: AlbumDao

    @Inject
    lateinit var settingsRepository: SettingsRepository

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
            .build()
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

    private suspend fun genreForTrack(trackId: String): String? {
        val albumId = trackDao.getTrackById(trackId)?.albumId ?: return null
        return albumDao.getAlbumById(albumId)?.genre
    }

    // El sistema llama a este método cuando un MediaController (como el de AudioPlayerManager) intenta conectarse
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
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
                        Log.d(TAG, "Precached track=$trackId")
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
        private const val PRECACHE_ATTEMPTS = 3
        private const val PRECACHE_RETRY_DELAY_MS = 500L
    }
}
