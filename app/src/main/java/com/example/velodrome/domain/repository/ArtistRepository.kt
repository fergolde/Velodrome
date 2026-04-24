package com.example.velodrome.domain.repository

import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.ArtistWithAlbums
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for artist operations.
 */
interface ArtistRepository {
    /**
     * Observa todos los artists desde la base de datos local (offline-first).
     * Returns un Flow reactivo que emite actualizaciones.
     */
    fun observeAllArtists(): Flow<List<Artist>>

    suspend fun getArtists(offset: Int = 0, size: Int = 50): Result<List<Artist>>
    suspend fun getArtist(artistId: String): Result<ArtistWithAlbums>
    suspend fun search(query: String): Result<List<Artist>>

    /**
     * Sincroniza artists desde el servidor y los guarda en la base de datos local.
     * El repositorio decide si usar caché o red.
     */
    suspend fun syncArtistsFromServer(): Result<Int>
}