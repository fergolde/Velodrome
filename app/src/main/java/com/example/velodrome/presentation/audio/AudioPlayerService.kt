package com.example.velodrome.presentation.audio

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.velodrome.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service for real audio playback using Media3 ExoPlayer.
 * Uses SimpleCache for progressive caching of audio streams.
 */
@UnstableApi
@AndroidEntryPoint
class AudioPlayerService : MediaSessionService() {

    @Inject
    lateinit var simpleCache: SimpleCache

    @Inject
    lateinit var cacheDataSourceFactory: CacheDataSource.Factory

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AudioPlayerService onCreate")

        // Build ExoPlayer with CacheDataSource.Factory for progressive caching
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(cacheDataSourceFactory)
            )
            .build()

        // Create pending intent for media notification
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionActivity(pendingIntent)
            .build()

        // Register player listener
        exoPlayer?.addListener(playerListener)
    }

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
     * Prepare a track for playback (does not start playing)
     */
    fun prepareTrack(streamUrl: String, trackTitle: String, artistName: String, coverArtUrl: String?) {
        Log.d(TAG, "Preparing track: $trackTitle")

        val metadata = MediaMetadata.Builder()
            .setTitle(trackTitle)
            .setArtist(artistName)
            .apply {
                coverArtUrl?.let { setArtworkUri(android.net.Uri.parse(it)) }
            }
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()

        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
    }

    /**
     * Start or resume playback
     */
    fun play() {
        Log.d(TAG, "Play")
        exoPlayer?.play()
    }

    /**
     * Pause playback
     */
    fun pause() {
        Log.d(TAG, "Pause")
        exoPlayer?.pause()
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    /**
     * Seek to position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    /**
     * Skip to next track in playlist
     */
    fun next(): Boolean {
        return if (exoPlayer?.hasNextMediaItem() == true) {
            exoPlayer?.seekToNextMediaItem()
            true
        } else {
            false
        }
    }

    /**
     * Skip to previous track in playlist
     */
    fun previous(): Boolean {
        return if (exoPlayer?.hasPreviousMediaItem() == true) {
            exoPlayer?.seekToPreviousMediaItem()
            true
        } else {
            false
        }
    }

    /**
     * Get current playback position in milliseconds
     */
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    /**
     * Get current buffered position in milliseconds
     */
    fun getBufferedPosition(): Long {
        return exoPlayer?.bufferedPosition ?: 0L
    }

    /**
     * Get total duration in milliseconds
     */
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }

    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    /**
     * Set the playlist for continuous playback
     */
    fun setPlaylist(items: List<MediaItem>) {
        exoPlayer?.setMediaItems(items)
        exoPlayer?.prepare()
    }

    /**
     * Clear the current playlist
     */
    fun clearPlaylist() {
        exoPlayer?.clearMediaItems()
    }

    /**
     * Listener for player events
     */
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            AudioPlayerManager.onPlaybackStateChanged(isPlaying)
            if (isPlaying) {
                Log.d(TAG, "Player is playing")
            } else {
                Log.d(TAG, "Player stopped")
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    AudioPlayerManager.onReady(exoPlayer?.duration ?: 0L)
                    Log.d(TAG, "Player ready, duration: ${exoPlayer?.duration}")
                }
                Player.STATE_BUFFERING -> {
                    AudioPlayerManager.onBuffering()
                    Log.d(TAG, "Player buffering")
                }
                Player.STATE_ENDED -> {
                    AudioPlayerManager.onPlaybackCompleted()
                    Log.d(TAG, "Player ended")
                }
                Player.STATE_IDLE -> {
                    Log.d(TAG, "Player idle")
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                Log.d(TAG, "Media item changed: ${it.mediaMetadata.title}")
                AudioPlayerManager.onMediaItemChanged(it)
            }
        }
    }

    companion object {
        private const val TAG = "AudioPlayerService"
    }
}