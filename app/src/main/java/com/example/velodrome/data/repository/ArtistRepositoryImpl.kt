package com.example.velodrome.data.repository

import android.util.Log
import com.example.velodrome.data.local.datasource.LocalMusicDataSource
import com.example.velodrome.data.local.mapper.toDomain
import com.example.velodrome.data.local.mapper.toEntity
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.data.remote.dto.AlbumDetailDto
import com.example.velodrome.data.remote.dto.AlbumDto
import com.example.velodrome.data.remote.dto.ArtistDetailDto
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.ArtistWithAlbums
import com.example.velodrome.domain.repository.AlbumRepository
import com.example.velodrome.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepositoryImpl @Inject constructor(
    private val api: NavidromeApi,
    private val localMusicDataSource: LocalMusicDataSource
) : ArtistRepository {

    override fun observeAllArtists(): Flow<List<Artist>> {
        return localMusicDataSource.observeAllArtists().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getArtists(offset: Int, size: Int): Result<List<Artist>> {
        return runCatching {
            Log.d("Repo", "=== getArtists called ===")
            val response = api.getArtists(size, offset)

            val indexes = response.response.artists?.indexes
            val flatArtists = response.response.artists?.artistList

            val artistsList = when {
                indexes != null -> indexes.flatMap { it.artists ?: emptyList() }
                flatArtists != null -> flatArtists
                else -> emptyList()
            }

            Log.d("Repo", "Found ${artistsList.size} artists")

            artistsList.map { artistDto ->
                Artist(
                    id = artistDto.id,
                    name = decodeHtmlEntities(artistDto.name),
                    albumCount = artistDto.albumCount ?: 0,
                    coverUrl = artistDto.coverArt
                )
            }
        }
    }

    override suspend fun getArtist(artistId: String): Result<ArtistWithAlbums> {
        return runCatching {
            Log.d("Repo", "=== getArtist called: $artistId ===")
            val response = api.getArtist(artistId)
            val artistDto = response.response.artist

            Log.d("Repo", "Got artist DTO: id=${artistDto?.id}, name=${artistDto?.name}")

            val dto = artistDto ?: ArtistDetailDto(id = artistId, name = "Unknown")
            val albums = dto.albums?.map { mapAlbumDto(it) } ?: emptyList()

            Log.d("Repo", "Found ${albums.size} albums for artist ${dto.name}")

            val artist = Artist(
                id = dto.id,
                name = dto.name,
                albumCount = dto.albumCount ?: 0,
                coverUrl = dto.coverArt
            )

            ArtistWithAlbums(artist = artist, albums = albums)
        }
    }

    override suspend fun search(query: String): Result<List<Artist>> {
        return runCatching {
            val response = api.search(query)
            val artists = response.response.searchResult2?.artists ?: emptyList()
            artists.map { dto ->
                Artist(
                    id = dto.id,
                    name = dto.name,
                    albumCount = dto.albumCount ?: 0,
                    coverUrl = dto.coverArt
                )
            }
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

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    override suspend fun syncArtistsFromServer(): Result<Int> {
        return runCatching {
            Log.d("ArtistRepo", "=== syncArtistsFromServer ===")
            var offset = 0
            val pageSize = 500
            var totalSynced = 0

            while (offset < 10000) {
                val result = getArtists(offset = offset, size = pageSize)
                val artists = result.getOrNull()
                    ?: throw result.exceptionOrNull() ?: Exception("Failed to fetch artists")

                if (artists.isEmpty()) {
                    Log.d("ArtistRepo", "No more artists at offset $offset")
                    break
                }

                val entities = artists.map { it.toEntity() }
                localMusicDataSource.insertArtists(entities)
                totalSynced += artists.size
                Log.d("ArtistRepo", "Synced ${artists.size} artists (total: $totalSynced)")

                if (artists.size < pageSize) break
                offset += pageSize
            }
            Log.d("ArtistRepo", "Artist sync completed: $totalSynced total")
            totalSynced
        }
    }
}