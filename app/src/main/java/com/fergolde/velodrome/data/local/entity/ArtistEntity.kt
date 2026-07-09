package com.fergolde.velodrome.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val albumCount: Int,
    val coverUrl: String?,
    val updatedAt: Long = System.currentTimeMillis()
)