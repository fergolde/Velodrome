package com.example.velodrome.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val albumId: String,
    val artistName: String,
    val albumName: String,
    val title: String,
    val durationSec: Int,
    val trackNumber: Int,
    val coverArtId: String?,
    val localFilePath: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)