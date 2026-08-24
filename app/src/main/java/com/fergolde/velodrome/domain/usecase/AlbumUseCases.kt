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

class GetAlbumCountUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(): Int = repository.albumCount()
}

class GetLocalAlbumsUseCase @Inject constructor(
    private val repository: AlbumRepository
) {
    suspend operator fun invoke(): List<Album> = repository.getLocalAlbums()
}

// ========== WRAPPER ==========
class AlbumUseCases @Inject constructor(
    val getLatestAlbums: GetLatestAlbumsUseCase,
    val getTopAlbums: GetTopAlbumsUseCase,
    val getGenres: GetGenresUseCase,
    val getRecentlyPlayedAlbums: GetRecentlyPlayedAlbumsUseCase,
    val getRandomAlbums: GetRandomAlbumsUseCase,
    val getAlbum: GetAlbumUseCase,
    val searchLocal: SearchLocalAlbumsUseCase,
    val syncAlbums: SyncAlbumsUseCase,
    val observeAlbums: ObserveAlbumsUseCase,
    val getMinYear: GetMinYearUseCase,
    val albumCount: GetAlbumCountUseCase,
    val getLocalAlbums: GetLocalAlbumsUseCase
)