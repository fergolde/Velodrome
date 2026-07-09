package com.fergolde.velodrome.di

import com.fergolde.velodrome.data.repository.AlbumRepositoryImpl
import com.fergolde.velodrome.data.repository.ArtistRepositoryImpl
import com.fergolde.velodrome.data.repository.AuthRepositoryImpl
import com.fergolde.velodrome.data.repository.ScrobbleRepositoryImpl
import com.fergolde.velodrome.data.repository.TrackRepositoryImpl
import com.fergolde.velodrome.domain.repository.AlbumRepository
import com.fergolde.velodrome.domain.repository.ArtistRepository
import com.fergolde.velodrome.domain.repository.AuthRepository
import com.fergolde.velodrome.domain.repository.ScrobbleRepository
import com.fergolde.velodrome.domain.repository.TrackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(impl: AlbumRepositoryImpl): AlbumRepository

    @Binds
    @Singleton
    abstract fun bindArtistRepository(impl: ArtistRepositoryImpl): ArtistRepository

    @Binds
    @Singleton
    abstract fun bindTrackRepository(impl: TrackRepositoryImpl): TrackRepository

    @Binds
    @Singleton
    abstract fun bindScrobbleRepository(impl: ScrobbleRepositoryImpl): ScrobbleRepository
}