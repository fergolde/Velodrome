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

    /**
     * Sugerencias por prefijo sobre el título o el artista del track. Antes no
     * existía ninguna búsqueda de tracks sobre la biblioteca local. El prefijo
     * va como parámetro bound.
     */
    @Query(
        "SELECT * FROM tracks WHERE title LIKE :prefix || '%' OR artistName LIKE :prefix || '%' " +
            "ORDER BY title ASC LIMIT :limit"
    )
    suspend fun searchTracksByPrefix(prefix: String, limit: Int): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int

    @Query("SELECT * FROM tracks WHERE albumId IN (:albumIds) ORDER BY trackNumber ASC")
    suspend fun getTracksForAlbumIds(albumIds: List<String>): List<TrackEntity>

    @Upsert
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()
}