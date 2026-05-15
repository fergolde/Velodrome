package com.example.velodrome.data.repository

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepositoryImpl @OptIn(UnstableApi::class)
@Inject constructor(
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

    @OptIn(UnstableApi::class)
    override suspend fun getOfflineTracks(): List<Track> {
        val allLocalTracks = trackDao.getAllTracksOnce()

        return allLocalTracks.filter { track ->
            // Validamos que el track esté completo (95%+ descargado)
            cacheManager.isTrackFullyCached(track.id, track.sizeBytes)
        }.map { it.toDomain() }
    }
}