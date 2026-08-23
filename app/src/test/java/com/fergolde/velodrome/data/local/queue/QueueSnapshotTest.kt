package com.fergolde.velodrome.data.local.queue

import com.fergolde.velodrome.domain.model.Track
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueSnapshotTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `snapshot round-trips through json`() {
        val snapshot = QueueSnapshot(
            tracks = listOf(
                Track(id = "t1", albumId = "a1", title = "Song One", artistName = "Artist",
                    albumName = "Album", durationSec = 200, sizeBytes = 5_000_000L,
                    trackNumber = 1, coverArtId = "c1").toDto(),
                Track(id = "t2", albumId = "a1", title = "Song Two",
                    durationSec = 180, sizeBytes = 4_000_000L, trackNumber = 2).toDto()
            ),
            currentIndex = 1,
            positionMs = 42_000
        )

        val raw = json.encodeToString(snapshot)
        val restored = json.decodeFromString<QueueSnapshot>(raw)

        assertEquals(snapshot, restored)
    }

    @Test
    fun `dto maps back to domain with defaults for playback-only fields`() {
        val domain = TrackDto(
            id = "t1", title = "Song", artistName = "A", albumName = "B",
            albumId = "a1", durationSec = 100, trackNumber = 3, coverArtId = "cov"
        ).toDomain()

        assertEquals("t1", domain.id)
        assertEquals("Song", domain.title)
        assertEquals("a1", domain.albumId)
        assertEquals(100, domain.durationSec)
        assertEquals(0L, domain.sizeBytes)      // not persisted, rebuilt later
        assertEquals(0, domain.playCount)       // not persisted
        assertEquals("cov", domain.coverArtId)
    }

    @Test
    fun `corrupt json decodes to null instead of crashing restore`() {
        val result = runCatching {
            json.decodeFromString<QueueSnapshot>("{ not valid")
        }.getOrNull()

        assertNull(result)
    }
}
