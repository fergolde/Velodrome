package com.example.velodrome.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.velodrome.data.local.entity.ScrobbleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrobbleDao {
    @Query("SELECT * FROM pending_scrobbles WHERE isSubmitted = 0 ORDER BY timestamp ASC")
    suspend fun getPendingScrobbles(): List<ScrobbleEntity>

    @Query("SELECT * FROM pending_scrobbles WHERE isSubmitted = 0 ORDER BY timestamp ASC")
    fun getPendingScrobblesFlow(): Flow<List<ScrobbleEntity>>

    @Query("SELECT COUNT(*) FROM pending_scrobbles WHERE isSubmitted = 0")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScrobble(scrobble: ScrobbleEntity): Long

    @Query("UPDATE pending_scrobbles SET isSubmitted = 1 WHERE id = :id")
    suspend fun markAsSubmitted(id: Long)

    @Query("DELETE FROM pending_scrobbles WHERE id = :id")
    suspend fun deleteScrobble(id: Long)

    @Query("DELETE FROM pending_scrobbles WHERE isSubmitted = 1")
    suspend fun deleteSubmitted()
}