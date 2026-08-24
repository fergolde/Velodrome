package com.fergolde.velodrome.domain.model

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int,
    val coverUrl: String?
)

data class Album(
    val id: String,
    val artistId: String,
    val artistName: String,
    val title: String,
    val year: Int?,
    val genre: String?,
    val coverUrl: String?
)

data class Track(
    val id: String,
    val albumId: String,
    val albumName: String = "",
    val artistName: String = "",
    val title: String,
    val durationSec: Int,
    val sizeBytes: Long,
    val trackNumber: Int,
    val playCount: Int = 0,
    val coverArtId: String? = null
)

data class AuthResult(
    val success: Boolean,
    val error: String? = null
)

data class ArtistWithAlbums(
    val artist: Artist,
    val albums: List<Album>
)

data class Playlist(
    val id: String,
    val name: String,
    val songCount: Int = 0,
    val coverArtId: String? = null,
    val tracks: List<Track> = emptyList()
)