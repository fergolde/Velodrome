package com.fergolde.velodrome.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Fija que la migración 2→3 solo agrega la tabla de historial,
 * con el DDL exacto que Room genera, sin tocar datos de usuario.
 */
class SearchHistoryMigrationDdlTest {
    private val expected = listOf(
        "CREATE TABLE IF NOT EXISTS `search_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`accountScope` TEXT NOT NULL, `query` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_accountScope_query` " +
            "ON `search_history` (`accountScope`, `query`)",
        "CREATE INDEX IF NOT EXISTS `index_search_history_accountScope_timestamp` " +
            "ON `search_history` (`accountScope`, `timestamp`)",
    )

    private class RecordingDatabase {
        val statements = mutableListOf<String>()
        val db: SupportSQLiteDatabase = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { proxy, method, args ->
            when {
                method.name == "execSQL" -> { statements += args!![0] as String; null }
                method.name == "toString" -> "RecordingDatabase"
                method.name == "hashCode" -> System.identityHashCode(proxy)
                method.name == "equals" -> proxy === args?.get(0)
                else -> throw UnsupportedOperationException("MIGRATION_2_3 solo debe ejecutar execSQL: ${method.name}")
            }
        } as SupportSQLiteDatabase
    }

    @Test
    fun `migration creates exactly the table and indices Room expects`() {
        val recorder = RecordingDatabase()
        VelodromeDatabase.MIGRATION_2_3.migrate(recorder.db)
        assertEquals(expected, recorder.statements)
    }

    @Test
    fun `migration goes from version 2 to 3`() {
        assertEquals(2, VelodromeDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, VelodromeDatabase.MIGRATION_2_3.endVersion)
    }

    @Test
    fun `migration is additive only and never touches user data`() {
        val recorder = RecordingDatabase()
        VelodromeDatabase.MIGRATION_2_3.migrate(recorder.db)
        assertTrue(recorder.statements.isNotEmpty())
        recorder.statements.forEach { sql ->
            val upper = sql.uppercase()
            assertTrue("sentencia no aditiva: $sql", upper.startsWith("CREATE "))
            listOf("DROP", "DELETE", "ALTER", "UPDATE", "INSERT", "PRAGMA").forEach {
                assertTrue("MIGRATION_2_3 contiene $it: $sql", !upper.contains(it))
            }
        }
    }
}
