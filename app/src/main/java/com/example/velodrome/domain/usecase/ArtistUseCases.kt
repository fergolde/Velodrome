package com.example.velodrome.domain.usecase

import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.repository.NavidromeRepository
import javax.inject.Inject

class GetArtistsUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(offset: Int = 0, size: Int = 50): Result<List<Artist>> {
        return repository.getArtists(offset, size)
    }
}

class GetAlbumUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(albumId: String) = repository.getAlbum(albumId)
}

class GetTracksUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(albumId: String) = repository.getTracks(albumId)
}

class GetStreamUrlUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(trackId: String) = repository.getStreamUrl(trackId)
}

class SearchUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(query: String) = repository.search(query)
}