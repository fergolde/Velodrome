package com.fergolde.velodrome.domain.usecase

import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.repository.TrackRepository
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
    suspend operator fun invoke(
        size: Int = 50,
        genre: String? = null,
        fromYear: Int? = null,
        toYear: Int? = null
    ): Result<List<Track>> {
        return repository.getRandomSongs(size, genre, fromYear, toYear)
    }
}

class GetRandomSongsByYearUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(size: Int = 50, fromYear: Int? = null, toYear: Int? = null): Result<List<Track>> {
        return repository.getRandomSongs(size, null, fromYear, toYear)
    }
}

class SearchRemoteTracksUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(query: String): Result<List<Track>> {
        return repository.searchRemoteTracks(query)
    }
}

class GetTopSongsUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(count: Int = 100): Result<List<Track>> {
        return repository.getTopSongs(count)
    }
}

class GetOfflineTracksUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(): List<Track> {
        return repository.getOfflineTracks()
    }
}

class GetTopGlobalTracksUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(size: Int = 100): Result<List<Track>> {
        return repository.getTopGlobalTracks(size)
    }
}

// ========== WRAPPER ==========
class TrackUseCases @Inject constructor(
    val observeTracksByAlbum: ObserveTracksByAlbumUseCase,
    val syncTracksForAlbum: SyncTracksForAlbumUseCase,
    val getRandomSongsByGenre: GetRandomSongsByGenreUseCase,
    val getRandomSongs: GetRandomSongsUseCase,
    val searchRemoteTracks: SearchRemoteTracksUseCase,
    val getOfflineTracks: GetOfflineTracksUseCase,
    val GetTopGlobalTracksUseCase : GetTopGlobalTracksUseCase
)