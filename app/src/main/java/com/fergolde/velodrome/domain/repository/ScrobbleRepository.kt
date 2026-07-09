package com.fergolde.velodrome.domain.repository

/**
 * Repository interface for scrobble operations.
 */
interface ScrobbleRepository {
    suspend fun scrobble(trackId: String, time: Long? = null, submission: Boolean = true): Result<Unit>
    suspend fun savePendingScrobble(trackId: String, timestamp: Long)
}