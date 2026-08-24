package com.fergolde.velodrome.data.local.dao

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query
import com.fergolde.velodrome.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    suspend fun getAllTracksOnce(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query(
        "SELECT albums.genre FROM tracks " +
            "INNER JOIN albums ON tracks.albumId = albums.id " +
            "WHERE tracks.id = :trackId LIMIT 1"
    )
    suspend fun findGenreByTrackId(trackId: String): String?

    @Query("SELECT * FROM tracks WHERE albumId = :albumId ORDER BY trackNumber ASC")
    fun observeTracksByAlbum(albumId: String): Flow<List<TrackEntity>>

    @Upsert
    suspend fun insertTracks(tracks: List<TrackEntity>)
}