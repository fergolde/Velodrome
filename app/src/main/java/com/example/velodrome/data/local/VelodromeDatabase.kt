package com.example.velodrome.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.velodrome.data.local.dao.AlbumDao
import com.example.velodrome.data.local.dao.ArtistDao
import com.example.velodrome.data.local.dao.ScrobbleDao
import com.example.velodrome.data.local.dao.TrackDao
import com.example.velodrome.data.local.entity.AlbumEntity
import com.example.velodrome.data.local.entity.ArtistEntity
import com.example.velodrome.data.local.entity.ScrobbleEntity
import com.example.velodrome.data.local.entity.TrackEntity

@Database(
    entities = [ArtistEntity::class, AlbumEntity::class, TrackEntity::class, ScrobbleEntity::class],
    version = 2,
    exportSchema = false
)
abstract class VelodromeDatabase : RoomDatabase() {
    abstract fun artistDao(): ArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun trackDao(): TrackDao
    abstract fun scrobbleDao(): ScrobbleDao

    companion object {
        const val DATABASE_NAME = "velodrome_db"
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN localFilePath TEXT DEFAULT NULL")
    }
}