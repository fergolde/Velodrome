package com.example.velodrome.di

import android.content.Context
import androidx.room.Room
import com.example.velodrome.data.local.VelodromeDatabase
import com.example.velodrome.data.local.MIGRATION_1_2
import com.example.velodrome.data.local.dao.AlbumDao
import com.example.velodrome.data.local.dao.ArtistDao
import com.example.velodrome.data.local.dao.ScrobbleDao
import com.example.velodrome.data.local.dao.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVelodromeDatabase(
        @ApplicationContext context: Context
    ): VelodromeDatabase {
        return Room.databaseBuilder(
            context,
            VelodromeDatabase::class.java,
            VelodromeDatabase.DATABASE_NAME
        ).addMigrations(MIGRATION_1_2).build()
    }

    @Provides
    @Singleton
    fun provideArtistDao(database: VelodromeDatabase): ArtistDao {
        return database.artistDao()
    }

    @Provides
    @Singleton
    fun provideAlbumDao(database: VelodromeDatabase): AlbumDao {
        return database.albumDao()
    }

    @Provides
    @Singleton
    fun provideTrackDao(database: VelodromeDatabase): TrackDao {
        return database.trackDao()
    }

    @Provides
    @Singleton
    fun provideScrobbleDao(database: VelodromeDatabase): ScrobbleDao {
        return database.scrobbleDao()
    }
}