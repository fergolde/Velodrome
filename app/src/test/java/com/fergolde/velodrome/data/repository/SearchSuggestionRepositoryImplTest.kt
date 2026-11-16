package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.AlbumDao
import com.fergolde.velodrome.data.local.dao.ArtistDao
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.entity.AlbumEntity
import com.fergolde.velodrome.data.local.entity.ArtistEntity
import com.fergolde.velodrome.data.local.entity.TrackEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Las sugerencias se resuelven contra la biblioteca local: son por prefijo y
 * nunca tocan la red.
 */
class SearchSuggestionRepositoryImplTest {

    private val artistDao: ArtistDao = mockk(relaxed = true)
    private val albumDao: AlbumDao = mockk(relaxed = true)
    private val trackDao: TrackDao = mockk(relaxed = true)

    private val repository = SearchSuggestionRepositoryImpl(artistDao, albumDao, trackDao)

    @Before
    fun setup() {
        coEvery { artistDao.searchArtistsByPrefix(any(), any()) } returns emptyList()
        coEvery { albumDao.searchAlbumsByPrefix(any(), any()) } returns emptyList()
        coEvery { trackDao.searchTracksByPrefix(any(), any()) } returns emptyList()
    }

    private val artistEntity = ArtistEntity(id = "art1", name = "Daft Punk", albumCount = 2, coverUrl = null)
    private val albumEntity = AlbumEntity(
        id = "al1",
        artistId = "art1",
        artistName = "Daft Punk",
        title = "Discovery",
        year = 2001,
        genre = "Electronic",
        coverUrl = null,
    )
    private val trackEntity = TrackEntity(
        id = "t1",
        albumId = "al1",
        artistName = "Daft Punk",
        albumName = "Discovery",
        title = "One More Time",
        durationSec = 320,
        trackNumber = 1,
        coverArtId = null,
    )

    @Test
    fun `suggestions come from the local DAOs for the given prefix`() = runTest {
        coEvery { artistDao.searchArtistsByPrefix("daf", any()) } returns listOf(artistEntity)
        coEvery { albumDao.searchAlbumsByPrefix("daf", any()) } returns listOf(albumEntity)
        coEvery { trackDao.searchTracksByPrefix("daf", any()) } returns listOf(trackEntity)

        val artists = repository.suggestArtists("daf")
        val albums = repository.suggestAlbums("daf")
        val tracks = repository.suggestTracks("daf")

        assertEquals(listOf("Daft Punk"), artists.map { it.name })
        assertEquals(listOf("Discovery"), albums.map { it.title })
        assertEquals(listOf("One More Time"), tracks.map { it.title })
        coVerify { artistDao.searchArtistsByPrefix("daf", any()) }
        coVerify { albumDao.searchAlbumsByPrefix("daf", any()) }
        coVerify { trackDao.searchTracksByPrefix("daf", any()) }
    }

    @Test
    fun `prefix is trimmed before hitting the DAOs`() = runTest {
        repository.suggestAlbums("  dis  ")

        coVerify { albumDao.searchAlbumsByPrefix("dis", any()) }
    }

    @Test
    fun `blank prefix returns no suggestions and does not query the library`() = runTest {
        // Un prefijo vacío con LIKE '%' devolvería la biblioteca entera.
        assertTrue(repository.suggestArtists("   ").isEmpty())
        assertTrue(repository.suggestAlbums("").isEmpty())
        assertTrue(repository.suggestTracks("\n").isEmpty())

        coVerify(exactly = 0) { artistDao.searchArtistsByPrefix(any(), any()) }
        coVerify(exactly = 0) { albumDao.searchAlbumsByPrefix(any(), any()) }
        coVerify(exactly = 0) { trackDao.searchTracksByPrefix(any(), any()) }
    }
}
