package com.example.velodrome.data.repository

import android.util.Log
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.data.remote.dto.SongDto
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.repository.TrackRepository
import com.example.velodrome.util.CredentialsManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepositoryImpl @Inject constructor(
    private val api: NavidromeApi,
    private val credentialsManager: CredentialsManager
) : TrackRepository {

    override suspend fun getTracks(albumId: String): Result<List<Track>> {
        return runCatching {
            Log.d("VelodromeTracks", "getMusicDirectory albumId=$albumId")
            val response = api.getMusicDirectory(albumId)
            val songsList = response.response.directory?.child ?: emptyList()
            Log.d("VelodromeTracks", "Found ${songsList.size} songs")

            songsList.map { song ->
                Track(
                    id = song.id,
                    albumId = albumId,
                    title = song.title,
                    artistName = song.artist ?: "",
                    albumName = song.album ?: "",
                    durationSec = song.duration ?: 0,
                    sizeBytes = song.size ?: 0L,
                    bitrate = song.bitRate ?: 0,
                    trackNumber = song.track ?: 0,
                    coverArtId = song.coverArt
                )
            }
        }
    }

    override suspend fun getStreamUrl(trackId: String): String {
        val serverUrl = credentialsManager.getServerUrl() ?: ""
        val authParams = credentialsManager.generateAuthParams()

        return if (authParams != null) {
            val (username, token, salt) = authParams
            "${serverUrl}rest/stream.view?id=$trackId&u=$username&t=$token&s=$salt&v=1.16.1&c=Velodrome&maxBitRate=320"
        } else {
            "${serverUrl}rest/stream.view?id=$trackId&maxBitRate=320"
        }
    }

    private fun mapSongDto(dto: SongDto, albumId: String): Track {
        val effectiveCoverArtId = dto.coverArt ?: "al-$albumId"
        return Track(
            id = dto.id,
            albumId = albumId,
            title = dto.title,
            artistName = dto.artist ?: "",
            albumName = dto.album ?: albumId,
            durationSec = dto.duration ?: 0,
            sizeBytes = dto.size ?: 0L,
            bitrate = dto.bitRate ?: 0,
            trackNumber = dto.track ?: 0,
            isCached = false,
            coverArtId = effectiveCoverArtId
        )
    }

    override suspend fun getSongsByGenre(genre: String, count: Int, offset: Int): Result<List<Track>> {
        return runCatching {
            Log.d("TrackRepo", "getSongsByGenre: genre=$genre, count=$count, offset=$offset")
            val response = api.getSongsByGenre(genre, count, offset)
            val songDtos = response.response.songsByGenre?.song ?: emptyList()
            Log.d("TrackRepo", "Found ${songDtos.size} songs from genre $genre")
            songDtos.map { mapSongDto(it, it.albumId ?: genre) }
        }
    }

    override suspend fun getRandomSongsByGenre(genre: String, size: Int): Result<List<Track>> {
        return runCatching {
            Log.d("TrackRepo", "getRandomSongsByGenre: genre=$genre, size=$size")
            val response = api.getRandomSongs(size, genre)
            val songDtos = response.response.randomSongs?.song ?: emptyList()
            Log.d("TrackRepo", "Found ${songDtos.size} random songs from genre $genre")
            songDtos.map { mapSongDto(it, it.albumId ?: genre) }
        }
    }

    override suspend fun getRandomSongs(size: Int): Result<List<Track>> {
        return runCatching {
            Log.d("TrackRepo", "getRandomSongs: size=$size")
            val response = api.getRandomSongs(size, null)
            Log.d("TrackRepo", "getRandomSongs: response=$response")
            val songDtos = response.response.randomSongs?.song ?: emptyList()
            Log.d("TrackRepo", "Found ${songDtos.size} random songs")
            songDtos.map { mapSongDto(it, it.albumId ?: "") }
        }
    }
}