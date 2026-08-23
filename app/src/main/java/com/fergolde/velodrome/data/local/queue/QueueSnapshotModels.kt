package com.fergolde.velodrome.data.local.queue

import com.fergolde.velodrome.domain.model.Track
import kotlinx.serialization.Serializable

/**
 * Local persistence model for the player queue. Deliberately kept out of the
 * domain layer: Track stays serialization-free.
 */
@Serializable
data class TrackDto(
    val id: String,
    val title: String,
    val artistName: String = "",
    val albumName: String = "",
    val albumId: String = "",
    val durationSec: Int = 0,
    val trackNumber: Int = 0,
    val coverArtId: String? = null
)

@Serializable
data class QueueSnapshot(
    val tracks: List<TrackDto> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0
)

fun Track.toDto(): TrackDto = TrackDto(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    albumId = albumId,
    durationSec = durationSec,
    trackNumber = trackNumber,
    coverArtId = coverArtId
)

fun TrackDto.toDomain(): Track = Track(
    id = id,
    albumId = albumId,
    albumName = albumName,
    artistName = artistName,
    title = title,
    durationSec = durationSec,
    sizeBytes = 0L,
    trackNumber = trackNumber,
    playCount = 0,
    coverArtId = coverArtId
)
