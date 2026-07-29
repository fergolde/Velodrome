package com.fergolde.velodrome.domain.usecase

import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.repository.TrackRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TrackUseCasesTest {

    private val repository: TrackRepository = mockk()
    private val observeTracks = ObserveTracksByAlbumUseCase(repository)
    private val syncTracks = SyncTracksForAlbumUseCase(repository)
    private val getRandom = GetRandomSongsUseCase(repository)
    private val searchRemote = SearchRemoteTracksUseCase(repository)
    private val getOffline = GetOfflineTracksUseCase(repository)
    private val getTopGlobal = GetTopGlobalTracksUseCase(repository)

    private val sampleTrack = Track(
        id = "t1", albumId = "a1", albumName = "Album", artistName = "Artist",
        title = "Song", durationSec = 200, sizeBytes = 5000000L, bitrate = 320,
        trackNumber = 1, coverArtId = "cov-1"
    )

    @Test
    fun observeTracksByAlbum_emitsFromRepo() = runTest {
        coEvery { repository.observeTracksByAlbum("a1") } returns flowOf(listOf(sampleTrack))
        val result = observeTracks("a1").first()
        assertEquals(listOf(sampleTrack), result)
    }

    @Test
    fun syncTracksForAlbum_success() = runTest {
        coEvery { repository.syncTracksForAlbum("a1") } returns Result.success(Unit)
        assertTrue(syncTracks("a1").isSuccess)
        coVerify { repository.syncTracksForAlbum("a1") }
    }

    @Test
    fun syncTracksForAlbum_failure() = runTest {
        coEvery { repository.syncTracksForAlbum("a1") } returns Result.failure(Exception("fail"))
        assertTrue(syncTracks("a1").isFailure)
    }

    @Test
    fun getRandomSongs_success() = runTest {
        coEvery { repository.getRandomSongs(50, null, null, null) } returns Result.success(listOf(sampleTrack))
        val result = getRandom()
        assertTrue(result.isSuccess)
        assertEquals(listOf(sampleTrack), result.getOrNull())
    }

    @Test
    fun getRandomSongs_withFilters() = runTest {
        coEvery { repository.getRandomSongs(10, "Rock", 2000, 2010) } returns Result.success(listOf(sampleTrack))
        val result = getRandom(size = 10, genre = "Rock", fromYear = 2000, toYear = 2010)
        assertTrue(result.isSuccess)
    }

    @Test
    fun getRandomSongs_failure() = runTest {
        coEvery { repository.getRandomSongs(50, null, null, null) } returns Result.failure(Exception("fail"))
        assertTrue(getRandom().isFailure)
    }

    @Test
    fun searchRemoteTracks_success() = runTest {
        coEvery { repository.searchRemoteTracks("query") } returns Result.success(listOf(sampleTrack))
        val result = searchRemote("query")
        assertTrue(result.isSuccess)
    }

    @Test
    fun searchRemoteTracks_emptyQuery() = runTest {
        coEvery { repository.searchRemoteTracks("") } returns Result.success(emptyList())
        assertTrue(searchRemote("").getOrNull()!!.isEmpty())
    }

    @Test
    fun getOfflineTracks_returnsFiltered() = runTest {
        coEvery { repository.getOfflineTracks() } returns listOf(sampleTrack)
        val result = getOffline()
        assertEquals(listOf(sampleTrack), result)
    }

    @Test
    fun getOfflineTracks_empty() = runTest {
        coEvery { repository.getOfflineTracks() } returns emptyList()
        assertTrue(getOffline().isEmpty())
    }

    @Test
    fun getTopGlobalTracks_success() = runTest {
        coEvery { repository.getTopGlobalTracks(100) } returns Result.success(listOf(sampleTrack))
        val result = getTopGlobal()
        assertTrue(result.isSuccess)
    }

    @Test
    fun getTopGlobalTracks_customSize() = runTest {
        coEvery { repository.getTopGlobalTracks(50) } returns Result.success(listOf(sampleTrack))
        val result = getTopGlobal(50)
        assertTrue(result.isSuccess)
        coVerify { repository.getTopGlobalTracks(50) }
    }

    @Test
    fun getTopGlobalTracks_failure() = runTest {
        coEvery { repository.getTopGlobalTracks(100) } returns Result.failure(Exception("fail"))
        assertTrue(getTopGlobal().isFailure)
    }
}
