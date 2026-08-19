package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.entity.TrackEntity
import com.fergolde.velodrome.data.local.mapper.toDomain
import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.data.remote.dto.*
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.util.CacheManager
import com.fergolde.velodrome.util.CredentialsManager
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TrackRepositoryImplTest {

    private val api: NavidromeApi = mockk()
    private val trackDao: TrackDao = mockk()
    private val credentialsManager: CredentialsManager = mockk()
    private val cacheManager: CacheManager = mockk()
    private val repository = TrackRepositoryImpl(api, trackDao, credentialsManager, cacheManager)

    private val sampleSongDto = SongDto(
        id = "t1", title = "Song 1", album = "Album", albumId = "a1",
        artist = "Artist", track = 1, duration = 200, size = 5000000L,
        bitRate = 320, coverArt = "cov-1", playCount = 10, year = 2020
    )

    @Test
    fun observeTracksByAlbum_mapsFromDao() = runTest {
        val entity = TrackEntity(id = "t1", albumId = "a1", artistName = "A", albumName = "B", title = "T", durationSec = 180, trackNumber = 1, coverArtId = null)
        coEvery { trackDao.observeTracksByAlbum("a1") } returns flowOf(listOf(entity))
        val result = repository.observeTracksByAlbum("a1").first()
        assertEquals(1, result.size)
    }

    @Test
    fun syncTracksForAlbum_success() = runTest {
        val directoryDto = DirectoryDto(child = listOf(sampleSongDto))
        val dto = SubsonicResponseDto(status = "ok", version = "1.16.1", directory = directoryDto)
        coEvery { api.getMusicDirectory("a1") } returns SubsonicResponse(dto)
        coEvery { trackDao.insertTracks(any()) } just runs

        val result = repository.syncTracksForAlbum("a1")
        assertTrue(result.isSuccess)
        coVerify { trackDao.insertTracks(any()) }
    }

    @Test
    fun syncTracksForAlbum_filtersNonMusic() = runTest {
        val dirSong = SongDto(id = "t1", title = "Song", isDir = true, albumId = "a1")
        val directoryDto = DirectoryDto(child = listOf(dirSong))
        val dto = SubsonicResponseDto(status = "ok", version = "1.16.1", directory = directoryDto)
        coEvery { api.getMusicDirectory("a1") } returns SubsonicResponse(dto)

        val result = repository.syncTracksForAlbum("a1")
        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { trackDao.insertTracks(any()) }
    }

    @Test
    fun syncTracksForAlbum_apiError() = runTest {
        coEvery { api.getMusicDirectory("a1") } throws RuntimeException("API down")
        assertTrue(repository.syncTracksForAlbum("a1").isFailure)
    }

    @Test
    fun getRandomSongs_success() = runTest {
        val randomDto = RandomSongsDto(song = listOf(sampleSongDto))
        val dto = SubsonicResponseDto(status = "ok", version = "1.16.1", randomSongs = randomDto)
        coEvery { api.getRandomSongs(50, null, null, null) } returns SubsonicResponse(dto)
        coEvery { trackDao.insertTracks(any()) } just runs

        val result = repository.getRandomSongs(50)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun searchRemoteTracks_success() = runTest {
        val searchDto = SearchResultDto(songs = listOf(sampleSongDto))
        val dto = SubsonicResponseDto(status = "ok", version = "1.16.1", searchResult3 = searchDto)
        coEvery { api.search3(query = "query", songCount = 100) } returns SubsonicResponse(dto)

        val result = repository.searchRemoteTracks("query")
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun getOfflineTracks_returnsCached() = runTest {
        val entity = TrackEntity(id = "t1", albumId = "a1", artistName = "A", albumName = "B", title = "T", durationSec = 180, trackNumber = 1, coverArtId = null, sizeBytes = 5000000)
        coEvery { trackDao.getAllTracksOnce() } returns listOf(entity)
        every { cacheManager.isTrackFullyCached("t1", 5000000L) } returns true

        val result = repository.getOfflineTracks()
        assertEquals(1, result.size)
    }

    @Test
    fun getOfflineTracks_excludesUncached() = runTest {
        val entity = TrackEntity(id = "t1", albumId = "a1", artistName = "A", albumName = "B", title = "T", durationSec = 180, trackNumber = 1, coverArtId = null, sizeBytes = 5000000)
        coEvery { trackDao.getAllTracksOnce() } returns listOf(entity)
        every { cacheManager.isTrackFullyCached("t1", 5000000L) } returns false

        val result = repository.getOfflineTracks()
        assertTrue(result.isEmpty())
    }
}
