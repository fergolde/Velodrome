package com.example.velodrome.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.velodrome.data.local.datasource.LocalMusicDataSource
import com.example.velodrome.data.local.mapper.toDomain
import com.example.velodrome.data.local.mapper.toEntity
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.data.remote.dto.AlbumDetailDto
import com.example.velodrome.data.remote.dto.AlbumDto
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val api: NavidromeApi,
    private val dataStore: DataStore<Preferences>,
    private val localMusicDataSource: LocalMusicDataSource
) : AlbumRepository {

    override fun observeAllAlbums(): Flow<List<Album>> {
        return localMusicDataSource.observeAllAlbums().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAlbum(albumId: String): Result<Album> {
        return runCatching {
            Log.d("Repo", "=== getAlbum called: $albumId ===")
            val response = api.getAlbum(albumId)
            val albumDto = response.response.album

            Log.d("Repo", "Got album DTO: id=${albumDto?.id}, title=${albumDto?.title}")

            val dto = albumDto ?: AlbumDetailDto(id = albumId)
            Album(
                id = dto.id,
                artistId = dto.artistId ?: "",
                artistName = dto.artist ?: "",
                title = dto.title ?: dto.name ?: "",
                year = dto.year,
                genre = dto.genre,
                coverUrl = dto.coverArt
            )
        }
    }

    private fun mapAlbumDto(dto: AlbumDto): Album {
        return Album(
            id = dto.id,
            artistId = dto.artistId ?: "",
            artistName = dto.artist ?: "",
            title = dto.title ?: dto.name ?: "",
            year = dto.year,
            genre = dto.genre,
            coverUrl = dto.coverArt
        )
    }

    override suspend fun getLatestAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getLatestAlbums called ===")
            val response = api.getAlbumList2(type = "newest", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            Log.d("Repo", "Found ${albums.size} albums from API")
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getTopAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getTopAlbums (frequent) ===")
            val response = api.getAlbumList2(type = "frequent", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            Log.d("Repo", "Found ${albums.size} top albums")
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getRecentlyPlayedAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getRecentlyPlayedAlbums (recent) ===")
            val response = api.getAlbumList2(type = "recent", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            Log.d("Repo", "Found ${albums.size} recently played albums")
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getRandomAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getRandomAlbums (random) ===")
            val response = api.getAlbumList2(type = "random", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            Log.d("Repo", "Found ${albums.size} random albums")
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getAllAlbums(size: Int): Result<List<Album>> {
        return getAllAlbumsFromServer(offset = 0, size = size)
    }

    override suspend fun getAllAlbumsFromServer(offset: Int, size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getAllAlbumsFromServer offset=$offset size=$size ===")
            val response = api.getAlbumList2(type = "alphabeticalByName", size = size, offset = offset)
            val albums = response.response.albumList2?.albums ?: emptyList()
            Log.d("Repo", "Found ${albums.size} albums at offset $offset")
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getAlbumsByYear(year: Int, size: Int): Result<List<Album>> {
        return runCatching {
            val response = api.getAlbumList2(
                type = "alphabeticalByName",
                size = size,
                fromYear = year,
                toYear = year
            )
            val albums = response.response.albumList2?.albums ?: emptyList()
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getAlbumsByGenre(genre: String, size: Int): Result<List<Album>> {
        return runCatching {
            val response = api.getAlbumList2(
                type = "alphabeticalByName",
                size = size,
                genre = genre
            )
            val albums = response.response.albumList2?.albums ?: emptyList()
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getGenres(): Result<List<String>> {
        return runCatching {
            val response = api.getGenres()
            val genres = response.response.genres?.genres ?: emptyList()
            Log.d("AlbumRepo", "Found ${genres.size} genres")
            genres.mapNotNull { it.value ?: it.name }
        }
    }

    override suspend fun syncAlbumsFromServer(): Result<Int> {
        return runCatching {
            Log.d("AlbumRepo", "=== syncAlbumsFromServer ===")
            var offset = 0
            val pageSize = 500
            var totalSynced = 0

            while (offset < 10000) {
                val result = getAllAlbumsFromServer(offset = offset, size = pageSize)
                val albums = result.getOrNull()
                    ?: throw result.exceptionOrNull() ?: Exception("Failed to fetch albums")

                if (albums.isEmpty()) {
                    Log.d("AlbumRepo", "No more albums at offset $offset")
                    break
                }

                val entities = albums.map { it.toEntity() }
                localMusicDataSource.insertAlbums(entities)
                totalSynced += albums.size
                Log.d("AlbumRepo", "Synced ${albums.size} albums (total: $totalSynced)")

                if (albums.size < pageSize) break
                offset += pageSize
            }
            Log.d("AlbumRepo", "Album sync completed: $totalSynced total")
            totalSynced
        }
    }
}