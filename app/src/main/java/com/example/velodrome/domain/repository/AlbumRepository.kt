package com.example.velodrome.domain.repository

import com.example.velodrome.domain.model.Album
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for album operations.
 */
interface AlbumRepository {
    /**
     * Observa todos los albums desde la base de datos local (offline-first).
     * Returns un Flow reactivo que emite actualizaciones.
     */
    fun observeAllAlbums(): Flow<List<Album>>

    suspend fun getAlbum(albumId: String): Result<Album>
    suspend fun getLatestAlbums(size: Int = 20): Result<List<Album>>
    suspend fun getTopAlbums(size: Int = 20): Result<List<Album>>
    suspend fun getRecentlyPlayedAlbums(size: Int = 20): Result<List<Album>>
    suspend fun getRandomAlbums(size: Int = 20): Result<List<Album>>
    suspend fun getAllAlbums(size: Int = 100): Result<List<Album>>
    suspend fun getAllAlbumsFromServer(offset: Int = 0, size: Int = 100): Result<List<Album>>
    suspend fun getAlbumsByYear(year: Int, size: Int = 20): Result<List<Album>>
    suspend fun getAlbumsByGenre(genre: String, size: Int = 20): Result<List<Album>>
    suspend fun getGenres(): Result<List<String>>

    /**
     * Sincroniza albums desde el servidor y los guarda en la base de datos local.
     * El repositorio decide si usar caché o red.
     */
    suspend fun syncAlbumsFromServer(): Result<Int>
}