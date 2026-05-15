package com.example.velodrome.presentation.audio

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.velodrome.MainActivity
import com.example.velodrome.data.local.dao.TrackDao
import com.example.velodrome.data.local.entity.TrackEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null


    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            // Configuramos el buffer máximo a 20 minutos (1.200.000 ms)
            // ExoPlayer no parará de descargar hasta tener 20 mins cacheados.
            // Esto significa que bajará la canción actual entera en segundos.
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                1200000, // 20 minutos
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()

        // Configuramos ExoPlayer con soporte nativo para SimpleCache
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(cacheDataSourceFactory)
            )
            .build()

        // Escuchas para scrobbling y logs (sin llamadas estáticas)
        exoPlayer?.addAnalyticsListener(analyticsListener)
        exoPlayer?.addListener(playerListener)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionActivity(pendingIntent)
            .build()
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
        serviceJob.cancel()
        super.onDestroy()
    }

    /**
     * Listener interno del reproductor.
     * NOTA: Se han eliminado las llamadas a AudioPlayerManager.onXXX.
     * El AudioPlayerManager recibirá estas actualizaciones automáticamente a través de su MediaController.
     */
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                else -> Unit
            }
        }
    }

    /**
     * AnalyticsListener exclusivo para lógica de negocio de Scrobbling (Last.fm / Navidrome).
     */
    private val analyticsListener = object : AnalyticsListener {
        private val scrobbledTracks = mutableSetOf<String>()

        override fun onPositionDiscontinuity(
            eventTime: AnalyticsListener.EventTime,
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            checkScrobble(newPosition.positionMs)
        }

        override fun onMediaItemTransition(
            eventTime: AnalyticsListener.EventTime,
            mediaItem: MediaItem?,
            reason: Int
        ) {
            mediaItem?.let {
                val trackId = it.mediaId
                scrobbledTracks.remove(trackId)
                scrobbleManager.onTrackChanged()
                scrobbleManager.sendNowPlaying(trackId)

                // Guardar en Room para disponibilidad offline
                val meta = it.mediaMetadata
                serviceScope.launch {
                    val existing = trackDao.getTrackById(trackId)
                    if (existing == null) {
                        trackDao.insertTrack(
                            TrackEntity(
                                id = trackId,
                                albumId = "",
                                title = meta.title?.toString() ?: "Unknown",
                                artistName = meta.artist?.toString() ?: "",
                                albumName = meta.albumTitle?.toString() ?: "",
                                durationSec = 0,
                                trackNumber = 0,
                                coverArtId = meta.artworkUri?.toString(),
                                sizeBytes = 0L
                            )
                        )
                    }
                }
            }
        }

        private fun checkScrobble(currentPositionMs: Long) {
            val duration = exoPlayer?.duration ?: 0L
            val trackId = exoPlayer?.currentMediaItem?.mediaId ?: return

            if (duration > 0) {
                val halfwayPoint = duration / 2
                if (currentPositionMs >= halfwayPoint && trackId !in scrobbledTracks) {
                    scrobbledTracks.add(trackId)
                    scrobbleManager.checkAndScrobble(trackId, currentPositionMs, duration)
                }
            }
        }
    }
}