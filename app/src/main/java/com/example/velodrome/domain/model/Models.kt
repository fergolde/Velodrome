package com.example.velodrome.domain.model

data class User(
    val username: String,
    val token: String,
    val serverUrl: String
)

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
    val bitrate: Int,
    val trackNumber: Int,
    val isCached: Boolean = false,
    val coverArtId: String? = null
)

data class AuthResult(
    val success: Boolean,
    val token: String? = null,
    val error: String? = null
)

data class ArtistWithAlbums(
    val artist: Artist,
    val albums: List<Album>
)