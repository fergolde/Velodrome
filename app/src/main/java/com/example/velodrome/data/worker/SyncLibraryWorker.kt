package com.example.velodrome.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.velodrome.data.local.datasource.LocalMusicDataSource
import com.example.velodrome.data.local.mapper.toEntity
import com.example.velodrome.domain.repository.AlbumRepository
import com.example.velodrome.domain.repository.ArtistRepository
import com.example.velodrome.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
            val lastSyncTimestamp = settingsRepository.lastSyncTimestamp.first()
            val lastSyncOffset = settingsRepository.lastSyncOffset.first()

            if (lastSyncTimestamp > 0) {
                val hasChanges = albumRepository.hasServerChangedSince(lastSyncTimestamp)
                if (!hasChanges) return@withContext Result.success()

                val latestResult = albumRepository.getLatestAlbums(50)
                if (latestResult.isFailure) {
                    return@withContext classifyError(latestResult.exceptionOrNull())
                }

                val latestAlbums = latestResult.getOrNull() ?: emptyList()
                if (latestAlbums.isNotEmpty()) {
                    localMusicDataSource.insertAlbums(latestAlbums.map { it.toEntity() })
                }

                settingsRepository.setLastSyncTimestamp(System.currentTimeMillis())
                return@withContext Result.success()
            }

            val artistsResult = artistRepository.syncArtistsFromServer()
            if (artistsResult.isFailure) {
                return@withContext classifyError(artistsResult.exceptionOrNull())
            }

            val albumsResult = albumRepository.syncAlbumsFromServer(
                startOffset = lastSyncOffset
            ) { newOffset ->
                settingsRepository.setLastSyncOffset(newOffset)
            }
            if (albumsResult.isFailure) {
                return@withContext classifyError(albumsResult.exceptionOrNull())
            }

            settingsRepository.setLastSyncOffset(0)
            settingsRepository.setLastSyncTimestamp(System.currentTimeMillis())
            Result.success()

        } catch (e: Exception) {
            classifyError(e)
        }
    }

    private fun classifyError(e: Throwable?): Result {
        return when {
            e is HttpException && e.code() == 401 -> Result.failure()
            e is HttpException && e.code() == 403 -> Result.failure()
            e is SocketTimeoutException -> Result.retry()
            e is UnknownHostException -> Result.retry()
            e is HttpException -> Result.failure()
            else -> Result.retry()
        }
    }
}