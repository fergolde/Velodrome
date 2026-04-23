package com.example.velodrome.presentation.audio

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.velodrome.MainActivity
import dagger.hilt.android.AndroidEntryPoint
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

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    private val TAG = "AudioPlayerService"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AudioPlayerService onCreate")

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // Configuramos ExoPlayer con soporte nativo para SimpleCache
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
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
        Log.d(TAG, "AudioPlayerService onDestroy")
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }

    /**
     * Listener interno del reproductor.
     * NOTA: Se han eliminado las llamadas a AudioPlayerManager.onXXX.
     * El AudioPlayerManager recibirá estas actualizaciones automáticamente a través de su MediaController.
     */
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "Playback state: isPlaying = $isPlaying")
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> Log.d(TAG, "Player ready, duration: ${exoPlayer?.duration}")
                Player.STATE_BUFFERING -> Log.d(TAG, "Player buffering...")
                Player.STATE_ENDED -> Log.d(TAG, "Playlist ended")
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
                scrobbleManager.onTrackChanged(trackId)
                scrobbleManager.sendNowPlaying(trackId)
                Log.d(TAG, "Track changed: ${it.mediaMetadata.title}")
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