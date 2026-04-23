package com.example.velodrome.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.velodrome.data.local.dao.AlbumDao
import com.example.velodrome.data.local.dao.ArtistDao
import com.example.velodrome.data.repository.NavidromeRepositoryImpl
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager Worker para sincronización de biblioteca en background.
 * Maneja artists y albums con paginación (while offset < 10000).
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
            val artistDao = appEntryPoint.artistDao()
            val albumDao = appEntryPoint.albumDao()
            val navidromeRepository = appEntryPoint.navidromeRepository()

            // Sync artists
            val artistsResult = syncArtists(artistDao, navidromeRepository)
            if (artistsResult is SyncResult.Error) {
                Log.e(TAG, "Artist sync failed: ${artistsResult.message}")
                return@withContext Result.retry()
            }

            // Sync albums
            val albumsResult = syncAlbums(albumDao, navidromeRepository)
            if (albumsResult is SyncResult.Error) {
                Log.e(TAG, "Album sync failed: ${albumsResult.message}")
                return@withContext Result.retry()
            }

            Log.d(TAG, "SyncLibraryWorker: completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncLibraryWorker failed", e)
            Result.retry()
        }
    }

    private suspend fun syncArtists(
        artistDao: ArtistDao,
        navidromeRepository: NavidromeRepositoryImpl
    ): SyncResult {
        Log.d(TAG, "Syncing artists...")
        var offset = 0
        val pageSize = 500
        var totalSynced = 0

        while (offset < 10000) {
            val result = navidromeRepository.getArtists(offset = offset, size = pageSize)
            val artists = result.getOrNull()
            if (artists == null) {
                return SyncResult.Error(result.exceptionOrNull()?.message ?: "Failed to fetch artists")
            }
            if (artists.isEmpty()) {
                Log.d(TAG, "No more artists at offset $offset")
                break
            }

            val entities = artists.map { it.toEntity() }
            artistDao.insertArtists(entities)
            totalSynced += artists.size
            Log.d(TAG, "Synced ${artists.size} artists (total: $totalSynced)")

            if (artists.size < pageSize) break
            offset += pageSize
        }
        Log.d(TAG, "Artist sync completed: $totalSynced total")
        return SyncResult.Success
    }

    private suspend fun syncAlbums(
        albumDao: AlbumDao,
        navidromeRepository: NavidromeRepositoryImpl
    ): SyncResult {
        Log.d(TAG, "Syncing albums...")
        var offset = 0
        val pageSize = 500
        var totalSynced = 0

        while (offset < 10000) {
            val result = navidromeRepository.getAllAlbumsFromServer(offset = offset, size = pageSize)
            val albums = result.getOrNull()
            if (albums == null) {
                return SyncResult.Error(result.exceptionOrNull()?.message ?: "Failed to fetch albums")
            }
            if (albums.isEmpty()) {
                Log.d(TAG, "No more albums at offset $offset")
                break
            }

            val entities = albums.map { it.toEntity() }
            albumDao.insertAlbums(entities)
            totalSynced += albums.size
            Log.d(TAG, "Synced ${albums.size} albums (total: $totalSynced)")

            if (albums.size < pageSize) break
            offset += pageSize
        }
        Log.d(TAG, "Album sync completed: $totalSynced total")
        return SyncResult.Success
    }

    private fun Artist.toEntity() = com.example.velodrome.data.local.entity.ArtistEntity(
        id = id,
        name = name,
        albumCount = albumCount,
        coverUrl = coverUrl
    )

    private fun Album.toEntity() = com.example.velodrome.data.local.entity.AlbumEntity(
        id = id,
        artistId = artistId,
        artistName = artistName,
        title = title,
        year = year,
        genre = genre,
        coverUrl = coverUrl
    )

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
    fun artistDao(): com.example.velodrome.data.local.dao.ArtistDao
    fun albumDao(): com.example.velodrome.data.local.dao.AlbumDao
    fun navidromeRepository(): com.example.velodrome.data.repository.NavidromeRepositoryImpl
}

sealed class SyncResult {
    data object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
}