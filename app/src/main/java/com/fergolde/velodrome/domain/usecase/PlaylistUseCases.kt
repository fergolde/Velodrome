package com.fergolde.velodrome.domain.usecase

import com.fergolde.velodrome.domain.model.Playlist
import com.fergolde.velodrome.domain.repository.PlaylistRepository
import javax.inject.Inject

class GetPlaylistsUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(): Result<List<Playlist>> = repository.getPlaylists()
}

class GetPlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(id: String): Result<Playlist> = repository.getPlaylist(id)
}

data class PlaylistUseCases @Inject constructor(
    val getPlaylists: GetPlaylistsUseCase,
    val getPlaylist: GetPlaylistUseCase
)
