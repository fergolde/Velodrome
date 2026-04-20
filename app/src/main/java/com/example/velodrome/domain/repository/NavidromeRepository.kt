package com.example.velodrome.domain.repository

import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.ArtistWithAlbums
import com.example.velodrome.domain.model.AuthResult
import com.example.velodrome.domain.model.Track

interface NavidromeRepository {
    suspend fun login(username: String, password: String, serverUrl: String): Result<AuthResult>
    
    // Check if user has stored credentials
    fun isLoggedIn(): Boolean
    
    // Clear stored credentials
    fun logout()
    
    suspend fun getArtists(offset: Int = 0, size: Int = 50): Result<List<Artist>>
    suspend fun getAlbum(albumId: String): Result<Album>
    suspend fun getTracks(albumId: String): Result<List<Track>>
    suspend fun getStreamUrl(trackId: String): String
    suspend fun search(query: String): Result<List<Artist>>
    
    // Home screen methods
    suspend fun getLatestAlbums(size: Int = 20): Result<List<Album>>
    suspend fun getTopAlbums(size: Int = 20): Result<List<Album>>
    suspend fun getRecentlyPlayedAlbums(size: Int = 20): Result<List<Album>>
    suspend fun getRandomAlbums(size: Int = 20): Result<List<Album>>
    suspend fun getAllAlbums(size: Int = 100): Result<List<Album>>
    suspend fun getAllAlbumsFromServer(offset: Int = 0, size: Int = 100): Result<List<Album>>
    suspend fun getAlbumsByYear(year: Int, size: Int = 20): Result<List<Album>>
    suspend fun getAlbumsByGenre(genre: String, size: Int = 20): Result<List<Album>>
    suspend fun getGenres(): Result<List<String>>
    suspend fun getArtist(artistId: String): Result<ArtistWithAlbums>
    
    // Cache methods
    suspend fun getCacheSize(): Long
    suspend fun setCacheLimit(sizeGb: Int)
    suspend fun getCacheLimit(): Int
    fun getServerUrl(): String
    fun setServerUrl(url: String)

    // Scrobble
    suspend fun scrobble(trackId: String, time: Long? = null, submission: Boolean = true): Result<Unit>

    // Songs by genre
    suspend fun getSongsByGenre(genre: String, count: Int = 50, offset: Int = 0): Result<List<Track>>
    suspend fun getRandomSongsByGenre(genre: String, size: Int = 50): Result<List<Track>>
    suspend fun getRandomSongs(size: Int = 50): Result<List<Track>>
}