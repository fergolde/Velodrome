package com.example.velodrome.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey
    val id: String,
    val artistId: String,
    val artistName: String,
    val title: String,
    val year: Int?,
    val genre: String?,
    val coverUrl: String?,
    val updatedAt: Long = System.currentTimeMillis()
)