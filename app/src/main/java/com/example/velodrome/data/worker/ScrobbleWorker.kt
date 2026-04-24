package com.example.velodrome.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.velodrome.data.local.dao.ScrobbleDao
import com.example.velodrome.domain.repository.ScrobbleRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker que procesa scrobbles pendientes guardados en Room.
 * Se ejecuta cuando hay red disponible.
 */
class ScrobbleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "ScrobbleWorker: starting...")

        try {
            val appEntryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ScrobbleWorkerEntryPoint::class.java
            )
            val scrobbleDao = appEntryPoint.scrobbleDao()
            val scrobbleRepository = appEntryPoint.scrobbleRepository()

            val pending = scrobbleDao.getPendingScrobbles()
            Log.d(TAG, "Found ${pending.size} pending scrobbles")

            if (pending.isEmpty()) {
                return@withContext Result.success()
            }

            var failedCount = 0
            for (scrobble in pending) {
                val result = scrobbleRepository.scrobble(scrobble.trackId, scrobble.timestamp, submission = true)
                if (result.isSuccess) {
                    scrobbleDao.deleteScrobble(scrobble.id)
                    Log.d(TAG, "Submitted scrobble for track: ${scrobble.trackId}")
                } else {
                    failedCount++
                    Log.w(TAG, "Failed to submit scrobble ${scrobble.id}: ${result.exceptionOrNull()?.message}")
                }
            }

            if (failedCount > 0) {
                Log.w(TAG, "$failedCount scrobbles failed, will retry")
                return@withContext Result.retry()
            }

            Log.d(TAG, "ScrobbleWorker: completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ScrobbleWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ScrobbleWorker"
    }
}

/**
 * Entry point for Hilt injection in ScrobbleWorker.
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(SingletonComponent::class)
interface ScrobbleWorkerEntryPoint {
    fun scrobbleDao(): ScrobbleDao
    fun scrobbleRepository(): ScrobbleRepository
}