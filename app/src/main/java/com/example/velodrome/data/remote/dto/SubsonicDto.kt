package com.example.velodrome.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Root response from Subsonic API in JSON format.
 * {
 *   "subsonic-response": {
 *     "status": "ok",
 *     "version": "1.16.1",
 *     ...
 *   }
 * }
 */
@JsonClass(generateAdapter = true)
data class SubsonicResponse(
    @Json(name = "subsonic-response") val response: SubsonicResponseDto
)

@JsonClass(generateAdapter = true)
data class SubsonicResponseDto(
    val status: String,
    val version: String,
    @Json(name = "artists") val artists: ArtistsDto? = null,
    @Json(name = "artist") val artist: ArtistDetailDto? = null,
    @Json(name = "albumList2") val albumList2: AlbumListDto? = null,
    @Json(name = "album") val album: AlbumDetailDto? = null,
    @Json(name = "searchResult2") val searchResult2: SearchResultDto? = null,
    @Json(name = "genres") val genres: GenresDto? = null,
    @Json(name = "songs") val songs: List<SongDto>? = null,
    @Json(name = "randomSongs") val randomSongs: RandomSongsDto? = null,
    @Json(name = "songsByGenre") val songsByGenre: SongsByGenreDto? = null,
    val error: ErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class RandomSongsDto(
    @Json(name = "song") val song: List<SongDto>? = null
)

@JsonClass(generateAdapter = true)
data class SongsByGenreDto(
    @Json(name = "song") val song: List<SongDto>? = null
)

@JsonClass(generateAdapter = true)
data class ErrorDto(
    val code: Int,
    val message: String
)

// ============ Artists ============

@JsonClass(generateAdapter = true)
data class ArtistsDto(
    @Json(name = "index") val indexes: List<ArtistIndexDto>? = null,
    @Json(name = "artist") val artistList: List<ArtistDto>? = null
)

@JsonClass(generateAdapter = true)
data class ArtistIndexDto(
    val name: String,
    @Json(name = "artist") val artists: List<ArtistDto>
)

@JsonClass(generateAdapter = true)
data class ArtistDto(
    val id: String,
    val name: String,
    @Json(name = "albumCount") val albumCount: Int? = null,
    @Json(name = "coverArt") val coverArt: String? = null,
    @Json(name = "artistImageUrl") val artistImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ArtistDetailDto(
    val id: String,
    val name: String,
    @Json(name = "albumCount") val albumCount: Int? = null,
    @Json(name = "coverArt") val coverArt: String? = null,
    @Json(name = "album") val albums: List<AlbumDto>? = null
)

// ============ Albums ============

@JsonClass(generateAdapter = true)
data class AlbumListDto(
    @Json(name = "album") val albums: List<AlbumDto>? = null
)

@JsonClass(generateAdapter = true)
data class AlbumDto(
    val id: String,
    val name: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "artistId") val artistId: String? = null,
    @Json(name = "coverArt") val coverArt: String? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "genre") val genre: String? = null,
    @Json(name = "songCount") val songCount: Int? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "albumCount") val albumCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class AlbumDetailDto(
    val id: String,
    val name: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "artistId") val artistId: String? = null,
    @Json(name = "coverArt") val coverArt: String? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "genre") val genre: String? = null,
    @Json(name = "songs") val songs: List<SongDto>? = null,
    @Json(name = "songCount") val songCount: Int? = null,
    @Json(name = "duration") val duration: Int? = null
)

// ============ Songs/Tracks ============

@JsonClass(generateAdapter = true)
data class SongDto(
    val id: String,
    val title: String,
    @Json(name = "album") val album: String? = null,
    @Json(name = "albumId") val albumId: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "artistId") val artistId: String? = null,
    @Json(name = "track") val track: Int? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "genre") val genre: String? = null,
    @Json(name = "coverArt") val coverArt: String? = null,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "contentType") val contentType: String? = null,
    @Json(name = "suffix") val suffix: String? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "bitRate") val bitRate: Int? = null,
    @Json(name = "path") val path: String? = null
)

// ============ Search ============

@JsonClass(generateAdapter = true)
data class SearchResultDto(
    @Json(name = "artist") val artists: List<ArtistDto>? = null,
    @Json(name = "album") val albums: List<AlbumDto>? = null,
    @Json(name = "songs") val songs: List<SongDto>? = null
)

// ============ Genres ============

@JsonClass(generateAdapter = true)
data class GenresDto(
    @Json(name = "genre") val genres: List<GenreDto>? = null
)

@JsonClass(generateAdapter = true)
data class GenreDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "value") val value: String? = null,
    @Json(name = "songCount") val songCount: Int? = null,
    @Json(name = "albumCount") val albumCount: Int? = null
)
