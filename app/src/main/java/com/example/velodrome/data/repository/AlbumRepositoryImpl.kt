package com.example.velodrome.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.velodrome.data.local.datasource.LocalMusicDataSource
import com.example.velodrome.data.local.mapper.toDomain
import com.example.velodrome.data.local.mapper.toEntity
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.data.remote.dto.AlbumDetailDto
import com.example.velodrome.data.remote.dto.AlbumDto
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val api: NavidromeApi,
    private val localMusicDataSource: LocalMusicDataSource
) : AlbumRepository {

    override fun observeAllAlbums(): Flow<List<Album>> {
        return localMusicDataSource.observeAllAlbums().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAlbumsPaged(): PagingSource<Int, Album> {
        return AlbumPagingSource(localMusicDataSource)
    }

    override suspend fun getAlbum(albumId: String): Result<Album> {
        return runCatching {
            val response = api.getAlbum(albumId)
            val albumDto = response.response.album
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
            val response = api.getAlbumList2(type = "newest", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getTopAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            val response = api.getAlbumList2(type = "frequent", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getRecentlyPlayedAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            val response = api.getAlbumList2(type = "recent", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getRandomAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            val response = api.getAlbumList2(type = "random", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getAllAlbums(size: Int): Result<List<Album>> {
        return getAllAlbumsFromServer(offset = 0, size = size)
    }

    override suspend fun getAllAlbumsFromServer(offset: Int, size: Int): Result<List<Album>> {
        return runCatching {
            val response = api.getAlbumList2(type = "alphabeticalByName", size = size, offset = offset)
            val albums = response.response.albumList2?.albums ?: emptyList()
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
            genres.mapNotNull { it.value ?: it.name }
        }
    }

    override suspend fun syncAlbumsFromServer(): Result<Int> {
        return syncAlbumsFromServer(startOffset = 0) { }
    }

    override suspend fun syncAlbumsFromServer(
        startOffset: Int,
        onPageProcessed: suspend (newOffset: Int) -> Unit
    ): Result<Int> {
        return runCatching {
            var offset = startOffset
            val pageSize = 500
            var totalSynced = 0

            while (true) {
                val result = getAllAlbumsFromServer(offset = offset, size = pageSize)
                val albums = result.getOrNull()
                    ?: throw result.exceptionOrNull() ?: Exception("Failed to fetch albums")

                if (albums.isEmpty() || albums.size < pageSize) {
                    // Guardar los últimos resultados si hay alguno
                    if (albums.isNotEmpty()) {
                        val entities = albums.map { it.toEntity() }
                        localMusicDataSource.insertAlbums(entities)
                        totalSynced += albums.size
                        onPageProcessed(offset + albums.size)
                    }
                    break
                }

                val entities = albums.map { it.toEntity() }
                localMusicDataSource.insertAlbums(entities)
                totalSynced += albums.size

                // Notify offset for resume capability
                onPageProcessed(offset + albums.size)

                offset += pageSize
            }
            totalSynced
        }
    }

    override suspend fun hasServerChangedSince(timestamp: Long): Boolean {
        return runCatching {
            val response = api.getIndexes(ifModifiedSince = timestamp)
            val artistsDto = response.response.artists
            val hasChanges = artistsDto?.indexes?.isNotEmpty() == true
                || artistsDto?.artistList?.isNotEmpty() == true
            hasChanges
        }.getOrDefault(true) // Default to true on error to trigger full sync
    }

    override suspend fun searchLocal(query: String): List<Album> {
        return localMusicDataSource.searchAlbums(query).map { it.toDomain() }
    }

    override suspend fun getMinYear(): Int {
        return localMusicDataSource.getMinYear() ?: 1950
    }
}

/**
 * PagingSource that maps AlbumEntity to Album domain model.
 */
class AlbumPagingSource(
    private val localDataSource: LocalMusicDataSource
) : PagingSource<Int, Album>() {

    override fun getRefreshKey(state: PagingState<Int, Album>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Album> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            // offset real = página * tamaño de página
            val offset = page * pageSize

            // getAlbumsPage devuelve List<AlbumEntity>, no un Flow
            val albums = localDataSource.getAlbumsPage(offset = offset, limit = pageSize)

            LoadResult.Page(
                data = albums.map { it.toDomain() },
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (albums.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}