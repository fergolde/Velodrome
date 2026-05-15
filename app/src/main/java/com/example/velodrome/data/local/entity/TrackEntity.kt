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
    val sizeBytes: Long = 0L,
    val localFilePath: String? = null,
    val playCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)