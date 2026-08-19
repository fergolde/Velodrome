package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.data.remote.dto.SongDto
import com.fergolde.velodrome.domain.model.Playlist
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.repository.PlaylistRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val api: NavidromeApi
) : PlaylistRepository {

    override suspend fun getPlaylists(): Result<List<Playlist>> = runCatching {
        val response = api.getPlaylists()
        response.response.playlists?.playlist?.map { summary ->
            Playlist(
                id = summary.id,
                name = summary.name,
                songCount = summary.songCount ?: 0,
                coverArtId = summary.coverArt,
                tracks = emptyList()
            )
        } ?: emptyList()
    }

    override suspend fun getPlaylist(id: String): Result<Playlist> = runCatching {
        val response = api.getPlaylist(id)
        val detail = response.response.playlistDetail
            ?: throw IllegalStateException("Playlist not found")
        val tracks = detail.songs?.map { toTrack(it) } ?: emptyList()
        Playlist(
            id = detail.id,
            name = detail.name,
            songCount = tracks.size,
            coverArtId = null,
            tracks = tracks
        )
    }

    private fun toTrack(dto: SongDto): Track {
        val albumId = dto.albumId ?: ""
        val effectiveCoverArtId = dto.coverArt ?: "al-$albumId"
        return Track(
            id = dto.id,
            albumId = albumId,
            title = dto.title,
            artistName = dto.artist ?: "",
            albumName = dto.album ?: albumId,
            durationSec = dto.duration ?: 0,
            sizeBytes = dto.size ?: 0L,
            bitrate = dto.bitRate ?: 0,
            trackNumber = dto.track ?: 0,
            year = dto.year,
            isCached = false,
            coverArtId = effectiveCoverArtId,
            playCount = dto.playCount ?: 0
        )
    }
}
