package com.example.velodrome.data.sync

import android.util.Log
import com.example.velodrome.data.local.dao.AlbumDao
import com.example.velodrome.data.local.dao.ArtistDao
import com.example.velodrome.data.local.dao.TrackDao
import com.example.velodrome.data.local.entity.AlbumEntity
import com.example.velodrome.data.local.entity.ArtistEntity
import com.example.velodrome.data.local.entity.TrackEntity
import com.example.velodrome.data.repository.NavidromeRepositoryImpl
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncManager"

@Singleton
class SyncManager @Inject constructor(
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao,
    private val trackDao: TrackDao,
    private val navidromeRepository: NavidromeRepositoryImpl
) {
    suspend fun syncAll(): SyncResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting full sync...")

        try {
            // Sync artists first
            val artistsResult = syncArtists()
            if (artistsResult is SyncResult.Error) {
                return@withContext artistsResult
            }

            // Sync albums
            val albumsResult = syncAlbums()
            if (albumsResult is SyncResult.Error) {
                return@withContext albumsResult
            }

            Log.d(TAG, "Full sync completed successfully")
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun syncArtists(): SyncResult {
        Log.d(TAG, "Syncing artists...")
        var offset = 0
        val pageSize = 500
        var totalSynced = 0

        while (offset < 10000) { // Safety limit
            val result = navidromeRepository.getArtists(offset = offset, size = pageSize)
            val artists = result.getOrNull()
            if (artists == null) {
                return SyncResult.Error(result.exceptionOrNull()?.message ?: "Failed to fetch artists")
            }
            if (artists.isEmpty()) {
                Log.d(TAG, "No more artists to sync at offset $offset")
                break
            }

            val entities = artists.map { it.toEntity() }
            artistDao.insertArtists(entities)
            totalSynced += artists.size
            Log.d(TAG, "Synced ${artists.size} artists (total: $totalSynced)")

            if (artists.size < pageSize) {
                break
            }
            offset += pageSize
        }
        Log.d(TAG, "Artist sync completed: $totalSynced total")
        return SyncResult.Success
    }

    suspend fun syncAlbums(): SyncResult {
        Log.d(TAG, "Syncing albums...")
        var offset = 0
        val pageSize = 500
        var totalSynced = 0

        while (offset < 10000) { // Safety limit
            val result = navidromeRepository.getAllAlbumsFromServer(offset = offset, size = pageSize)
            val albums = result.getOrNull()
            if (albums == null) {
                return SyncResult.Error(result.exceptionOrNull()?.message ?: "Failed to fetch albums")
            }
            if (albums.isEmpty()) {
                Log.d(TAG, "No more albums to sync at offset $offset")
                break
            }

            val entities = albums.map { it.toEntity() }
            albumDao.insertAlbums(entities)
            totalSynced += albums.size
            Log.d(TAG, "Synced ${albums.size} albums (total: $totalSynced)")

            if (albums.size < pageSize) {
                break
            }
            offset += pageSize
        }
        Log.d(TAG, "Album sync completed: $totalSynced total")
        return SyncResult.Success
    }

    suspend fun getStats(): SyncStats {
        return SyncStats(
            artistCount = artistDao.getArtistCount(),
            albumCount = albumDao.getAlbumCount(),
            trackCount = trackDao.getTrackCount()
        )
    }

    private fun Artist.toEntity() = ArtistEntity(
        id = id,
        name = name,
        albumCount = albumCount,
        coverUrl = coverUrl
    )

    private fun Album.toEntity() = AlbumEntity(
        id = id,
        artistId = artistId,
        artistName = artistName,
        title = title,
        year = year,
        genre = genre,
        coverUrl = coverUrl
    )

    private fun Track.toEntity() = TrackEntity(
        id = id,
        albumId = albumId,
        artistName = artistName,
        albumName = albumName,
        title = title,
        durationSec = durationSec,
        trackNumber = trackNumber,
        coverArtId = coverArtId
    )
}

sealed class SyncResult {
    data object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
}

data class SyncStats(
    val artistCount: Int,
    val albumCount: Int,
    val trackCount: Int
)