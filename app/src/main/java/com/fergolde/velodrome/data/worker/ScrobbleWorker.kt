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
            for (scrobble in pending) {
                val result = scrobbleRepository.scrobble(scrobble.trackId, scrobble.timestamp, submission = true)
                if (result.isSuccess) {
                    scrobbleDao.deleteScrobble(scrobble.id)
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
}