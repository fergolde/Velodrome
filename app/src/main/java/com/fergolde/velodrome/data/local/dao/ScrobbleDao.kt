package com.fergolde.velodrome.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fergolde.velodrome.data.local.entity.ScrobbleEntity

@Dao
interface ScrobbleDao {
    @Query("SELECT * FROM pending_scrobbles WHERE isSubmitted = 0 ORDER BY timestamp ASC")
    suspend fun getPendingScrobbles(): List<ScrobbleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScrobble(scrobble: ScrobbleEntity): Long

    @Query("DELETE FROM pending_scrobbles WHERE id = :id")
    suspend fun deleteScrobble(id: Long)
}