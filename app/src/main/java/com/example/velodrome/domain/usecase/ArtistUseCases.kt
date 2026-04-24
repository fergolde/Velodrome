package com.example.velodrome.domain.usecase

import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.ArtistWithAlbums
import com.example.velodrome.domain.repository.ArtistRepository
import javax.inject.Inject

class GetArtistsUseCase @Inject constructor(
    private val repository: ArtistRepository
) {
    suspend operator fun invoke(offset: Int = 0, size: Int = 50): Result<List<Artist>> {
        return repository.getArtists(offset, size)
    }
}

class GetArtistUseCase @Inject constructor(
    private val repository: ArtistRepository
) {
    suspend operator fun invoke(artistId: String): Result<ArtistWithAlbums> {
        return repository.getArtist(artistId)
    }
}

class SearchUseCase @Inject constructor(
    private val repository: ArtistRepository
) {
    suspend operator fun invoke(query: String) = repository.search(query)
}

class SearchLocalArtistsUseCase @Inject constructor(
    private val repository: ArtistRepository
) {
    suspend operator fun invoke(query: String): List<Artist> {
        return repository.searchLocal(query)
    }
}

// ========== WRAPPER ==========
class ArtistUseCases @Inject constructor(
    val getArtists: GetArtistsUseCase,
    val getArtist: GetArtistUseCase,
    val search: SearchUseCase,
    val searchLocal: SearchLocalArtistsUseCase
)