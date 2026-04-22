package com.example.velodrome.presentation.player

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton holder for the injected PlayerManager instance.
 * This allows composables to access the injected instance without DI directly.
 */
@Singleton
class PlayerManagerHolder @Inject constructor(
    playerManager: PlayerManager
) {
    val playerManager: PlayerManager = playerManager
}