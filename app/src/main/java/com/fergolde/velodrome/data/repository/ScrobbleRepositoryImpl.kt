package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.ScrobbleDao
import com.fergolde.velodrome.data.local.entity.ScrobbleEntity
import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.domain.repository.ScrobbleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScrobbleRepositoryImpl @Inject constructor(
    private val api: NavidromeApi,
    private val scrobbleDao: ScrobbleDao
) : ScrobbleRepository {

    override suspend fun scrobble(trackId: String, time: Long?, submission: Boolean): Result<Unit> {
        return runCatching {
            api.scrobble(
                trackIds = listOf(trackId),
                times = time?.let { listOf(it) },
                submission = submission
            )
        }
    }

    override suspend fun scrobbleBatch(ids: List<String>, times: List<Long>, submission: Boolean): Result<Unit> {
        require(ids.size == times.size) { "ids and times must be parallel lists" }
        return runCatching {
            api.scrobble(trackIds = ids, times = times, submission = submission)
        }
    }

    override suspend fun savePendingScrobble(trackId: String, timestamp: Long) {
        val entity = ScrobbleEntity(
            trackId = trackId,
            timestamp = timestamp,
            isSubmitted = false
        )
        scrobbleDao.insertScrobble(entity)
    }
}