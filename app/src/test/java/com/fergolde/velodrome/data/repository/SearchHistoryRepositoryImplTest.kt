package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.SearchHistoryDao
import com.fergolde.velodrome.data.local.entity.SearchHistoryEntity
import com.fergolde.velodrome.util.CredentialsManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * El historial es local y por cuenta: cada cuenta ve solo sus queries y no
 * existe ninguna llamada de red para escribirlo o leerlo.
 */
class SearchHistoryRepositoryImplTest {

    private val dao = FakeSearchHistoryDao()
    private val credentialsManager: CredentialsManager = mockk()

    private fun repositoryFor(scope: String): SearchHistoryRepositoryImpl {
        every { credentialsManager.getAccountScope() } returns scope
        return SearchHistoryRepositoryImpl(dao, credentialsManager)
    }

    @Test
    fun `recording a query makes it the newest history entry`() = runTest {
        val repository = repositoryFor(SCOPE_A)

        repository.recordQuery("daft punk")
        repository.recordQuery("m83")

        assertEquals(listOf("m83", "daft punk"), repository.observeHistory().first())
    }

    @Test
    fun `recording the same query twice keeps a single entry`() = runTest {
        val repository = repositoryFor(SCOPE_A)

        repository.recordQuery("daft punk")
        repository.recordQuery("m83")
        repository.recordQuery("daft punk")

        assertEquals(listOf("daft punk", "m83"), repository.observeHistory().first())
    }

    @Test
    fun `recording trims the query and drops surrounding whitespace`() = runTest {
        val repository = repositoryFor(SCOPE_A)

        repository.recordQuery("  air  ")

        assertEquals(listOf("air"), repository.observeHistory().first())
    }

    @Test
    fun `a blank query is never recorded and keeps the stored history`() = runTest {
        val repository = repositoryFor(SCOPE_A)
        repository.recordQuery("air")
        val writesBefore = dao.insertCount

        repository.recordQuery("   ")

        assertEquals(listOf("air"), repository.observeHistory().first())
        assertEquals(writesBefore, dao.insertCount)
    }

    @Test
    fun `history is capped at the maximum keeping the newest entries`() = runTest {
        val repository = repositoryFor(SCOPE_A)

        (1..MAX_SEARCH_HISTORY_ENTRIES + 3).forEach { repository.recordQuery("query $it") }

        val history = repository.observeHistory().first()
        assertEquals(MAX_SEARCH_HISTORY_ENTRIES, history.size)
        assertEquals("query ${MAX_SEARCH_HISTORY_ENTRIES + 3}", history.first())
    }

    @Test
    fun `history of one account is invisible to another account`() = runTest {
        repositoryFor(SCOPE_A).recordQuery("cuenta a")

        val historyOfB = repositoryFor(SCOPE_B).observeHistory().first()

        assertEquals(emptyList<String>(), historyOfB)
    }

    @Test
    fun `removing a query deletes only that entry of the current account`() = runTest {
        repositoryFor(SCOPE_A).recordQuery("cuenta a")
        repositoryFor(SCOPE_B).recordQuery("cuenta b")
        val repository = repositoryFor(SCOPE_B)
        repository.recordQuery("otra b")

        repository.removeQuery("otra b")

        assertEquals(listOf("cuenta b"), repository.observeHistory().first())
        assertEquals(listOf("cuenta a"), repositoryFor(SCOPE_A).observeHistory().first())
    }

    @Test
    fun `clearing the history empties only the current account`() = runTest {
        repositoryFor(SCOPE_A).recordQuery("cuenta a")
        val repository = repositoryFor(SCOPE_B)
        repository.recordQuery("cuenta b")

        repository.clearHistory()

        assertEquals(emptyList<String>(), repository.observeHistory().first())
        assertEquals(listOf("cuenta a"), repositoryFor(SCOPE_A).observeHistory().first())
    }

    private companion object {
        const val SCOPE_A = "scope-a"
        const val SCOPE_B = "scope-b"
    }
}

/**
 * Doble en memoria del DAO. Reproduce las garantías que el repositorio espera
 * del SQL: índice único (cuenta, query) con reemplazo, y orden por timestamp e
 * id descendente.
 */
private class FakeSearchHistoryDao : SearchHistoryDao {

    private val rows = MutableStateFlow<List<SearchHistoryEntity>>(emptyList())
    private var nextId = 1L

    var insertCount = 0
        private set

    override suspend fun getQueries(accountScope: String): List<String> =
        ordered(accountScope).map { it.query }

    override fun observeQueries(accountScope: String, limit: Int): Flow<List<String>> =
        rows.map { ordered(accountScope).take(limit).map { row -> row.query } }

    override suspend fun deleteQuery(accountScope: String, query: String) {
        rows.update { current ->
            current.filterNot { it.accountScope == accountScope && it.query == query }
        }
    }

    override suspend fun deleteQueries(accountScope: String, queries: List<String>) {
        rows.update { current ->
            current.filterNot { it.accountScope == accountScope && it.query in queries }
        }
    }

    override suspend fun deleteHistory(accountScope: String) {
        rows.update { current -> current.filterNot { it.accountScope == accountScope } }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }

    override suspend fun insert(entry: SearchHistoryEntity) {
        insertCount++
        // Reemplazo por el índice único (accountScope, query).
        rows.update { current ->
            current.filterNot {
                it.accountScope == entry.accountScope && it.query == entry.query
            } + entry.copy(id = nextId++)
        }
    }

    private fun ordered(accountScope: String): List<SearchHistoryEntity> =
        rows.value
            .filter { it.accountScope == accountScope }
            .sortedWith(
                compareByDescending<SearchHistoryEntity> { it.timestamp }.thenByDescending { it.id }
            )
}
