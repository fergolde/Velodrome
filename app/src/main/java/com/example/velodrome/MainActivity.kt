package com.example.velodrome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInQuart
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import com.example.velodrome.di.CredentialsEntryPoint
import com.example.velodrome.presentation.navigation.Screen
import com.example.velodrome.presentation.screen.home.HomeScreen
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
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
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

    // 🔥 Estado inicial neutro (NO decidas aquí la pantalla)
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val credentialsManager = EntryPointAccessors.fromApplication(
        context.applicationContext,
        CredentialsEntryPoint::class.java
    ).credentialsManager()

    // 🔥 Cargar login UNA sola vez
    LaunchedEffect(Unit) {
        isLoggedIn = credentialsManager.hasCredentials()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 🔥 Solo render cuando ya sabemos estado
        if (isLoggedIn != null) {

            val startDestination =
                if (isLoggedIn == true)
                    Screen.Home.route
                else
                    Screen.Login.route

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {

                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = {
                            isLoggedIn = true
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        onAlbumClick = { albumId ->
                            navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                        },
                        onExploreClick = { navController.navigate(Screen.Explore.route) },
                        onPlayerClick = { PlayerState.isVisible = true },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Explore.route) {
                    ExploreScreen(
                        onHomeClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onArtistsViewAllClick = { navController.navigate(Screen.Artists.route) },
                        onAlbumsViewAllClick = { navController.navigate(Screen.Albums.route) },
                        onPlayerClick = { PlayerState.isVisible = true },
                        onArtistClick = { artistId ->
                            navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                        },
                        onAlbumClick = { albumId ->
                            navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                        },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Artists.route) {
                    ArtistsScreen(
                        onHomeClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onExploreClick = { navController.navigate(Screen.Explore.route) },
                        onPlayerClick = { PlayerState.isVisible = true },
                        onArtistClick = { artist ->
                            navController.navigate(Screen.ArtistDetail.createRoute(artist.id))
                        },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Albums.route) {
                    AlbumsScreen(
                        onHomeClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onExploreClick = { navController.navigate(Screen.Explore.route) },
                        onPlayerClick = { PlayerState.isVisible = true },
                        onAlbumClick = { album ->
                            navController.navigate(Screen.AlbumDetail.createRoute(album.id))
                        },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onExploreClick = { navController.navigate(Screen.Explore.route) },
                        onHomeClick = { navController.navigate(Screen.Home.route) }
                    )
                }

                composable(Screen.ArtistDetail.route) { backStackEntry ->
                    val artistId =
                        backStackEntry.arguments?.getString("artistId") ?: return@composable

                    ArtistDetailScreen(
                        artistId = artistId,
                        onBackClick = { navController.popBackStack() },
                        onAlbumClick = { albumId ->
                            navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                        },
                        onExploreClick = { navController.navigate(Screen.Explore.route) },
                        onPlayerClick = { PlayerState.isVisible = true },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.AlbumDetail.route) { backStackEntry ->
                    val albumId =
                        backStackEntry.arguments?.getString("albumId") ?: return@composable

                    AlbumDetailScreen(
                        albumId = albumId,
                        onBackClick = { navController.popBackStack() },
                        onHomeClick = { navController.navigate(Screen.Home.route) },
                        onExploreClick = { navController.navigate(Screen.Explore.route) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) },
                        onPlayerClick = { PlayerState.isVisible = true }
                    )
                }
            }
        }

        // 🔥 Player siempre encima
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
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                },
                onExploreClick = {
                    onDismiss()
                    navController.navigate(Screen.Explore.route)
                },
                onSettingsClick = {
                    onDismiss()
                    navController.navigate(Screen.Settings.route)
                },
                onQueueClick = { }
            )
        }
    }
}