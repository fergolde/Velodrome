package com.fergolde.velodrome.domain.repository

import com.fergolde.velodrome.domain.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for track/song operations.
 */
interface TrackRepository {
    fun observeTracksByAlbum(albumId: String): Flow<List<Track>>
    suspend fun syncTracksForAlbum(albumId: String): Result<Unit>
    suspend fun getRandomSongs(size: Int = 50, genre: String? = null, fromYear: Int? = null, toYear: Int? = null): Result<List<Track>>
    suspend fun searchRemoteTracks(query: String): Result<List<Track>>
    suspend fun getOfflineTracks(): List<Track>
    suspend fun getTopGlobalTracks(size: Int = 100): Result<List<Track>>
}