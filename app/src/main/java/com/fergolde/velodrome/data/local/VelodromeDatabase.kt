package com.fergolde.velodrome.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fergolde.velodrome.data.local.dao.AlbumDao
import com.fergolde.velodrome.data.local.dao.ArtistDao
import com.fergolde.velodrome.data.local.dao.ScrobbleDao
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.entity.AlbumEntity
import com.fergolde.velodrome.data.local.entity.ArtistEntity
import com.fergolde.velodrome.data.local.entity.ScrobbleEntity
import com.fergolde.velodrome.data.local.entity.TrackEntity

@Database(
    entities = [ArtistEntity::class, AlbumEntity::class, TrackEntity::class, ScrobbleEntity::class],
    version = 3,
    exportSchema = false
)
abstract class VelodromeDatabase : RoomDatabase() {
    abstract fun artistDao(): ArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun trackDao(): TrackDao
    abstract fun scrobbleDao(): ScrobbleDao

    companion object {
        const val DATABASE_NAME = "velodrome_db"

        /** v1 -> v2: drop write-only columns songCount/duration from albums. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE albums DROP COLUMN songCount")
                db.execSQL("ALTER TABLE albums DROP COLUMN duration")
            }
        }

        /**
         * v2 -> v3: add read-path indices and drop write-only updatedAt columns.
         * Index names must match Room's generated naming for schema validation.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_albumId_trackNumber` ON `tracks` (`albumId`, `trackNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_scrobbles_isSubmitted_timestamp` ON `pending_scrobbles` (`isSubmitted`, `timestamp`)")
                db.execSQL("ALTER TABLE artists DROP COLUMN updatedAt")
                db.execSQL("ALTER TABLE albums DROP COLUMN updatedAt")
                db.execSQL("ALTER TABLE tracks DROP COLUMN updatedAt")
            }
        }
    }
}