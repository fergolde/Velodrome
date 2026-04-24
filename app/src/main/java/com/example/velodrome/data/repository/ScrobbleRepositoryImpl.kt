package com.example.velodrome.data.repository

import android.util.Log
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.domain.repository.ScrobbleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScrobbleRepositoryImpl @Inject constructor(
    private val api: NavidromeApi
) : ScrobbleRepository {

    override suspend fun scrobble(trackId: String, time: Long?, submission: Boolean): Result<Unit> {
        return runCatching {
            api.scrobble(trackId, time, submission)
            Log.d("ScrobbleRepo", "Scrobbled track: $trackId, submission: $submission")
        }
    }
}