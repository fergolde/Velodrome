package com.example.velodrome.domain.repository

import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.ArtistWithAlbums

/**
 * Repository interface for artist operations.
 */
interface ArtistRepository {
    suspend fun getArtists(offset: Int = 0, size: Int = 50): Result<List<Artist>>
    suspend fun getArtist(artistId: String): Result<ArtistWithAlbums>
    suspend fun search(query: String): Result<List<Artist>>
}