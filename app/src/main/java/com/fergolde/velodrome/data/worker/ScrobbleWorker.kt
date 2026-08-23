package com.fergolde.velodrome.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fergolde.velodrome.data.local.dao.ScrobbleDao
import com.fergolde.velodrome.domain.repository.ScrobbleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker que procesa scrobbles pendientes guardados en Room.
 * Se ejecuta cuando hay red disponible.
 */
@HiltWorker
class ScrobbleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scrobbleDao: ScrobbleDao,
    private val scrobbleRepository: ScrobbleRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pending = scrobbleDao.getPendingScrobbles()

            if (pending.isEmpty()) {
                return@withContext Result.success()
            }

            var failedCount = 0
            for (chunk in pending.chunked(BATCH_SIZE)) {
                // One request per chunk: repeated id/time params preserve each
                // track's original listen timestamp.
                val result = scrobbleRepository.scrobbleBatch(
                    ids = chunk.map { it.trackId },
                    times = chunk.map { it.timestamp },
                    submission = true
                )
                if (result.isSuccess) {
                    scrobbleDao.deleteScrobbles(chunk.map { it.id })
                } else {
                    failedCount++
                }
            }

            if (failedCount > 0) {
                return@withContext Result.retry()
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private companion object {
        /** Scrobbles per HTTP request; bounds URL length. */
        const val BATCH_SIZE = 50
    }
}