package com.example.velodrome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.velodrome.presentation.screen.homescreen.HomeScreen
import com.example.velodrome.presentation.screen.login.LoginScreen
import com.example.velodrome.presentation.screen.explore.ExploreScreen
import com.example.velodrome.presentation.screen.artists.ArtistsScreen
import com.example.velodrome.presentation.screen.albums.AlbumsScreen
import com.example.velodrome.presentation.player.PlayerScreen
import com.example.velodrome.ui.theme.VelodromeTheme
import com.example.velodrome.util.CredentialsManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            VelodromeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    
    // Check if user has stored credentials
    var isLoggedIn by remember { mutableStateOf(CredentialsManager.hasCredentials()) }
    
    // Determine start destination based on login state
    val startDestination = if (isLoggedIn) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Update state after successful login
                    isLoggedIn = true
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onAlbumClick = { albumId ->
                    // TODO: navigate to album detail
                },
                onExploreClick = {
                    navController.navigate("explore")
                },
                onPlayerClick = {
                    navController.navigate("player")
                }
            )
        }
        composable("explore") {
            ExploreScreen(
                onHomeClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onArtistsViewAllClick = {
                    navController.navigate("artists")
                },
                onAlbumsViewAllClick = {
                    navController.navigate("albums")
                },
                onPlayerClick = {
                    navController.navigate("player")
                }
            )
        }
        composable("artists") {
            ArtistsScreen(
                onHomeClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onExploreClick = {
                    navController.navigate("explore")
                },
                onPlayerClick = {
                    navController.navigate("player")
                }
            )
        }
        composable("albums") {
            AlbumsScreen(
                onHomeClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onExploreClick = {
                    navController.navigate("explore")
                },
                onPlayerClick = {
                    navController.navigate("player")
                }
            )
        }
        composable("player") {
            PlayerScreen(
                onMinimizeClick = {
                    navController.popBackStack()
                },
                onHomeClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onExploreClick = {
                    navController.navigate("explore")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }
    }
}