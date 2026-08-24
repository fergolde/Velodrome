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

class SearchRemoteTracksUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(query: String): Result<List<Track>> {
        return repository.searchRemoteTracks(query)
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

class GetAllLocalTracksUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(): List<Track> = repository.getAllLocalTracks()
}

// ========== WRAPPER ==========
class TrackUseCases @Inject constructor(
    val observeTracksByAlbum: ObserveTracksByAlbumUseCase,
    val syncTracksForAlbum: SyncTracksForAlbumUseCase,
    val getRandomSongs: GetRandomSongsUseCase,
    val searchRemoteTracks: SearchRemoteTracksUseCase,
    val getOfflineTracks: GetOfflineTracksUseCase,
    val getTopGlobalTracks: GetTopGlobalTracksUseCase,
    val getAllLocalTracks: GetAllLocalTracksUseCase
)