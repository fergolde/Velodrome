package com.fergolde.velodrome.data.local.dao

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query
import com.fergolde.velodrome.data.local.entity.AlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY title ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums ORDER BY title ASC LIMIT :limit OFFSET :offset")
    suspend fun getAlbumsPage(offset: Int, limit: Int): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE title LIKE '%' || :query || '%' OR artistName LIKE '%' || :query || '%' ORDER BY title ASC")
    suspend fun searchAlbums(query: String): List<AlbumEntity>

    @Upsert
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Query("SELECT MIN(year) FROM albums WHERE year IS NOT NULL AND year > 1950")
    suspend fun getMinYear(): Int?
}