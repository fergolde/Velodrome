package com.fergolde.velodrome.data.local.entity

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
    val songCount: Int = 0,
    val duration: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)