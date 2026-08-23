package com.fergolde.velodrome.data.local.dao

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query
import com.fergolde.velodrome.data.local.entity.ScrobbleEntity

@Dao
interface ScrobbleDao {
    @Query("SELECT * FROM pending_scrobbles WHERE isSubmitted = 0 ORDER BY timestamp ASC")
    suspend fun getPendingScrobbles(): List<ScrobbleEntity>

    @Upsert
    suspend fun insertScrobble(scrobble: ScrobbleEntity): Long

    @Query("DELETE FROM pending_scrobbles WHERE id = :id")
    suspend fun deleteScrobble(id: Long)

    @Query("DELETE FROM pending_scrobbles WHERE id IN (:ids)")
    suspend fun deleteScrobbles(ids: List<Long>)
}