package com.example.velodrome.domain.repository

import com.example.velodrome.domain.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for track/song operations.
 */
interface TrackRepository {
    fun observeTracksByAlbum(albumId: String): Flow<List<Track>>
    suspend fun syncTracksForAlbum(albumId: String): Result<Unit>
    suspend fun getStreamUrl(trackId: String): String
    suspend fun getSongsByGenre(genre: String, count: Int = 50, offset: Int = 0): Result<List<Track>>
    suspend fun getRandomSongsByGenre(genre: String, size: Int = 50): Result<List<Track>>
    suspend fun getRandomSongs(size: Int = 50): Result<List<Track>>
    suspend fun getRandomSongs(size: Int = 50, fromYear: Int? = null, toYear: Int? = null): Result<List<Track>>
    suspend fun searchRemoteTracks(query: String): Result<List<Track>>
}