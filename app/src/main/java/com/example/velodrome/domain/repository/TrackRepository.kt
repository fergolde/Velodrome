package com.example.velodrome.domain.repository

import com.example.velodrome.domain.model.Track

/**
 * Repository interface for track/song operations.
 */
interface TrackRepository {
    suspend fun getTracks(albumId: String): Result<List<Track>>
    suspend fun getStreamUrl(trackId: String): String
    suspend fun getSongsByGenre(genre: String, count: Int = 50, offset: Int = 0): Result<List<Track>>
    suspend fun getRandomSongsByGenre(genre: String, size: Int = 50): Result<List<Track>>
    suspend fun getRandomSongs(size: Int = 50): Result<List<Track>>
}