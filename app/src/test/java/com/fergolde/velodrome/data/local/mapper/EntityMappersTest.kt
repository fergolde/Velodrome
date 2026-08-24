package com.fergolde.velodrome.data.local.mapper

import com.fergolde.velodrome.data.local.entity.AlbumEntity
import com.fergolde.velodrome.data.local.entity.ArtistEntity
import com.fergolde.velodrome.data.local.entity.TrackEntity
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.Track
import org.junit.Assert.*
import org.junit.Test

class EntityMappersTest {

    // ========= ARTIST =========

    @Test
    fun artistEntity_toDomain_mapsAllFields() {
        val entity = ArtistEntity(id = "1", name = "Test Artist", albumCount = 5, coverUrl = "art-1")
        val domain = entity.toDomain()
        assertEquals("1", domain.id)
        assertEquals("Test Artist", domain.name)
        assertEquals(5, domain.albumCount)
        assertEquals("art-1", domain.coverUrl)
    }

    @Test
    fun artistEntity_toDomain_nullCoverUrl() {
        val entity = ArtistEntity(id = "2", name = "No Cover", albumCount = 0, coverUrl = null)
        val domain = entity.toDomain()
        assertNull(domain.coverUrl)
    }

    @Test
    fun artist_toEntity_mapsAllFields() {
        val domain = Artist(id = "1", name = "Test Artist", albumCount = 5, coverUrl = "art-1")
        val entity = domain.toEntity()
        assertEquals("1", entity.id)
        assertEquals("Test Artist", entity.name)
        assertEquals(5, entity.albumCount)
        assertEquals("art-1", entity.coverUrl)
    }

    @Test
    fun artist_roundTrip() {
        val original = Artist(id = "a1", name = "Artist", albumCount = 3, coverUrl = "cov-1")
        val entity = original.toEntity()
        val restored = entity.toDomain()
        assertEquals(original, restored)
    }

    // ========= ALBUM =========

    @Test
    fun albumEntity_toDomain_mapsAllFields() {
        val entity = AlbumEntity(
            id = "a1", artistId = "art1", artistName = "Artist",
            title = "Album Title", year = 2020, genre = "Rock",
            coverUrl = "cov-1"
        )
        val domain = entity.toDomain()
        assertEquals("a1", domain.id)
        assertEquals("art1", domain.artistId)
        assertEquals("Artist", domain.artistName)
        assertEquals("Album Title", domain.title)
        assertEquals(2020, domain.year)
        assertEquals("Rock", domain.genre)
        assertEquals("cov-1", domain.coverUrl)
    }

    @Test
    fun albumEntity_toDomain_nullableFields() {
        val entity = AlbumEntity(
            id = "a2", artistId = "art2", artistName = "A",
            title = "T", year = null, genre = null, coverUrl = null
        )
        val domain = entity.toDomain()
        assertNull(domain.year)
        assertNull(domain.genre)
        assertNull(domain.coverUrl)
    }

    @Test
    fun album_roundTrip() {
        val original = Album(
            id = "a1", artistId = "art1", artistName = "Artist",
            title = "Album", year = 2020, genre = "Jazz",
            coverUrl = "cov-1"
        )
        val entity = original.toEntity()
        val restored = entity.toDomain()
        assertEquals(original, restored)
    }

    // ========= TRACK =========

    @Test
    fun trackEntity_toDomain_mapsAllFields() {
        val entity = TrackEntity(
            id = "t1", albumId = "a1", artistName = "Artist",
            albumName = "Album", title = "Track 1",
            durationSec = 180, trackNumber = 1, coverArtId = "cov-1",
            sizeBytes = 5000000L, playCount = 5
        )
        val domain = entity.toDomain()
        assertEquals("t1", domain.id)
        assertEquals("a1", domain.albumId)
        assertEquals("Artist", domain.artistName)
        assertEquals("Album", domain.albumName)
        assertEquals("Track 1", domain.title)
        assertEquals(180, domain.durationSec)
        assertEquals(1, domain.trackNumber)
        assertEquals("cov-1", domain.coverArtId)
        assertEquals(5000000L, domain.sizeBytes)
        assertEquals(5, domain.playCount)
    }

    @Test
    fun trackEntity_toDomain_nullCoverArt() {
        val entity = TrackEntity(
            id = "t2", albumId = "a1", artistName = "A",
            albumName = "B", title = "T", durationSec = 120,
            trackNumber = 2, coverArtId = null
        )
        val domain = entity.toDomain()
        assertNull(domain.coverArtId)
    }

    @Test
    fun track_roundTrip_preservesLocalFields() {
        val original = Track(
            id = "t1", albumId = "a1", albumName = "Album",
            artistName = "Artist", title = "Track 1", durationSec = 180,
            sizeBytes = 5000000L, trackNumber = 1,
            playCount = 7,
            coverArtId = "cov-1"
        )
        val entity = original.toEntity()
        assertEquals("t1", entity.id)
        assertEquals("a1", entity.albumId)
        assertEquals("Artist", entity.artistName)
        assertEquals("Track 1", entity.title)
        assertEquals(180, entity.durationSec)
        assertEquals(1, entity.trackNumber)
        assertEquals("cov-1", entity.coverArtId)
        assertEquals(7, entity.playCount)
        // Full round-trip: resync must not erase persisted fields
        assertEquals(original, entity.toDomain())
    }
}
