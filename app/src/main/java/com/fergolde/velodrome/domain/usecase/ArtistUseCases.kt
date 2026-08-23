package com.fergolde.velodrome.domain.usecase

import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.ArtistWithAlbums
import com.fergolde.velodrome.domain.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetArtistUseCase @Inject constructor(
    private val repository: ArtistRepository
) {
    suspend operator fun invoke(artistId: String): Result<ArtistWithAlbums> {
        return repository.getArtist(artistId)
    }
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

class GetArtistCountUseCase @Inject constructor(
    private val repository: ArtistRepository
) {
    suspend operator fun invoke(): Int = repository.artistCount()
}

// ========== WRAPPER ==========
class ArtistUseCases @Inject constructor(
    val getArtist: GetArtistUseCase,
    val searchLocal: SearchLocalArtistsUseCase,
    val syncArtists: SyncArtistsUseCase,
    val observeArtists: ObserveArtistsUseCase,
    val artistCount: GetArtistCountUseCase
)