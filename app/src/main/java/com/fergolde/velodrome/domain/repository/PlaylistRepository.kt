package com.fergolde.velodrome.domain.repository

import com.fergolde.velodrome.domain.model.Playlist

interface PlaylistRepository {
    suspend fun getPlaylists(): Result<List<Playlist>>
    suspend fun getPlaylist(id: String): Result<Playlist>
}
