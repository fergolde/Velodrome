package com.fergolde.velodrome.domain.usecase

import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.repository.AlbumRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AlbumUseCasesTest {

    private val repository: AlbumRepository = mockk()
    private val getLatest = GetLatestAlbumsUseCase(repository)
    private val getTop = GetTopAlbumsUseCase(repository)
    private val getGenres = GetGenresUseCase(repository)
    private val getRecent = GetRecentlyPlayedAlbumsUseCase(repository)
    private val getRandom = GetRandomAlbumsUseCase(repository)
    private val getAlbum = GetAlbumUseCase(repository)
    private val searchLocal = SearchLocalAlbumsUseCase(repository)
    private val syncAlbums = SyncAlbumsUseCase(repository)
    private val getMinYear = GetMinYearUseCase(repository)

    private val sampleAlbum = Album(id = "1", artistId = "a1", artistName = "Artist", title = "Album", year = 2020, genre = "Rock", coverUrl = "cov-1")

    @Test
    fun getLatestAlbums_success() = runTest {
        val albums = listOf(sampleAlbum)
        coEvery { repository.getLatestAlbums(20) } returns Result.success(albums)
        val result = getLatest(20)
        assertTrue(result.isSuccess)
        assertEquals(albums, result.getOrNull())
        coVerify { repository.getLatestAlbums(20) }
    }

    @Test
    fun getLatestAlbums_failure() = runTest {
        coEvery { repository.getLatestAlbums(20) } returns Result.failure(Exception("API error"))
        val result = getLatest(20)
        assertTrue(result.isFailure)
    }
    
    @Test
    fun getLatestAlbums_usesDefaultSize() = runTest {
        coEvery { repository.getLatestAlbums(20) } returns Result.success(listOf(sampleAlbum))
        getLatest()
        coVerify { repository.getLatestAlbums(20) }
    }

    @Test
    fun getTopAlbums_success() = runTest {
        val albums = listOf(sampleAlbum)
        coEvery { repository.getTopAlbums(10) } returns Result.success(albums)
        val result = getTop(10)
        assertTrue(result.isSuccess)
        assertEquals(albums, result.getOrNull())
    }

    @Test
    fun getTopAlbums_failure() = runTest {
        coEvery { repository.getTopAlbums(10) } returns Result.failure(Exception("fail"))
        assertTrue(getTop(10).isFailure)
    }

    @Test
    fun getGenres_success() = runTest {
        coEvery { repository.getGenres() } returns Result.success(listOf("Rock", "Pop"))
        val result = getGenres()
        assertTrue(result.isSuccess)
        assertEquals(listOf("Rock", "Pop"), result.getOrNull())
    }

    @Test
    fun getGenres_failure() = runTest {
        coEvery { repository.getGenres() } returns Result.failure(Exception("fail"))
        assertTrue(getGenres().isFailure)
    }

    @Test
    fun getRecentlyPlayedAlbums_success() = runTest {
        coEvery { repository.getRecentlyPlayedAlbums(5) } returns Result.success(listOf(sampleAlbum))
        assertTrue(getRecent(5).isSuccess)
    }

    @Test
    fun getRandomAlbums_success() = runTest {
        coEvery { repository.getRandomAlbums(15) } returns Result.success(listOf(sampleAlbum))
        assertTrue(getRandom(15).isSuccess)
    }

    @Test
    fun getAlbum_success() = runTest {
        coEvery { repository.getAlbum("1") } returns Result.success(sampleAlbum)
        val result = getAlbum("1")
        assertTrue(result.isSuccess)
        assertEquals(sampleAlbum, result.getOrNull())
    }

    @Test
    fun getAlbum_failure() = runTest {
        coEvery { repository.getAlbum("bad") } returns Result.failure(Exception("not found"))
        assertTrue(getAlbum("bad").isFailure)
    }

    @Test
    fun searchLocal_returnsResults() = runTest {
        coEvery { repository.searchLocal("query") } returns listOf(sampleAlbum)
        val result = searchLocal("query")
        assertEquals(listOf(sampleAlbum), result)
    }

    @Test
    fun searchLocal_emptyQuery() = runTest {
        coEvery { repository.searchLocal("") } returns emptyList()
        assertTrue(searchLocal("").isEmpty())
    }

    @Test
    fun syncAlbums_success() = runTest {
        coEvery { repository.syncAlbumsFromServer() } returns Result.success(42)
        val result = syncAlbums()
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun getMinYear_returnsValue() = runTest {
        coEvery { repository.getMinYear() } returns 1980
        assertEquals(1980, getMinYear())
    }
}
