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

/**
 * WorkManager Worker para sincronización de biblioteca en background.
 * Implementa Smart Sync: detección de cambios + resume interrumpido.
 */
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
            // Read sync state
            val lastSyncTimestamp = settingsRepository.lastSyncTimestamp.first()
            val lastSyncOffset = settingsRepository.lastSyncOffset.first()

            // CASE 1: Incremental sync (we have done a full sync before)
            if (lastSyncTimestamp > 0) {
                val hasChanges = albumRepository.hasServerChangedSince(lastSyncTimestamp)
                if (!hasChanges) {
                    return@withContext Result.success()
                }

                // Server has changes, fetch latest albums
                val latestResult = albumRepository.getLatestAlbums(50)
                if (latestResult.isFailure) {
                    return@withContext Result.retry()
                }

                // Save to local DB
                val latestAlbums = latestResult.getOrNull() ?: emptyList()
                if (latestAlbums.isNotEmpty()) {
                    // Mapear los modelos de dominio a entidades de base de datos
                    val entities = latestAlbums.map { it.toEntity() }

                    // Insertar en Room (el DAO usa OnConflictStrategy.REPLACE)
                    localMusicDataSource.insertAlbums(entities)
                }

                // Update timestamp
                settingsRepository.setLastSyncTimestamp(System.currentTimeMillis())
                return@withContext Result.success()
            }

            // CASE 2: Full sync (first time or recovery)
            // Sync artists (always full, no pagination needed for artists in current impl)
            val artistsResult = artistRepository.syncArtistsFromServer()
            if (artistsResult.isFailure) {
                return@withContext Result.retry()
            }

            // Sync albums with pagination and resume capability
            val albumsResult = albumRepository.syncAlbumsFromServer(
                startOffset = lastSyncOffset
            ) { newOffset ->
                // Callback to save offset after each page
                settingsRepository.setLastSyncOffset(newOffset)
            }

            if (albumsResult.isFailure) {
                return@withContext Result.retry()
            }

            // Full sync complete: reset offset and set timestamp
            settingsRepository.setLastSyncOffset(0)
            settingsRepository.setLastSyncTimestamp(System.currentTimeMillis())
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}