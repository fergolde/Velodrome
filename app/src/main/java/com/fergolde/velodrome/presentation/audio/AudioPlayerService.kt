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
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.entity.TrackEntity
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
        precacheJobs.clear()
        serviceJob.cancel()
        super.onDestroy()
    }

    /**
     * Listener interno del reproductor.
     * NOTA: Se han eliminado las llamadas a AudioPlayerManager.onXXX.
     * El AudioPlayerManager recibirá estas actualizaciones automáticamente a través de su MediaController.
     */
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                currentTrackId = it.mediaId
                currentDuration = exoPlayer?.duration ?: 0L
            }
            precacheNextTrack()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            precacheNextTrack()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            // Scrobble the previous track when the user changes to another song.
            if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex) {
                scrobbleCurrentTrack(oldPosition.positionMs)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // Playlist ended: scrobble the last track as fully played.
                scrobbleCurrentTrack(currentDuration)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val trackId = exoPlayer?.currentMediaItem?.mediaId ?: "unknown"
            Log.e(TAG, "Playback failed track=$trackId code=${error.errorCodeName}", error)
        }
    }

    private fun scrobbleCurrentTrack(playedMs: Long) {
        val trackId = currentTrackId ?: return
        val duration = currentDuration
        if (duration > 0) {
            scrobbleManager.checkAndScrobble(trackId, playedMs, duration)
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
     * AnalyticsListener para notificar "now playing" y persistir tracks en Room.
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
    }

    companion object {
        private const val TAG = "AudioPlayerService"
        private const val PRECACHE_ATTEMPTS = 3
        private const val PRECACHE_RETRY_DELAY_MS = 500L
    }
}
