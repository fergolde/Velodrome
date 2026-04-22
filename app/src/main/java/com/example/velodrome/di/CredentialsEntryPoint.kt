package com.example.velodrome.di

import com.example.velodrome.util.CredentialsManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CredentialsEntryPoint {
    fun credentialsManager(): CredentialsManager
}