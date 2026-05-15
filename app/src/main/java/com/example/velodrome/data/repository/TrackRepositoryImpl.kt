package com.example.velodrome.data.repository

import android.util.Log
import com.example.velodrome.data.local.dao.TrackDao
import com.example.velodrome.data.local.mapper.toDomain
import com.example.velodrome.data.local.mapper.toEntity
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.data.remote.dto.SongDto
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.repository.TrackRepository
import com.example.velodrome.util.CacheManager
import com.example.velodrome.util.CredentialsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG_OFFLINE = "LOCAL_OFFLINE"

@Singleton
class TrackRepositoryImpl @Inject constructor(
    private val api: NavidromeApi,
    private val trackDao: TrackDao,
    private val credentialsManager: CredentialsManager,
    private val cacheManager: CacheManager
) : TrackRepository {

    override fun observeTracksByAlbum(albumId: String): Flow<List<Track>> {
        return trackDao.observeTracksByAlbum(albumId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncTracksForAlbum(albumId: String): Result<Unit> {
        return runCatching {
            val response = api.getMusicDirectory(albumId)
            val songsList = response.response.directory?.child ?: emptyList()

            val entities = songsList.map { song ->
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
                ).toEntity()
            }
            trackDao.insertTracks(entities)
        }
    }

    override suspend fun getStreamUrl(trackId: String): String {
        return credentialsManager.getStreamUrl(trackId)
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
            year = dto.year,
            isCached = false,
            coverArtId = effectiveCoverArtId
        )
    }

    override suspend fun getSongsByGenre(genre: String, count: Int, offset: Int): Result<List<Track>> {
        return runCatching {
            val response = api.getSongsByGenre(genre, count, offset)
            val songDtos = response.response.songsByGenre?.song ?: emptyList()
            songDtos.map { mapSongDto(it, it.albumId ?: genre) }
        }
    }

    override suspend fun getRandomSongsByGenre(genre: String, size: Int): Result<List<Track>> {
        return runCatching {
            val response = api.getRandomSongs(size, genre)
            val songDtos = response.response.randomSongs?.song ?: emptyList()
            songDtos.map { mapSongDto(it, it.albumId ?: genre) }
        }
    }

    override suspend fun getRandomSongs(size: Int, genre: String?, fromYear: Int?, toYear: Int?): Result<List<Track>> {
        return runCatching {
            val response = api.getRandomSongs(size, genre, fromYear, toYear)
            val songDtos = response.response.randomSongs?.song ?: emptyList()
            songDtos.map { mapSongDto(it, it.albumId ?: "") }
        }
    }

    override suspend fun searchRemoteTracks(query: String): Result<List<Track>> {
        return runCatching {
            val response = api.search3(query = query, songCount = 100)
            val res = response.response

            val songDtos = res.searchResult3?.songs
                ?: res.searchResult2?.songs
                ?: emptyList()

            songDtos.map { mapSongDto(it, it.albumId ?: "search_res") }
        }
    }

    override suspend fun getTopSongs(count: Int): Result<List<Track>> {
        return runCatching {
            val response = api.getTopSongs(count)
            val songDtos = response.response.topSongs?.song ?: emptyList()
            songDtos.map { mapSongDto(it, it.albumId ?: "") }
        }
    }

    override suspend fun getOfflineTracks(): List<Track> {
        Log.d(TAG_OFFLINE, "=== getOfflineTracks() called ===")
        val allLocalTracks = trackDao.getAllTracksOnce()
        Log.d(TAG_OFFLINE, "Total local tracks in DB: ${allLocalTracks.size}")

        val cachedKeys = cacheManager.getCachedKeys()
        Log.d(TAG_OFFLINE, "SimpleCache keys count: ${cachedKeys.size}")

        val serverUrl = credentialsManager.getServerUrl() ?: ""
        val authParams = credentialsManager.generateAuthParams()

        if (authParams == null) {
            Log.d(TAG_OFFLINE, "No credentials - returning empty")
            return emptyList()
        }

        val (username, token, salt) = authParams

        val matched = allLocalTracks.filter { track ->
            val streamUrl = "${serverUrl.trimEnd('/')}/rest/stream.view" +
                "?id=${track.id}&u=$username&t=$token&s=$salt&v=1.16.1&c=Velodrome&maxBitRate=320"

            val sha1 = MessageDigest.getInstance("SHA-1")
            val hash = sha1.digest(streamUrl.toByteArray(Charsets.UTF_8))
            val hashHex = hash.joinToString("") { "%02x".format(it) }

            val matches = cachedKeys.contains(hashHex)
            if (matches) {
                Log.d(TAG_OFFLINE, "MATCH: '${track.title}' (id=${track.id})")
            }
            matches
        }

        Log.d(TAG_OFFLINE, "Matched offline tracks: ${matched.size}")
        return matched.map { it.toDomain() }
    }
}