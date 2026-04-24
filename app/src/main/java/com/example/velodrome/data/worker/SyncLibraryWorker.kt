package com.example.velodrome.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.velodrome.domain.repository.AlbumRepository
import com.example.velodrome.domain.repository.ArtistRepository
import com.example.velodrome.domain.repository.SettingsRepository
import com.example.velodrome.data.local.datasource.LocalMusicDataSource
import com.example.velodrome.data.local.mapper.toEntity
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * WorkManager Worker para sincronización de biblioteca en background.
 * Implementa Smart Sync: detección de cambios + resume interrumpido.
 */
class SyncLibraryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "SyncLibraryWorker: starting background sync...")

        try {
            // Get dependencies via Hilt manually (Worker cannot use constructor injection)
            val appEntryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                WorkerEntryPoint::class.java
            )
            val settingsRepository = appEntryPoint.settingsRepository()
            val artistRepository = appEntryPoint.artistRepository()
            val albumRepository = appEntryPoint.albumRepository()
            val localMusicDataSource = appEntryPoint.localMusicDataSource()

            // Read sync state
            val lastSyncTimestamp = settingsRepository.lastSyncTimestamp.first()
            val lastSyncOffset = settingsRepository.lastSyncOffset.first()
            Log.d(TAG, "Sync state: timestamp=$lastSyncTimestamp, offset=$lastSyncOffset")

            // CASE 1: Incremental sync (we have done a full sync before)
            if (lastSyncTimestamp > 0) {
                Log.d(TAG, "Mode: Incremental sync")

                val hasChanges = albumRepository.hasServerChangedSince(lastSyncTimestamp)
                if (!hasChanges) {
                    Log.d(TAG, "No changes on server")
                    return@withContext Result.success()
                }

                // Server has changes, fetch latest albums
                val latestResult = albumRepository.getLatestAlbums(50)
                if (latestResult.isFailure) {
                    Log.e(TAG, "Failed to get latest albums: ${latestResult.exceptionOrNull()?.message}")
                    return@withContext Result.retry()
                }

                // Save to local DB
                val latestAlbums = latestResult.getOrNull() ?: emptyList()
                if (latestAlbums.isNotEmpty()) {
                    // Mapear los modelos de dominio a entidades de base de datos
                    val entities = latestAlbums.map { it.toEntity() }

                    // Insertar en Room (el DAO usa OnConflictStrategy.REPLACE)
                    localMusicDataSource.insertAlbums(entities)

                    Log.d(TAG, "Inserted ${entities.size} latest albums into local DB")
                }

                // Update timestamp
                settingsRepository.setLastSyncTimestamp(System.currentTimeMillis())
                Log.d(TAG, "Incremental sync completed")
                return@withContext Result.success()
            }

            // CASE 2: Full sync (first time or recovery)
            Log.d(TAG, "Mode: Full sync (resuming from offset=$lastSyncOffset)")

            // Sync artists (always full, no pagination needed for artists in current impl)
            val artistsResult = artistRepository.syncArtistsFromServer()
            if (artistsResult.isFailure) {
                Log.e(TAG, "Artist sync failed: ${artistsResult.exceptionOrNull()?.message}")
                return@withContext Result.retry()
            }
            Log.d(TAG, "Artist sync completed: ${artistsResult.getOrNull()} total")

            // Sync albums with pagination and resume capability
            val albumsResult = albumRepository.syncAlbumsFromServer(
                startOffset = lastSyncOffset
            ) { newOffset ->
                // Callback to save offset after each page
                Log.d(TAG, "Page processed, saving offset: $newOffset")
                settingsRepository.setLastSyncOffset(newOffset)
            }

            if (albumsResult.isFailure) {
                Log.e(TAG, "Album sync failed: ${albumsResult.exceptionOrNull()?.message}")
                return@withContext Result.retry()
            }
            Log.d(TAG, "Album sync completed: ${albumsResult.getOrNull()} total")

            // Full sync complete: reset offset and set timestamp
            settingsRepository.setLastSyncOffset(0)
            settingsRepository.setLastSyncTimestamp(System.currentTimeMillis())
            Log.d(TAG, "Full sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncLibraryWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncLibraryWorker"
    }
}

/**
 * Entry points for Hilt injection in Workers.
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun settingsRepository(): SettingsRepository
    fun artistRepository(): ArtistRepository
    fun albumRepository(): AlbumRepository
    fun localMusicDataSource(): LocalMusicDataSource
}