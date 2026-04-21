package com.example.velodrome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInQuart
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.velodrome.presentation.screen.homescreen.HomeScreen
import com.example.velodrome.presentation.screen.login.LoginScreen
import com.example.velodrome.presentation.screen.explore.ExploreScreen
import com.example.velodrome.presentation.screen.artists.ArtistsScreen
import com.example.velodrome.presentation.screen.albums.AlbumsScreen
import com.example.velodrome.presentation.screen.artistdetail.ArtistDetailScreen
import com.example.velodrome.presentation.screen.albumdetail.AlbumDetailScreen
import com.example.velodrome.presentation.screen.settings.SettingsScreen
import com.example.velodrome.presentation.player.PlayerScreen
import com.example.velodrome.domain.repository.SettingsRepository
import com.example.velodrome.ui.theme.VelodromeTheme
import com.example.velodrome.util.CredentialsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            VelodromeTheme(settingsRepository = settingsRepository) {
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
object PlayerState {
    var isVisible by mutableStateOf(false)
}


@Composable
fun MainApp() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(CredentialsManager.hasCredentials()) }
    val startDestination = if (isLoggedIn) "home" else "login"

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Capa 1: toda la app normal ──────────────────────────────────────
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        isLoggedIn = true
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    onAlbumClick = { albumId -> navController.navigate("album/$albumId") },
                    onExploreClick = { navController.navigate("explore") },
                    onPlayerClick = { PlayerState.isVisible = true },  // ← solo mostrar overlay
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("explore") {
                ExploreScreen(
                    onHomeClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                    onArtistsViewAllClick = { navController.navigate("artists") },
                    onAlbumsViewAllClick = { navController.navigate("albums") },
                    onPlayerClick = { PlayerState.isVisible = true },
                    onArtistClick = { artistId -> navController.navigate("artist/$artistId") },
                    onAlbumClick = { albumId -> navController.navigate("album/$albumId") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("artists") {
                ArtistsScreen(
                    onHomeClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                    onExploreClick = { navController.navigate("explore") },
                    onPlayerClick = { PlayerState.isVisible = true },
                    onArtistClick = { artist -> navController.navigate("artist/${artist.id}") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("albums") {
                AlbumsScreen(
                    onHomeClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                    onExploreClick = { navController.navigate("explore") },
                    onPlayerClick = { PlayerState.isVisible = true },
                    onAlbumClick = { album -> navController.navigate("album/${album.id}") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onExploreClick = { navController.navigate("explore") },
                    onHomeClick = { navController.navigate("home") }
                )
            }
            composable("artist/{artistId}") { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
                ArtistDetailScreen(
                    artistId = artistId,
                    onBackClick = { navController.popBackStack() },
                    onAlbumClick = { albumId -> navController.navigate("album/$albumId") },
                    onExploreClick = { navController.navigate("explore") },
                    onPlayerClick = { PlayerState.isVisible = true }
                )
            }
            composable("album/{albumId}") {
                AlbumDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onExploreClick = { navController.navigate("explore") },
                    onPlayerClick = { PlayerState.isVisible = true }
                )
            }
        }

        // ── Capa 2: Player overlay — siempre compuesto, nunca destruido ─────
        PlayerOverlay(
            navController = navController,
            isVisible = PlayerState.isVisible,
            onDismiss = { PlayerState.isVisible = false }
        )
    }
}

@Composable
fun PlayerOverlay(
    navController: NavController,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(1f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(380, easing = EaseOutQuart)
            )
        } else {
            // Salida por botón — solo llega aquí si no fue swipe
            // (si fue swipe, offsetY ya está en 1f por el snapTo del PlayerScreen)
            if (offsetY.value < 1f) {
                offsetY.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(280, easing = EaseInQuart)
                )
            }
        }
    }

    if (isVisible || offsetY.value < 1f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = size.height * offsetY.value
                    alpha = (1f - offsetY.value * 1.5f).coerceIn(0f, 1f)
                }
        ) {
            PlayerScreen(
                // El swipe mueve el overlay directamente
                onDrag = { dragFraction ->
                    coroutineScope.launch { offsetY.snapTo(dragFraction) }
                },
                onDragEnd = { completed ->
                    coroutineScope.launch {
                        if (completed) {
                            offsetY.animateTo(1f, tween(200, easing = EaseInQuart))
                            onDismiss()
                            offsetY.snapTo(1f)
                        } else {
                            offsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                },
                onMinimizeClick = onDismiss,
                onHomeClick = {
                    onDismiss()
                    navController.navigate("home") { popUpTo("home") { inclusive = true } }
                },
                onExploreClick = {
                    onDismiss()
                    navController.navigate("explore")
                },
                onSettingsClick = {
                    onDismiss()
                    navController.navigate("settings")
                },
                onQueueClick = { }
            )
        }
    }
}