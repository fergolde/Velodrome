package com.fergolde.velodrome.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fergolde.velodrome.data.local.datasource.LocalMusicDataSource
import com.fergolde.velodrome.data.local.mapper.toEntity
import com.fergolde.velodrome.domain.repository.AlbumRepository
import com.fergolde.velodrome.domain.repository.ArtistRepository
import com.fergolde.velodrome.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncLibraryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val localMusicDataSource: LocalMusicDataSource
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val forceFullSync = inputData.getBoolean(KEY_FORCE_FULL_SYNC, false)
            val lastSyncTimestamp = if (forceFullSync) {
                settingsRepository.setLastSyncTimestamp(0)
                settingsRepository.setLastSyncOffset(0)
                0L
            } else {
                settingsRepository.lastSyncTimestamp.first()
            }
            val lastSyncOffset = settingsRepository.lastSyncOffset.first()

            if (lastSyncTimestamp > 0) {
                // Throttle: the lightweight "has server changed?" probe runs at most
                // once per SYNC_CHECK_INTERVAL_MS. Within the window, an app open
                // costs zero network. A failed probe leaves the stamp untouched so
                // it retries on the next open.
                val now = System.currentTimeMillis()
                val lastCheckAt = settingsRepository.lastServerCheckAt.first()
                if (now - lastCheckAt < SYNC_CHECK_INTERVAL_MS) {
                    return@withContext Result.success()
                }

                val hasChanges = albumRepository.hasServerChangedSince(lastSyncTimestamp)
                if (!hasChanges) {
                    settingsRepository.setLastServerCheckAt(now)
                    return@withContext Result.success()
                }

                val latestResult = albumRepository.getLatestAlbums(50)
                if (latestResult.isFailure) {
                    return@withContext classifyError(latestResult.exceptionOrNull())
                }

                val latestAlbums = latestResult.getOrNull() ?: emptyList()
                if (latestAlbums.isNotEmpty()) {
                    localMusicDataSource.insertAlbums(latestAlbums.map { it.toEntity() })
                }

                settingsRepository.setLastSyncTimestamp(System.currentTimeMillis())
                settingsRepository.setLastServerCheckAt(now)
                return@withContext Result.success()
            }

            // Artists and albums are independent tables/endpoints: fetch both
            // streams concurrently so the initial sync costs the slower stream
            // instead of the sum of both. Final stamps only land if BOTH succeed;
            // otherwise lastSyncTimestamp stays 0 and the next open retries the
            // full sync (re-upserting already-inserted rows is idempotent).
            coroutineScope {
                val artistsDeferred = async { artistRepository.syncArtistsFromServer() }
                val albumsDeferred = async {
                    albumRepository.syncAlbumsFromServer(
                        startOffset = lastSyncOffset
                    ) { newOffset ->
                        settingsRepository.setLastSyncOffset(newOffset)
                    }
                }

                val artistsResult = artistsDeferred.await()
                val albumsResult = albumsDeferred.await()

                if (artistsResult.isFailure) {
                    return@coroutineScope classifyError(artistsResult.exceptionOrNull())
                }
                if (albumsResult.isFailure) {
                    return@coroutineScope classifyError(albumsResult.exceptionOrNull())
                }

                settingsRepository.setLastSyncOffset(0)
                settingsRepository.setLastSyncTimestamp(System.currentTimeMillis())
                settingsRepository.setLastServerCheckAt(System.currentTimeMillis())
                Result.success()
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            classifyError(e)
        }
    }

    companion object {
        /** Minimum gap between lightweight server-change probes (3 hours). */
        private const val SYNC_CHECK_INTERVAL_MS = 3L * 60 * 60 * 1000

        private const val KEY_FORCE_FULL_SYNC = "force_full_sync"

        const val WORK_NAME_IMMEDIATE = "sync_library_immediate"
        const val WORK_NAME_PERIODIC = "sync_library_periodic"

        private fun networkConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncLibraryWorker>()
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME_IMMEDIATE, ExistingWorkPolicy.KEEP, request)
        }

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncLibraryWorker>(24, TimeUnit.HOURS)
                .setInputData(workDataOf(KEY_FORCE_FULL_SYNC to true))
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }

    private fun classifyError(e: Throwable?): Result {
        return when (e) {
            is HttpException if e.code() == 401 -> Result.failure()
            is HttpException if e.code() == 403 -> Result.failure()
            is SocketTimeoutException -> Result.retry()
            is UnknownHostException -> Result.retry()
            is HttpException -> Result.failure()
            else -> Result.retry()
        }
    }
}