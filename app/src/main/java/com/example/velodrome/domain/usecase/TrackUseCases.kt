package com.example.velodrome.domain.usecase

import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTracksByAlbumUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    operator fun invoke(albumId: String): Flow<List<Track>> = repository.observeTracksByAlbum(albumId)
}

class SyncTracksForAlbumUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(albumId: String): Result<Unit> = repository.syncTracksForAlbum(albumId)
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

class SearchRemoteTracksUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(query: String): Result<List<Track>> {
        return repository.searchRemoteTracks(query)
    }
}

// ========== WRAPPER ==========
class TrackUseCases @Inject constructor(
    val observeTracksByAlbum: ObserveTracksByAlbumUseCase,
    val syncTracksForAlbum: SyncTracksForAlbumUseCase,
    val getRandomSongsByGenre: GetRandomSongsByGenreUseCase,
    val getRandomSongs: GetRandomSongsUseCase,
    val searchRemoteTracks: SearchRemoteTracksUseCase
)