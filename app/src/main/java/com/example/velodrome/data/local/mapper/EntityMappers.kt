package com.example.velodrome.data.local.mapper

import com.example.velodrome.data.local.entity.AlbumEntity
import com.example.velodrome.data.local.entity.ArtistEntity
import com.example.velodrome.data.local.entity.TrackEntity
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.Track

fun ArtistEntity.toDomain(): Artist = Artist(
    id = id,
    name = name,
    albumCount = albumCount,
    coverUrl = coverUrl
)

fun Artist.toEntity(): ArtistEntity = ArtistEntity(
    id = id,
    name = name,
    albumCount = albumCount,
    coverUrl = coverUrl
)

fun AlbumEntity.toDomain(): Album = Album(
    id = id,
    artistId = artistId,
    artistName = artistName,
    title = title,
    year = year,
    genre = genre,
    coverUrl = coverUrl,
    songCount = songCount,
    duration = duration
)

fun Album.toEntity(): AlbumEntity = AlbumEntity(
    id = id,
    artistId = artistId,
    artistName = artistName,
    title = title,
    year = year,
    genre = genre,
    coverUrl = coverUrl,
    songCount = songCount,
    duration = duration
)

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    albumId = albumId,
    albumName = albumName,
    artistName = artistName,
    title = title,
    durationSec = durationSec,
    sizeBytes = 0, // Not stored locally
    bitrate = 0, // Not stored locally
    trackNumber = trackNumber,
    isCached = localFilePath != null,
    coverArtId = coverArtId
)

fun Track.toEntity(): TrackEntity = TrackEntity(
    id = id,
    albumId = albumId,
    artistName = artistName,
    albumName = albumName,
    title = title,
    durationSec = durationSec,
    trackNumber = trackNumber,
    coverArtId = coverArtId,
    localFilePath = null
)