package com.fergolde.velodrome.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fergolde.velodrome.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getAllArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getArtistsPagingSource(): PagingSource<Int, ArtistEntity>

    @Query("SELECT * FROM artists ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getArtistsPage(offset: Int, limit: Int): List<ArtistEntity>

    @Query("SELECT * FROM artists ORDER BY name ASC")
    suspend fun getAllArtistsOnce(): List<ArtistEntity>

    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getArtistById(id: String): ArtistEntity?

    @Query("SELECT * FROM artists WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchArtists(query: String): List<ArtistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: ArtistEntity)

    @Query("DELETE FROM artists")
    suspend fun deleteAllArtists()

    @Query("SELECT COUNT(*) FROM artists")
    suspend fun getArtistCount(): Int
}