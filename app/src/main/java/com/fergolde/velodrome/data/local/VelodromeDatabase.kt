package com.fergolde.velodrome.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fergolde.velodrome.data.local.dao.AlbumDao
import com.fergolde.velodrome.data.local.dao.ArtistDao
import com.fergolde.velodrome.data.local.dao.ScrobbleDao
import com.fergolde.velodrome.data.local.dao.SearchHistoryDao
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.entity.AlbumEntity
import com.fergolde.velodrome.data.local.entity.ArtistEntity
import com.fergolde.velodrome.data.local.entity.ScrobbleEntity
import com.fergolde.velodrome.data.local.entity.SearchHistoryEntity
import com.fergolde.velodrome.data.local.entity.TrackEntity

@Database(
    entities = [
        ArtistEntity::class,
        AlbumEntity::class,
        TrackEntity::class,
        ScrobbleEntity::class,
        SearchHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VelodromeDatabase : RoomDatabase() {
    abstract fun artistDao(): ArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun trackDao(): TrackDao
    abstract fun scrobbleDao(): ScrobbleDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        const val DATABASE_NAME = "velodrome_db"

        /** Adds the `name` index used by artist paging and search. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artists_name ON artists(name)")
            }
        }

        /**
         * Añade la tabla del historial de búsqueda (v3).
         *
         * El DDL debe coincidir exactamente con el que genera Room para
         * [SearchHistoryEntity], incluidos named indices: Room valida el schema
         * al abrir y falla si falta un índice o cambia una columna.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `search_history` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`accountScope` TEXT NOT NULL, " +
                        "`query` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_accountScope_query` " +
                        "ON `search_history` (`accountScope`, `query`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_search_history_accountScope_timestamp` " +
                        "ON `search_history` (`accountScope`, `timestamp`)"
                )
            }
        }
    }
}
