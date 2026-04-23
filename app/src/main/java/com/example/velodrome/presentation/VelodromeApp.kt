package com.example.velodrome.presentation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.EntryPointAccessors
import com.example.velodrome.di.CredentialsEntryPoint
import com.example.velodrome.presentation.components.SharedBottomNavigationBar
import com.example.velodrome.presentation.components.MiniPlayerOverlay
import com.example.velodrome.presentation.navigation.Screen
import com.example.velodrome.presentation.player.PlayerScreen
import com.example.velodrome.presentation.screen.albumdetail.AlbumDetailScreen
import com.example.velodrome.presentation.screen.albums.AlbumsScreen
import com.example.velodrome.presentation.screen.artistdetail.ArtistDetailScreen
import com.example.velodrome.presentation.screen.artists.ArtistsScreen
import com.example.velodrome.presentation.screen.explore.ExploreScreen
import com.example.velodrome.presentation.screen.home.HomeScreen
import com.example.velodrome.presentation.screen.login.LoginScreen
import com.example.velodrome.presentation.screen.settings.SettingsScreen

object PlayerState {
    var isVisible by mutableStateOf(false)
}

@Composable
fun VelodromeMainApp() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

    val context = LocalContext.current
    val credentialsManager = EntryPointAccessors.fromApplication(
        context.applicationContext,
        CredentialsEntryPoint::class.java
    ).credentialsManager()

    // Check credentials on launch
    LaunchedEffect(Unit) {
        isLoggedIn = credentialsManager.hasCredentials()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Only render when we know the state
        if (isLoggedIn != null) {
            MainScaffold(
                navController = navController,
                startDestination = if (isLoggedIn == true) Screen.Home.route else Screen.Login.route,
                onLoginSuccess = { isLoggedIn = true }
            )
        }
    }
}

@Composable
fun MainScaffold(
    navController: NavHostController,
    startDestination: String,
    onLoginSuccess: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    val showBottomBar = currentRoute == "home" || currentRoute == "explore" || currentRoute == "settings" ||
            currentRoute == "albums" || currentRoute == "artists" ||
            currentRoute.startsWith("album/") || currentRoute.startsWith("artist/")

    Scaffold(
        bottomBar = {
            Column {
                if (showBottomBar) {
                    MiniPlayerOverlay(onPlayerClick = { PlayerState.isVisible = true })
                    SharedBottomNavigationBar(
                        currentRoute = currentRoute ?: "",
                        onHomeClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onExploreClick = {
                            navController.navigate(Screen.Explore.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(onLoginSuccess = onLoginSuccess)
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
                        onExploreClick = { navController.navigate(Screen.Explore.route) },
                        onPlayerClick = { PlayerState.isVisible = true },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Explore.route) {
                    ExploreScreen(
                        onHomeClick = { navController.navigate(Screen.Home.route) },
                        onArtistsViewAllClick = { navController.navigate(Screen.Artists.route) },
                        onAlbumsViewAllClick = { navController.navigate(Screen.Albums.route) },
                        onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) },
                        onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Artists.route) {
                    ArtistsScreen(
                        onHomeClick = { navController.navigate(Screen.Home.route) },
                        onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it.id)) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Albums.route) {
                    AlbumsScreen(
                        onHomeClick = { navController.navigate(Screen.Home.route) },
                        onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it.id)) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onHomeClick = { navController.navigate(Screen.Home.route) }
                    )
                }

                composable(
                    route = Screen.ArtistDetail.route,
                    arguments = listOf(navArgument("artistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
                    ArtistDetailScreen(
                        artistId = artistId,
                        onBackClick = { navController.popBackStack() },
                        onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
                    )
                }

                composable(
                    route = Screen.AlbumDetail.route,
                    arguments = listOf(navArgument("albumId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
                    AlbumDetailScreen(
                        albumId = albumId,
                        onBackClick = { navController.popBackStack() },
                        onHomeClick = { navController.navigate(Screen.Home.route) }
                    )
                }
            }
        }

        // Player overlay
        if (PlayerState.isVisible) {
            PlayerScreen(
                onMinimizeClick = { PlayerState.isVisible = false },
                onHomeClick = {
                    PlayerState.isVisible = false
                    navController.navigate(Screen.Home.route)
                },
                onExploreClick = {
                    PlayerState.isVisible = false
                    navController.navigate(Screen.Explore.route)
                },
                onSettingsClick = {
                    PlayerState.isVisible = false
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
    }
}