package com.example.velodrome.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.velodrome.data.local.datasource.LocalMusicDataSource
import com.example.velodrome.data.local.mapper.toDomain
import com.example.velodrome.data.local.mapper.toEntity
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.data.remote.dto.AlbumDto
import com.example.velodrome.data.remote.dto.ArtistDetailDto
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.ArtistWithAlbums
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

    override fun getArtistsPaged(): PagingSource<Int, Artist> {
        return ArtistPagingSource(localMusicDataSource)
    }

    override suspend fun getArtists(offset: Int, size: Int): Result<List<Artist>> {
        return runCatching {
            val response = api.getArtists(size, offset)

            val indexes = response.response.artists?.indexes
            val flatArtists = response.response.artists?.artistList

            val artistsList = when {
                indexes != null -> indexes.flatMap { it.artists }
                flatArtists != null -> flatArtists
                else -> emptyList()
            }


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
            val response = api.getArtist(artistId)
            val artistDto = response.response.artist

            val dto = artistDto ?: ArtistDetailDto(id = artistId, name = "Unknown")
            val albums = dto.albums?.map { mapAlbumDto(it) } ?: emptyList()

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
        return syncArtistsFromServer(startOffset = 0) { }
    }

    override suspend fun syncArtistsFromServer(
        startOffset: Int,
        onPageProcessed: suspend (newOffset: Int) -> Unit
    ): Result<Int> {
        return runCatching {
            var offset = startOffset
            val pageSize = 500
            var totalSynced = 0

            while (true) {
                val result = getArtists(offset = offset, size = pageSize)
                val artists = result.getOrNull()
                    ?: throw result.exceptionOrNull() ?: Exception("Failed to fetch artists")

                if (artists.isEmpty() || artists.size < pageSize) {
                    // Guardar los últimos resultados si hay alguno
                    if (artists.isNotEmpty()) {
                        val entities = artists.map { it.toEntity() }
                        localMusicDataSource.insertArtists(entities)
                        totalSynced += artists.size
                        onPageProcessed(offset + artists.size)
                    }
                    break
                }

                val entities = artists.map { it.toEntity() }
                localMusicDataSource.insertArtists(entities)
                totalSynced += artists.size

                // Notify offset for resume capability
                onPageProcessed(offset + artists.size)

                offset += pageSize
            }
            totalSynced
        }
    }

    override suspend fun searchLocal(query: String): List<Artist> {
        return localMusicDataSource.searchArtists(query).map { it.toDomain() }
    }
}

class ArtistPagingSource(
    private val localDataSource: LocalMusicDataSource
) : PagingSource<Int, Artist>() {

    override fun getRefreshKey(state: PagingState<Int, Artist>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Artist> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val offset = page * pageSize

            val artists = localDataSource.getArtistsPage(offset = offset, limit = pageSize)

            LoadResult.Page(
                data = artists.map { it.toDomain() },
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (artists.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}