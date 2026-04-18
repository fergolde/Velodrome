package com.example.velodrome.di

import com.example.velodrome.data.repository.NavidromeRepositoryImpl
import com.example.velodrome.domain.repository.NavidromeRepository
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
    abstract fun bindNavidromeRepository(
        impl: NavidromeRepositoryImpl
    ): NavidromeRepository
}