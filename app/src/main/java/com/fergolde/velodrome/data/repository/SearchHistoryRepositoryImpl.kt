package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.SearchHistoryDao
import com.fergolde.velodrome.data.local.entity.SearchHistoryEntity
import com.fergolde.velodrome.domain.repository.SearchHistoryRepository
import com.fergolde.velodrome.util.CredentialsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/** Cuántas queries recuerda el historial de cada cuenta. */
const val MAX_SEARCH_HISTORY_ENTRIES = 10

/**
 * Normaliza una query antes de guardarla: recorta espacios y devuelve `null`
 * cuando queda vacía, porque una búsqueda en blanco no es historial.
 */
fun normalizeSearchQuery(raw: String): String? = raw.trim().ifEmpty { null }

/**
 * Aplica una query nueva al historial reciente y devuelve la lista resultante,
 * la más reciente primero.
 *
 * Reglas:
 * - la comparación ignora mayúsculas y minúsculas;
 * - la query nueva encabeza la lista y nunca se duplica;
 * - se descartan las queries previas que son un **prefijo** de la nueva: al
 *   escribir "daft" el historial no debe quedarse con "d", "da" y "daf" como si
 *   fueran búsquedas distintas;
 * - la lista se recorta a [limit] entradas, tirando las más viejas.
 */
fun applySearchHistoryEntry(
    previous: List<String>,
    rawQuery: String,
    limit: Int = MAX_SEARCH_HISTORY_ENTRIES
): List<String> {
    val query = normalizeSearchQuery(rawQuery) ?: return previous
    if (limit <= 0) return emptyList()

    val normalizedNew = query.lowercase()
    val kept = previous.filterNot { existing ->
        val normalizedExisting = existing.lowercase()
        normalizedExisting == normalizedNew || normalizedNew.startsWith(normalizedExisting)
    }

    return (listOf(query) + kept).take(limit)
}

/**
 * Historial de búsqueda por cuenta, solo en el dispositivo.
 *
 * La cuenta se resuelve con [CredentialsManager.getAccountScope] en cada
 * operación, igual que las cachés de audio y carátulas: ninguna consulta puede
 * leer ni escribir filas de otra cuenta.
 */
@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
    private val credentialsManager: CredentialsManager
) : SearchHistoryRepository {

    override fun observeHistory(): Flow<List<String>> = flow {
        // La cuenta se resuelve al empezar a coleccionar, no al construir el Flow.
        emitAll(
            searchHistoryDao.observeQueries(
                accountScope = credentialsManager.getAccountScope(),
                limit = MAX_SEARCH_HISTORY_ENTRIES
            )
        )
    }.distinctUntilChanged()

    override suspend fun recordQuery(query: String) {
        val normalized = normalizeSearchQuery(query) ?: return
        val accountScope = credentialsManager.getAccountScope()
        val previous = searchHistoryDao.getQueries(accountScope)
        val updated = applySearchHistoryEntry(previous, normalized, MAX_SEARCH_HISTORY_ENTRIES)

        // Lo que ya no está en la lista nueva son duplicados o prefijos
        // obsoletos de esta misma búsqueda; solo se borran esas filas.
        val dropped = previous.filterNot { it in updated }
        if (dropped.isNotEmpty()) {
            searchHistoryDao.deleteQueries(accountScope, dropped)
        }

        searchHistoryDao.insert(
            SearchHistoryEntity(
                accountScope = accountScope,
                query = normalized,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeQuery(query: String) {
        val normalized = normalizeSearchQuery(query) ?: return
        searchHistoryDao.deleteQuery(credentialsManager.getAccountScope(), normalized)
    }

    override suspend fun clearHistory() {
        searchHistoryDao.deleteHistory(credentialsManager.getAccountScope())
    }
}
