package com.example.velodrome.data.local.datasource

import com.example.velodrome.data.local.dao.AlbumDao
import com.example.velodrome.data.local.dao.ArtistDao
import com.example.velodrome.data.local.entity.AlbumEntity
import com.example.velodrome.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMusicDataSource @Inject constructor(
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao
) {
    // Artists
    fun observeAllArtists(): Flow<List<ArtistEntity>> = artistDao.getAllArtists()

    suspend fun getAllArtistsOnce(): List<ArtistEntity> = artistDao.getAllArtistsOnce()

    suspend fun getArtistById(id: String): ArtistEntity? = artistDao.getArtistById(id)

    suspend fun searchArtists(query: String): List<ArtistEntity> = artistDao.searchArtists(query)

    suspend fun insertArtists(artists: List<ArtistEntity>) = artistDao.insertArtists(artists)

    suspend fun getArtistCount(): Int = artistDao.getArtistCount()

    // Albums
    fun observeAllAlbums(): Flow<List<AlbumEntity>> = albumDao.getAllAlbums()

    suspend fun getAllAlbumsOnce(): List<AlbumEntity> = albumDao.getAllAlbumsOnce()

    suspend fun getAlbumById(id: String): AlbumEntity? = albumDao.getAlbumById(id)

    suspend fun getAlbumsByArtist(artistId: String): List<AlbumEntity> = albumDao.getAlbumsByArtist(artistId)

    suspend fun searchAlbums(query: String): List<AlbumEntity> = albumDao.searchAlbums(query)

    suspend fun insertAlbums(albums: List<AlbumEntity>) = albumDao.insertAlbums(albums)

    suspend fun getAlbumCount(): Int = albumDao.getAlbumCount()
}