package com.example.velodrome.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using kotlinx.serialization
 */
sealed class Routes {
    @Serializable
    data object Login : Routes()

    @Serializable
    data object Home : Routes()

    @Serializable
    data object Explore : Routes()

    @Serializable
    data object Settings : Routes()

    @Serializable
    data object Artists : Routes()

    @Serializable
    data object Albums : Routes()

    @Serializable
    data class AlbumDetail(val albumId: String) : Routes()

    @Serializable
    data class ArtistDetail(val artistId: String) : Routes()

    @Serializable
    data class Player(val trackId: String? = null) : Routes()
}