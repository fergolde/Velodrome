package com.example.velodrome.di

import android.content.Context
import com.example.velodrome.data.datasource.MusicCacheDataSource
import com.example.velodrome.presentation.audio.AudioPlayerManager
import com.example.velodrome.presentation.audio.ScrobbleManager
import com.example.velodrome.util.CredentialsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideAudioPlayerManager(
        @ApplicationContext context: Context,
        scrobbleManager: ScrobbleManager,
        musicCacheDataSource: MusicCacheDataSource?,
        credentialsManager: CredentialsManager
    ): AudioPlayerManager {
        return AudioPlayerManager(context, scrobbleManager, musicCacheDataSource, credentialsManager)
    }
}
