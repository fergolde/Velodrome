package com.example.velodrome.data.local.datasource

import com.example.velodrome.data.local.dao.AlbumDao
import com.example.velodrome.data.local.dao.ArtistDao
import com.example.velodrome.data.local.entity.AlbumEntity
import com.example.velodrome.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMusicDataSource @Inject constructor(
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao
) {
    // Artists
    fun observeAllArtists(): Flow<List<ArtistEntity>> = artistDao.getAllArtists()

    suspend fun searchArtists(query: String): List<ArtistEntity> = artistDao.searchArtists(query)

    suspend fun insertArtists(artists: List<ArtistEntity>) = artistDao.insertArtists(artists)

    // Albums
    fun observeAllAlbums(): Flow<List<AlbumEntity>> = albumDao.getAllAlbums()

    suspend fun searchAlbums(query: String): List<AlbumEntity> = albumDao.searchAlbums(query)

    suspend fun insertAlbums(albums: List<AlbumEntity>) = albumDao.insertAlbums(albums)

}