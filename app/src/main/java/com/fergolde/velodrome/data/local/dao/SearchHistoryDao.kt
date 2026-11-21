package com.fergolde.velodrome.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fergolde.velodrome.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso al historial de búsqueda. Todas las consultas están acotadas a una
 * cuenta (`accountScope`) y usan parámetros bound: nunca se interpola SQL.
 */
@Dao
interface SearchHistoryDao {
    /** Todas las queries guardadas de la cuenta, la más reciente primero. */
    @Query(
        "SELECT `query` FROM search_history WHERE accountScope = :accountScope " +
            "ORDER BY timestamp DESC, id DESC"
    )
    suspend fun getQueries(accountScope: String): List<String>

    /** Últimas [limit] queries de la cuenta, la más reciente primero. */
    @Query(
        "SELECT `query` FROM search_history WHERE accountScope = :accountScope " +
            "ORDER BY timestamp DESC, id DESC LIMIT :limit"
    )
    fun observeQueries(accountScope: String, limit: Int): Flow<List<String>>

    /**
     * Reemplaza la fila de (cuenta, query) si ya existía, gracias al índice
     * único. Así el historial se deduplica sin leer antes qué había.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE accountScope = :accountScope AND `query` = :query")
    suspend fun deleteQuery(accountScope: String, query: String)

    @Query("DELETE FROM search_history WHERE accountScope = :accountScope AND `query` IN (:queries)")
    suspend fun deleteQueries(accountScope: String, queries: List<String>)

    @Query("DELETE FROM search_history WHERE accountScope = :accountScope")
    suspend fun deleteHistory(accountScope: String)

    /** Wipe completo: escape hatch del sign-out y del reset de cuenta. */
    @Query("DELETE FROM search_history")
    suspend fun deleteAll()
}
