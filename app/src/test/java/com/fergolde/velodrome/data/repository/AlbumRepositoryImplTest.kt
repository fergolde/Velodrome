package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.datasource.LocalMusicDataSource
import com.fergolde.velodrome.data.local.entity.AlbumEntity
import com.fergolde.velodrome.data.local.mapper.toEntity
import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.data.remote.dto.*
import com.fergolde.velodrome.domain.model.Album
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AlbumRepositoryImplTest {

    private val api: NavidromeApi = mockk()
    private val localDataSource: LocalMusicDataSource = mockk()
    private val repository = AlbumRepositoryImpl(api, localDataSource)

    private val sampleAlbumDto = AlbumDto(
        id = "1", name = "Album Name", title = "Album Title", artist = "Artist",
        artistId = "a1", coverArt = "cov-1", year = 2020, genre = "Rock"
    )

    private val sampleAlbum = Album(
        id = "1", artistId = "a1", artistName = "Artist", title = "Album Title",
        year = 2020, genre = "Rock", coverUrl = "cov-1"
    )

    @Test
    fun getLatestAlbums_success() = runTest {
        val listDto = AlbumListDto(albums = listOf(sampleAlbumDto))
        val dto = SubsonicResponseDto(status = "ok", albumList2 = listDto)
        coEvery { api.getAlbumList2(type = "newest", size = 20) } returns SubsonicResponse(dto)
        val result = repository.getLatestAlbums(20)
        assertTrue(result.isSuccess)
        assertEquals(listOf(sampleAlbum), result.getOrNull())
    }

    @Test
    fun getLatestAlbums_emptyList() = runTest {
        val listDto = AlbumListDto(albums = null)
        val dto = SubsonicResponseDto(status = "ok", albumList2 = listDto)
        coEvery { api.getAlbumList2(type = "newest", size = 20) } returns SubsonicResponse(dto)
        val result = repository.getLatestAlbums(20)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun getLatestAlbums_apiError() = runTest {
        coEvery { api.getAlbumList2(type = "newest", size = 20) } throws RuntimeException("API down")
        val result = repository.getLatestAlbums(20)
        assertTrue(result.isFailure)
    }

    @Test
    fun getTopAlbums_success() = runTest {
        val listDto = AlbumListDto(albums = listOf(sampleAlbumDto))
        val dto = SubsonicResponseDto(status = "ok", albumList2 = listDto)
        coEvery { api.getAlbumList2(type = "frequent", size = 20) } returns SubsonicResponse(dto)
        val result = repository.getTopAlbums(20)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun getAlbum_success() = runTest {
        val detailDto = AlbumDetailDto(id = "1", title = "Album", artist = "Artist", artistId = "a1", coverArt = "cov-1", year = 2020, genre = "Rock")
        val dto = SubsonicResponseDto(status = "ok", album = detailDto)
        coEvery { api.getAlbum("1") } returns SubsonicResponse(dto)
        val result = repository.getAlbum("1")
        assertTrue(result.isSuccess)
        assertEquals("1", result.getOrNull()!!.id)
    }

    @Test
    fun getGenres_success() = runTest {
        val genreDtos = listOf(GenreDto(name = "Rock", value = "rock"), GenreDto(name = "Pop", value = "pop"))
        val genresDto = GenresDto(genres = genreDtos)
        val dto = SubsonicResponseDto(status = "ok", genres = genresDto)
        coEvery { api.getGenres() } returns SubsonicResponse(dto)
        val result = repository.getGenres()
        assertTrue(result.isSuccess)
        assertEquals(listOf("rock", "pop"), result.getOrNull())
    }

    @Test
    fun getGenres_fallbackToName() = runTest {
        val genreDtos = listOf(GenreDto(name = "Rock", value = null))
        val genresDto = GenresDto(genres = genreDtos)
        val dto = SubsonicResponseDto(status = "ok", genres = genresDto)
        coEvery { api.getGenres() } returns SubsonicResponse(dto)
        val result = repository.getGenres()
        assertEquals(listOf("Rock"), result.getOrNull())
    }

    @Test
    fun searchLocal_delegates() = runTest {
        val entity = AlbumEntity(id = "1", artistId = "a1", artistName = "A", title = "T", year = null, genre = null, coverUrl = null)
        coEvery { localDataSource.searchAlbums("query") } returns listOf(entity)
        val result = repository.searchLocal("query")
        assertEquals(1, result.size)
    }

    @Test
    fun getMinYear_returnsFromLocal() = runTest {
        coEvery { localDataSource.getMinYear() } returns 1980
        assertEquals(1980, repository.getMinYear())
    }

    @Test
    fun getMinYear_defaultWhenNull() = runTest {
        coEvery { localDataSource.getMinYear() } returns null
        assertEquals(1950, repository.getMinYear())
    }

    @Test
    fun getMinYear_defaultWhenZero() = runTest {
        coEvery { localDataSource.getMinYear() } returns 0
        assertEquals(1950, repository.getMinYear())
    }
}
