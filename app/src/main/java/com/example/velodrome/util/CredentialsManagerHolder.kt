package com.example.velodrome.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton holder for injected instances.
 * This allows composables to access injected singletons without direct DI.
 */
@Singleton
class CredentialsManagerHolder @Inject constructor(
    credentialsManager: CredentialsManager
) {
    val credentialsManager: CredentialsManager = credentialsManager
}