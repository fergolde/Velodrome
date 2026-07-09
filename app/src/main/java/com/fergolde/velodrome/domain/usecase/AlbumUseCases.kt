package com.fergolde.velodrome.domain.usecase

import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLatestAlbumsUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(size: Int = 20): Result<List<Album>> {
        return repository.getLatestAlbums(size)
    }
}

class GetTopAlbumsUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(size: Int = 20): Result<List<Album>> {
        return repository.getTopAlbums(size)
    }
}

class GetAlbumsByYearUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(year: Int, size: Int = 20): Result<List<Album>> {
        return repository.getAlbumsByYear(year, size)
    }
}

class GetAlbumsByGenreUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(genre: String, size: Int = 20): Result<List<Album>> {
        return repository.getAlbumsByGenre(genre, size)
    }
}

class GetGenresUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(): Result<List<String>> {
        return repository.getGenres()
    }
}

class GetRecentlyPlayedAlbumsUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(size: Int = 20): Result<List<Album>> {
        return repository.getRecentlyPlayedAlbums(size)
    }
}

class GetRandomAlbumsUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(size: Int = 20): Result<List<Album>> {
        return repository.getRandomAlbums(size)
    }
}

class GetAllAlbumsUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(size: Int = 100): Result<List<Album>> {
        return repository.getAllAlbums(size)
    }
}

class GetAlbumUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(albumId: String) = repository.getAlbum(albumId)
}

class SearchLocalAlbumsUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(query: String): List<Album> {
        return repository.searchLocal(query)
    }
}

class SyncAlbumsUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(): Result<Int> = repository.syncAlbumsFromServer()
}

class ObserveAlbumsUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    operator fun invoke(): Flow<List<Album>> = repository.observeAllAlbums()
}

class GetMinYearUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(): Int = repository.getMinYear()
}

// ========== WRAPPER ==========
class AlbumUseCases @Inject constructor(
    val getLatestAlbums: GetLatestAlbumsUseCase,
    val getTopAlbums: GetTopAlbumsUseCase,
    val getGenres: GetGenresUseCase,
    val getRecentlyPlayedAlbums: GetRecentlyPlayedAlbumsUseCase,
    val getRandomAlbums: GetRandomAlbumsUseCase,
    val searchLocal: SearchLocalAlbumsUseCase,
    val syncAlbums: SyncAlbumsUseCase,
    val observeAlbums: ObserveAlbumsUseCase,
    val getMinYear: GetMinYearUseCase
)