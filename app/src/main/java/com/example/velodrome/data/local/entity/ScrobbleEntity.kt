package com.example.velodrome.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_scrobbles")
data class ScrobbleEntity(
    @PrimaryKey
    val id: Long = 0,
    val trackId: String,
    val timestamp: Long,
    val isSubmitted: Boolean = false
)