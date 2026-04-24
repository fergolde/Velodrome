package com.example.velodrome.data.repository

import android.util.Log
import com.example.velodrome.data.local.VelodromeDatabase
import com.example.velodrome.data.local.entity.ScrobbleEntity
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.domain.repository.ScrobbleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScrobbleRepositoryImpl @Inject constructor(
    private val api: NavidromeApi,
    private val database: VelodromeDatabase
) : ScrobbleRepository {

    override suspend fun scrobble(trackId: String, time: Long?, submission: Boolean): Result<Unit> {
        return runCatching {
            api.scrobble(trackId, time, submission)
            Log.d("ScrobbleRepo", "Scrobbled track: $trackId, submission: $submission")
        }
    }

    override suspend fun savePendingScrobble(trackId: String, timestamp: Long) {
        val entity = ScrobbleEntity(
            trackId = trackId,
            timestamp = timestamp,
            isSubmitted = false
        )
        database.scrobbleDao().insertScrobble(entity)
        Log.d("ScrobbleRepo", "Saved pending scrobble for track: $trackId")
    }
}