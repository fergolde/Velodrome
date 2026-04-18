package com.example.velodrome.domain.repository

import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
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
    suspend fun getAlbumsByYear(year: Int, size: Int = 20): Result<List<Album>>
    suspend fun getAlbumsByGenre(genre: String, size: Int = 20): Result<List<Album>>
    suspend fun getGenres(): Result<List<String>>
    
    // Cache methods
    suspend fun getCacheSize(): Long
    suspend fun setCacheLimit(sizeGb: Int)
    suspend fun getCacheLimit(): Int
    fun getServerUrl(): String
    fun setServerUrl(url: String)
}