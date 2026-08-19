package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.datasource.LocalMusicDataSource
import com.fergolde.velodrome.data.local.entity.AlbumEntity
import com.fergolde.velodrome.data.local.entity.ArtistEntity
import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.data.remote.dto.*
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.ArtistWithAlbums
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ArtistRepositoryImplTest {

    private val api: NavidromeApi = mockk()
    private val localDataSource: LocalMusicDataSource = mockk()
    private val repository = ArtistRepositoryImpl(api, localDataSource)

    @Test
    fun observeAllArtists_mapsFromLocal() = runTest {
        val entity = ArtistEntity(id = "1", name = "Artist", albumCount = 3, coverUrl = "art-1")
        coEvery { localDataSource.observeAllArtists() } returns flowOf(listOf(entity))
        val result = repository.observeAllArtists().first()
        assertEquals(1, result.size)
        assertEquals("Artist", result[0].name)
    }

    @Test
    fun getArtists_withIndexes_flattensToArtists() = runTest {
        val artistDto = ArtistDto(id = "1", name = "Artist 1", albumCount = 3, coverArt = "art-1")
        val index = ArtistIndexDto(name = "A", artists = listOf(artistDto))
        val artistsDto = ArtistsDto(indexes = listOf(index), artistList = null)
        val dto = SubsonicResponseDto(status = "ok", version = "1.16.1", artists = artistsDto)
        coEvery { api.getArtists(50, 0) } returns SubsonicResponse(dto)

        val result = repository.getArtists(0, 50)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
        assertEquals("Artist 1", result.getOrNull()!![0].name)
    }

    @Test
    fun getArtists_decodesHtmlEntities() = runTest {
        val artistDto = ArtistDto(id = "1", name = "Rock &amp; Roll Band", albumCount = 3, coverArt = "art-1")
        val index = ArtistIndexDto(name = "A", artists = listOf(artistDto))
        val artistsDto = ArtistsDto(indexes = listOf(index), artistList = null)
        val dto = SubsonicResponseDto(status = "ok", version = "1.16.1", artists = artistsDto)
        coEvery { api.getArtists(50, 0) } returns SubsonicResponse(dto)

        val result = repository.getArtists(0, 50)
        assertEquals("Rock & Roll Band", result.getOrNull()!![0].name)
    }

    @Test
    fun getArtists_usesArtistListWhenNoIndexes() = runTest {
        val artistDto = ArtistDto(id = "1", name = "Artist", albumCount = 3, coverArt = "art-1")
        val artistsDto = ArtistsDto(indexes = null, artistList = listOf(artistDto))
        val dto = SubsonicResponseDto(status = "ok", version = "1.16.1", artists = artistsDto)
        coEvery { api.getArtists(50, 0) } returns SubsonicResponse(dto)

        val result = repository.getArtists(0, 50)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun getArtist_success() = runTest {
        val albumDto = AlbumDto(id = "a1", name = "Album", artist = "Artist", artistId = "1", year = 2020)
        val detailDto = ArtistDetailDto(id = "1", name = "Artist", albumCount = 5, coverArt = "art-1", albums = listOf(albumDto))
        val dto = SubsonicResponseDto(status = "ok", version = "1.16.1", artist = detailDto)
        coEvery { api.getArtist("1") } returns SubsonicResponse(dto)

        val result = repository.getArtist("1")
        assertTrue(result.isSuccess)
        val expected = ArtistWithAlbums(
            artist = Artist(id = "1", name = "Artist", albumCount = 5, coverUrl = "art-1"),
            albums = listOf(
                com.fergolde.velodrome.domain.model.Album(id = "a1", artistId = "1", artistName = "Artist", title = "Album", year = 2020, genre = null, coverUrl = null)
            )
        )
        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun getArtist_notFound() = runTest {
        coEvery { api.getArtist("bad") } throws RuntimeException("not found")
        assertTrue(repository.getArtist("bad").isFailure)
    }

    @Test
    fun searchLocal_delegates() = runTest {
        val entity = ArtistEntity(id = "1", name = "Artist", albumCount = 3, coverUrl = "art-1")
        coEvery { localDataSource.searchArtists("query") } returns listOf(entity)
        val result = repository.searchLocal("query")
        assertEquals(1, result.size)
    }
}
