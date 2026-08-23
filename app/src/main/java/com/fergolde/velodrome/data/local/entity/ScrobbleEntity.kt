package com.fergolde.velodrome.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_scrobbles",
    indices = [Index(value = ["isSubmitted", "timestamp"])]
)
data class ScrobbleEntity(
    @PrimaryKey
    val id: Long = 0,
    val trackId: String,
    val timestamp: Long,
    val isSubmitted: Boolean = false
)
