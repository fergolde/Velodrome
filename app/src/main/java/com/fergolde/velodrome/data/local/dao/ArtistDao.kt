package com.fergolde.velodrome.data.local.dao

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query
import com.fergolde.velodrome.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getAllArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getArtistsPage(offset: Int, limit: Int): List<ArtistEntity>

    @Query("SELECT * FROM artists WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchArtists(query: String): List<ArtistEntity>

    @Upsert
    suspend fun insertArtists(artists: List<ArtistEntity>)
}