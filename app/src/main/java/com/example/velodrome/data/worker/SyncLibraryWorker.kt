package com.example.velodrome.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.velodrome.domain.repository.AlbumRepository
import com.example.velodrome.domain.repository.ArtistRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager Worker para sincronización de biblioteca en background.
 * Usa los repositorios que encapsulan la lógica de sync (offline-first).
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
            val artistRepository = appEntryPoint.artistRepository()
            val albumRepository = appEntryPoint.albumRepository()

            // Sync artists (el repositorio maneja paginación e inserción)
            val artistsResult = artistRepository.syncArtistsFromServer()
            if (artistsResult.isFailure) {
                Log.e(TAG, "Artist sync failed: ${artistsResult.exceptionOrNull()?.message}")
                return@withContext Result.retry()
            }
            Log.d(TAG, "Artist sync completed: ${artistsResult.getOrNull()} total")

            // Sync albums (el repositorio maneja paginación e inserción)
            val albumsResult = albumRepository.syncAlbumsFromServer()
            if (albumsResult.isFailure) {
                Log.e(TAG, "Album sync failed: ${albumsResult.exceptionOrNull()?.message}")
                return@withContext Result.retry()
            }
            Log.d(TAG, "Album sync completed: ${albumsResult.getOrNull()} total")

            Log.d(TAG, "SyncLibraryWorker: completed successfully")
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
 * Entry point for Hilt injection in Worker.
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun artistRepository(): ArtistRepository
    fun albumRepository(): AlbumRepository
}