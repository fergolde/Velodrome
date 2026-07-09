package com.fergolde.velodrome.domain.repository

import androidx.paging.PagingSource
import com.fergolde.velodrome.domain.model.Album
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

    /**
     * Paging source for efficient loading of albums.
     */
    fun getAlbumsPaged(): PagingSource<Int, Album>

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
     * Busca albums en la base de datos local por título o nombre de artista.
     * Uso: búsqueda reactiva con flatMapLatest en el ViewModel.
     */
    suspend fun searchLocal(query: String): List<Album>

    /**
     * Sincroniza albums desde el servidor y los guarda en la base de datos local.
     * El repositorio decide si usar caché o red.
     */
    suspend fun syncAlbumsFromServer(): Result<Int>

    /**
     * Sync with pagination support for resume capability.
     */
    suspend fun syncAlbumsFromServer(
        startOffset: Int = 0,
        onPageProcessed: suspend (newOffset: Int) -> Unit
    ): Result<Int>

    /**
     * Check if server has changed since given timestamp.
     * Uses ifModifiedSince param in getIndexes API.
     */
    suspend fun hasServerChangedSince(timestamp: Long): Boolean

    /**
     * Get the minimum year from albums in the database.
     */
    suspend fun getMinYear(): Int
}