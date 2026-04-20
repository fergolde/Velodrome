package com.example.velodrome.domain.usecase

import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.repository.NavidromeRepository
import javax.inject.Inject

class GetLatestAlbumsUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(size: Int = 20): Result<List<Album>> {
        return repository.getLatestAlbums(size)
    }
}

class GetTopAlbumsUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(size: Int = 20): Result<List<Album>> {
        return repository.getTopAlbums(size)
    }
}

class GetAlbumsByYearUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(year: Int, size: Int = 20): Result<List<Album>> {
        return repository.getAlbumsByYear(year, size)
    }
}

class GetAlbumsByGenreUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(genre: String, size: Int = 20): Result<List<Album>> {
        return repository.getAlbumsByGenre(genre, size)
    }
}

class GetGenresUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(): Result<List<String>> {
        return repository.getGenres()
    }
}

class GetRecentlyPlayedAlbumsUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(size: Int = 20): Result<List<Album>> {
        return repository.getRecentlyPlayedAlbums(size)
    }
}

class GetRandomAlbumsUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(size: Int = 20): Result<List<Album>> {
        return repository.getRandomAlbums(size)
    }
}

class GetAllAlbumsUseCase @Inject constructor(
    private val repository: NavidromeRepository
) {
    suspend operator fun invoke(size: Int = 100): Result<List<Album>> {
        return repository.getAllAlbums(size)
    }
}