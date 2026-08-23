package com.fergolde.velodrome.domain.repository

/**
 * Repository interface for scrobble operations.
 */
interface ScrobbleRepository {
    suspend fun scrobble(trackId: String, time: Long? = null, submission: Boolean = true): Result<Unit>

    /**
     * Submits several listens in one request using repeated id/time params,
     * preserving each track's original listen timestamp.
     */
    suspend fun scrobbleBatch(ids: List<String>, times: List<Long>, submission: Boolean = true): Result<Unit>

    suspend fun savePendingScrobble(trackId: String, timestamp: Long)
}