package com.fergolde.velodrome.domain.usecase

import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.ArtistWithAlbums
import com.fergolde.velodrome.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
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

class SyncArtistsUseCase @Inject constructor(
    private val repository: ArtistRepository
) {
    suspend operator fun invoke(): Result<Int> = repository.syncArtistsFromServer()
}

class ObserveArtistsUseCase @Inject constructor(
    private val repository: ArtistRepository
) {
    operator fun invoke(): Flow<List<Artist>> = repository.observeAllArtists()
}

// ========== WRAPPER ==========
class ArtistUseCases @Inject constructor(
    //val getArtists: GetArtistsUseCase,
    val search: SearchUseCase,
    val searchLocal: SearchLocalArtistsUseCase,
    val syncArtists: SyncArtistsUseCase,
    val observeArtists: ObserveArtistsUseCase
)