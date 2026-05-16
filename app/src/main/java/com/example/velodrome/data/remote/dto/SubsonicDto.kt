package com.example.velodrome.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ================= ROOT =================

@JsonClass(generateAdapter = true)
data class SubsonicResponse(
    @param:Json(name = "subsonic-response")
    val response: SubsonicResponseDto
)

@JsonClass(generateAdapter = true)
data class SubsonicResponseDto(
    val status: String,
    val version: String,

    // Artists
    @param:Json(name = "artists") val artists: ArtistsDto? = null,
    @param:Json(name = "artist") val artist: ArtistDetailDto? = null,

    // Albums
    @param:Json(name = "albumList2") val albumList2: AlbumListDto? = null,
    @param:Json(name = "album") val album: AlbumDetailDto? = null,

    // MusicDirectory (TRACKS REALS)
    @param:Json(name = "directory") val directory: DirectoryDto? = null,

    // Search / genres
    @param:Json(name = "searchResult2") val searchResult2: SearchResultDto? = null,
    @param:Json(name = "searchResult3") val searchResult3: SearchResultDto? = null,
    @param:Json(name = "genres") val genres: GenresDto? = null,

    // Songs endpoints (fallbacks reales Subsonic)
    @param:Json(name = "songs") val songs: List<SongDto>? = null,
    @param:Json(name = "randomSongs") val randomSongs: RandomSongsDto? = null,
    @param:Json(name = "songsByGenre") val songsByGenre: SongsByGenreDto? = null,
    @param:Json(name = "topSongs") val topSongs: TopSongsDto? = null,

    val error: ErrorDto? = null
)

// ================= MUSIC DIRECTORY (CLAVE) =================

@JsonClass(generateAdapter = true)
data class DirectoryDto(
    @param:Json(name = "child")
    val child: List<SongDto>? = null
)

// ================= ERROR =================

@JsonClass(generateAdapter = true)
data class ErrorDto(
    val code: Int,
    val message: String
)

// ================= ARTISTS =================

@JsonClass(generateAdapter = true)
data class ArtistsDto(
    @param:Json(name = "index") val indexes: List<ArtistIndexDto>? = null,
    @param:Json(name = "artist") val artistList: List<ArtistDto>? = null
)

@JsonClass(generateAdapter = true)
data class ArtistIndexDto(
    val name: String,
    @param:Json(name = "artist") val artists: List<ArtistDto>
)

@JsonClass(generateAdapter = true)
data class ArtistDto(
    val id: String,
    val name: String,
    @param:Json(name = "albumCount") val albumCount: Int? = null,
    @param:Json(name = "coverArt") val coverArt: String? = null,
    @param:Json(name = "artistImageUrl") val artistImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ArtistDetailDto(
    val id: String,
    val name: String,
    @param:Json(name = "albumCount") val albumCount: Int? = null,
    @param:Json(name = "coverArt") val coverArt: String? = null,
    @param:Json(name = "album") val albums: List<AlbumDto>? = null
)

// ================= ALBUMS =================

@JsonClass(generateAdapter = true)
data class AlbumListDto(
    @param:Json(name = "album") val albums: List<AlbumDto>? = null
)

@JsonClass(generateAdapter = true)
data class AlbumDto(
    val id: String,
    val name: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "artist") val artist: String? = null,
    @param:Json(name = "artistId") val artistId: String? = null,
    @param:Json(name = "coverArt") val coverArt: String? = null,
    @param:Json(name = "year") val year: Int? = null,
    @param:Json(name = "genre") val genre: String? = null,
    @param:Json(name = "songCount") val songCount: Int? = null,
    @param:Json(name = "duration") val duration: Int? = null,
    @param:Json(name = "albumCount") val albumCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class AlbumDetailDto(
    val id: String,
    val name: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "artist") val artist: String? = null,
    @param:Json(name = "artistId") val artistId: String? = null,
    @param:Json(name = "coverArt") val coverArt: String? = null,
    @param:Json(name = "year") val year: Int? = null,
    @param:Json(name = "genre") val genre: String? = null,
    @param:Json(name = "song") val songs: List<SongDto>? = null,
    @param:Json(name = "songCount") val songCount: Int? = null,
    @param:Json(name = "duration") val duration: Int? = null
)

// ================= SONGS =================

@JsonClass(generateAdapter = true)
data class SongDto(
    val id: String,
    val title: String,
    val isDir: Boolean? = null,

    @param:Json(name = "album") val album: String? = null,
    @param:Json(name = "albumId") val albumId: String? = null,
    @param:Json(name = "artist") val artist: String? = null,
    @param:Json(name = "artistId") val artistId: String? = null,

    @param:Json(name = "track") val track: Int? = null,
    @param:Json(name = "year") val year: Int? = null,
    @param:Json(name = "genre") val genre: String? = null,
    @param:Json(name = "coverArt") val coverArt: String? = null,

    @param:Json(name = "size") val size: Long? = null,
    @param:Json(name = "contentType") val contentType: String? = null,
    @param:Json(name = "suffix") val suffix: String? = null,
    @param:Json(name = "duration") val duration: Int? = null,
    @param:Json(name = "bitRate") val bitRate: Int? = null,
    @param:Json(name = "playCount") val playCount: Int? = null,
    @param:Json(name = "path") val path: String? = null
)

// ================= SEARCH =================

@JsonClass(generateAdapter = true)
data class SearchResultDto(
    @param:Json(name = "artist") val artists: List<ArtistDto>? = null,
    @param:Json(name = "album") val albums: List<AlbumDto>? = null,
    @param:Json(name = "song") val songs: List<SongDto>? = null
)

// ================= GENRES =================

@JsonClass(generateAdapter = true)
data class GenresDto(
    @param:Json(name = "genre") val genres: List<GenreDto>? = null
)

@JsonClass(generateAdapter = true)
data class GenreDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "value") val value: String? = null,
    @param:Json(name = "songCount") val songCount: Int? = null,
    @param:Json(name = "albumCount") val albumCount: Int? = null
)

// ================= RANDOM / BY GENRE =================

@JsonClass(generateAdapter = true)
data class RandomSongsDto(
    @param:Json(name = "song") val song: List<SongDto>? = null
)

@JsonClass(generateAdapter = true)
data class SongsByGenreDto(
    @param:Json(name = "song") val song: List<SongDto>? = null
)

// ================= TOP SONGS =================

@JsonClass(generateAdapter = true)
data class TopSongsDto(
    @param:Json(name = "song") val song: List<SongDto>? = null
)