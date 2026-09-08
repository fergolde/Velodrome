package com.fergolde.velodrome.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "albums",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artistName"])
    ]
)
data class AlbumEntity(
    @PrimaryKey
    val id: String,
    val artistId: String,
    val artistName: String,
    val title: String,
    val year: Int?,
    val genre: String?,
    val coverUrl: String?
)
