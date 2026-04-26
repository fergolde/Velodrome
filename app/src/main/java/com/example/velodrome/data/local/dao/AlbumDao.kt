package com.example.velodrome.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.velodrome.data.local.entity.AlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY title ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums ORDER BY title ASC")
    fun getAlbumsPagingSource(): PagingSource<Int, AlbumEntity>

    @Query("SELECT * FROM albums ORDER BY title ASC LIMIT :limit OFFSET :offset")
    suspend fun getAlbumsPage(offset: Int, limit: Int): List<AlbumEntity>

    @Query("SELECT * FROM albums ORDER BY title ASC")
    suspend fun getAllAlbumsOnce(): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumById(id: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE artistId = :artistId ORDER BY title ASC")
    suspend fun getAlbumsByArtist(artistId: String): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE title LIKE '%' || :query || '%' OR artistName LIKE '%' || :query || '%' ORDER BY title ASC")
    suspend fun searchAlbums(query: String): List<AlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums()

    @Query("SELECT COUNT(*) FROM albums")
    suspend fun getAlbumCount(): Int

    @Query("SELECT MIN(year) FROM albums WHERE year IS NOT NULL AND year > 1950")
    suspend fun getMinYear(): Int?
}