package com.fergolde.velodrome.data.repository

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.mapper.toDomain
import com.fergolde.velodrome.data.local.mapper.toEntity
import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.data.remote.dto.SongDto
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.repository.TrackRepository
import com.fergolde.velodrome.util.CacheManager
import com.fergolde.velodrome.util.CredentialsManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

            // Filtramos asegurando que solo procesamos archivos (isDir = false o null)
            val songsList = response.response.directory?.child?.filter { it.isDir != true }
                ?: return@runCatching // Si no hay directorio o hijos, terminamos exitosamente

            val entities = songsList.map { song ->
                Track(
                    id = song.id,
                    albumId = albumId,
                    title = song.title,
                    artistName = song.artist ?: "",
                    albumName = song.album ?: "",
                    durationSec = song.duration ?: 0,
                    sizeBytes = song.size ?: 0L,
                    trackNumber = song.track ?: 0,
                    coverArtId = song.coverArt,
                    playCount = song.playCount ?: 0
                ).toEntity()
            }

            if (entities.isNotEmpty()) {
                trackDao.insertTracks(entities)
            }
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
            trackNumber = dto.track ?: 0,
            coverArtId = effectiveCoverArtId,
            playCount = dto.playCount ?: 0
        )
    }

    override suspend fun getRandomSongs(size: Int, genre: String?, fromYear: Int?, toYear: Int?): Result<List<Track>> {
        return runCatching {
            val response = api.getRandomSongs(size, genre, fromYear, toYear)
            val songDtos = response.response.randomSongs?.song ?: emptyList()
            val tracks = songDtos.map { mapSongDto(it, it.albumId ?: "") }

            // ¡Guardamos en Room para tener el sizeBytes disponible en el futuro!
            saveTracksToLocalDb(tracks)

            tracks
        }
    }

    override suspend fun searchRemoteTracks(query: String): Result<List<Track>> {
        return runCatching {
            val response = api.search3(query = query, songCount = 100)
            val songDtos = response.response.searchResult3?.songs ?: emptyList()
            songDtos.map { mapSongDto(it, it.albumId ?: "search_res") }
        }
    }

    @OptIn(UnstableApi::class)
    override suspend fun getOfflineTracks(): List<Track> {
        val allLocalTracks = trackDao.getAllTracksOnce()

        return allLocalTracks.filter { track ->
            val spans = cacheManager.isTrackFullyCached(track.id, track.sizeBytes)
            spans
        }.map { it.toDomain() }
    }


    @OptIn(UnstableApi::class)
    override suspend fun getTopGlobalTracks(size: Int): Result<List<Track>> {
        return runCatching {
            val response = api.getAlbumList2(type = "frequent", size = 50)
            val albums = response.response.albumList2?.albums ?: emptyList()

            val allTracks = coroutineScope {
                albums.map { album ->
                    async {
                        val albumResponse = api.getAlbum(album.id)
                        val songs = albumResponse.response.album?.songs ?: emptyList()
                        songs.map { mapSongDto(it, album.id) }
                    }
                }.awaitAll().flatten()
            }

            allTracks
                .filter { it.playCount > 0 }
                .distinctBy { it.id }
                .sortedByDescending { it.playCount }
                .take(size)
                .shuffled()
        }
    }

    // Usa este método para guardar canciones cada vez que las obtengas de la API
    private suspend fun saveTracksToLocalDb(tracks: List<Track>) {
        trackDao.insertTracks(tracks.map { it.toEntity() })
    }
}