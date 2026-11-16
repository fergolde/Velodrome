package com.fergolde.velodrome.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Historial de búsquedas de la cuenta activa.
 *
 * Vive solo en el dispositivo y está acotado por cuenta: no hay red, no se
 * sincroniza y una cuenta nunca ve las queries de otra.
 */
interface SearchHistoryRepository {
    /** Últimas queries de la cuenta activa, la más reciente primero. */
    fun observeHistory(): Flow<List<String>>

    /** Registra [query] como la búsqueda más reciente de la cuenta activa. */
    suspend fun recordQuery(query: String)

    /** Borra una query concreta del historial de la cuenta activa. */
    suspend fun removeQuery(query: String)

    /** Vacía el historial de la cuenta activa. */
    suspend fun clearHistory()
}
