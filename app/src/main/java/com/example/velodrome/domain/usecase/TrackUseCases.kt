package com.example.velodrome.domain.usecase

import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.repository.TrackRepository
import javax.inject.Inject

class GetTracksUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(albumId: String) = repository.getTracks(albumId)
}

class GetStreamUrlUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(trackId: String) = repository.getStreamUrl(trackId)
}

class GetSongsByGenreUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(genre: String, count: Int = 50, offset: Int = 0): Result<List<Track>> {
        return repository.getSongsByGenre(genre, count, offset)
    }
}

class GetRandomSongsByGenreUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(genre: String, size: Int = 50): Result<List<Track>> {
        return repository.getRandomSongsByGenre(genre, size)
    }
}

class GetRandomSongsUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(size: Int = 50): Result<List<Track>> {
        return repository.getRandomSongs(size)
    }
}