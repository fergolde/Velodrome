package com.fergolde.velodrome.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Una query del historial de búsqueda de una cuenta.
 *
 * `accountScope` sale de `CredentialsManager.getAccountScope()`, igual que las
 * cachés de audio y carátulas: una cuenta nunca lee las búsquedas de otra. El
 * índice único (accountScope, query) es el que deduplica: guardar la misma
 * query otra vez reemplaza la fila anterior en lugar de duplicarla.
 */
@Entity(
    tableName = "search_history",
    indices = [
        Index(value = ["accountScope", "query"], unique = true),
        Index(value = ["accountScope", "timestamp"])
    ]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountScope: String,
    val query: String,
    val timestamp: Long
)
