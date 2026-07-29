package com.fergolde.velodrome.domain.usecase

import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.ArtistWithAlbums
import com.fergolde.velodrome.domain.repository.ArtistRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ArtistUseCasesTest {

    private val repository: ArtistRepository = mockk()
    private val getArtist = GetArtistUseCase(repository)
    private val searchLocal = SearchLocalArtistsUseCase(repository)
    private val syncArtists = SyncArtistsUseCase(repository)
    private val observeArtists = ObserveArtistsUseCase(repository)

    private val sampleArtist = Artist(id = "1", name = "Artist Name", albumCount = 3, coverUrl = "art-1")
    private val sampleAlbum = Album(id = "a1", artistId = "1", artistName = "Artist Name", title = "Album", year = 2020, genre = "Rock", coverUrl = "cov-1")

    @Test
    fun getArtist_success() = runTest {
        val expected = ArtistWithAlbums(artist = sampleArtist, albums = listOf(sampleAlbum))
        coEvery { repository.getArtist("1") } returns Result.success(expected)
        val result = getArtist("1")
        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
        coVerify { repository.getArtist("1") }
    }

    @Test
    fun getArtist_failure() = runTest {
        coEvery { repository.getArtist("bad") } returns Result.failure(Exception("not found"))
        assertTrue(getArtist("bad").isFailure)
    }

    @Test
    fun searchLocal_findsResults() = runTest {
        coEvery { repository.searchLocal("artist") } returns listOf(sampleArtist)
        val result = searchLocal("artist")
        assertEquals(listOf(sampleArtist), result)
    }

    @Test
    fun searchLocal_empty() = runTest {
        coEvery { repository.searchLocal("") } returns emptyList()
        assertTrue(searchLocal("").isEmpty())
    }

    @Test
    fun syncArtists_success() = runTest {
        coEvery { repository.syncArtistsFromServer() } returns Result.success(10)
        val result = syncArtists()
        assertTrue(result.isSuccess)
        assertEquals(10, result.getOrNull())
    }

    @Test
    fun syncArtists_failure() = runTest {
        coEvery { repository.syncArtistsFromServer() } returns Result.failure(Exception("fail"))
        assertTrue(syncArtists().isFailure)
    }

    @Test
    fun observeArtists_emitsFromRepo() = runTest {
        coEvery { repository.observeAllArtists() } returns flowOf(listOf(sampleArtist))
        val result = observeArtists().first()
        assertEquals(listOf(sampleArtist), result)
    }
}
