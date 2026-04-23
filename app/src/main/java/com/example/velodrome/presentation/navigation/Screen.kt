package com.example.velodrome.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Type-safe navigation routes for Velodrome app.
 * Replaces hardcoded string routes with strongly-typed sealed class.
 */
/*sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    // Auth routes
    data object Login : Screen(route = "login", title = "Login")

    // Main routes
    data object Home : Screen(route = "home", title = "Home", icon = Icons.Default.Home)
    data object Explore : Screen(route = "explore", title = "Explore", icon = Icons.Default.Explore)
    data object Artists : Screen(route = "artists", title = "Artists")
    data object Albums : Screen(route = "albums", title = "Albums")
    data object Settings : Screen(route = "settings", title = "Settings", icon = Icons.Default.Settings)

    // Detail routes with arguments
    data object ArtistDetail : Screen(route = "artist/{artistId}", title = "Artist") {
        fun createRoute(artistId: String) = "artist/$artistId"
    }

    data object AlbumDetail : Screen(route = "album/{albumId}", title = "Album") {
        fun createRoute(albumId: String) = "album/$albumId"
    }

    companion object {
        /**
         * All bottom navigation destinations
         */
        val bottomNavItems = listOf(Home, Explore, Settings)
    }
}*/