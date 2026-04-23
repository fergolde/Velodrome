package com.example.velodrome.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import dagger.hilt.android.EntryPointAccessors
import com.example.velodrome.di.CredentialsEntryPoint
import com.example.velodrome.presentation.components.SharedBottomNavigationBar
import com.example.velodrome.presentation.components.MiniPlayer
import com.example.velodrome.presentation.navigation.Routes
import com.example.velodrome.presentation.player.PlayerManager
import com.example.velodrome.presentation.player.PlayerScreen
import com.example.velodrome.presentation.screen.albumdetail.AlbumDetailScreen
import com.example.velodrome.presentation.screen.albums.AlbumsScreen
import com.example.velodrome.presentation.screen.artistdetail.ArtistDetailScreen
import com.example.velodrome.presentation.screen.artists.ArtistsScreen
import com.example.velodrome.presentation.screen.explore.ExploreScreen
import com.example.velodrome.presentation.screen.home.HomeScreen
import com.example.velodrome.presentation.screen.login.LoginScreen
import com.example.velodrome.presentation.screen.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun VelodromeMainApp() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

    val context = LocalContext.current
    val credentialsManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CredentialsEntryPoint::class.java
        ).credentialsManager()
    }

    LaunchedEffect(Unit) {
        isLoggedIn = credentialsManager.hasCredentials()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoggedIn != null) {
            MainScaffold(
                navController = navController,
                // Si está logueado va a Home, si no a Login (usando objetos de Routes)
                startDestination = if (isLoggedIn == true) Routes.Home else Routes.Login,
                onLoginSuccess = { isLoggedIn = true }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    navController: NavHostController,
    startDestination: Any,
    onLoginSuccess: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Estado del reproductor
    val currentTrack by PlayerManager.currentTrack.collectAsState()
    val isPlaying by PlayerManager.isPlaying.collectAsState()
    val currentPosition by PlayerManager.currentPosition.collectAsState()
    val hasSong = currentTrack != null
    val scope = rememberCoroutineScope()

    // Configuración del BottomSheet para el Player
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 0.dp, // El mini reproductor se gestiona en la bottomBar del Scaffold interno
        sheetContent = {
            PlayerScreen(
                onMinimizeClick = {
                    scope.launch { sheetState.bottomSheetState.partialExpand() }
                },
                onHomeClick = { navController.navigate(Routes.Home) },
                onExploreClick = { navController.navigate(Routes.Explore) },
                onSettingsClick = { navController.navigate(Routes.Settings) }
            )
        },
        sheetDragHandle = {
            if (hasSong) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.5f)))
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                // Solo mostramos la barra si no estamos en la pantalla de Login
                val showBars = currentDestination?.hasRoute<Routes.Login>() == false
                if (showBars) {
                    Column {
                        // MiniPlayer: Solo si hay canción y el BottomSheet está colapsado
                        if (hasSong && sheetState.bottomSheetState.currentValue != SheetValue.Expanded) {
                            MiniPlayer(
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                onPlayPauseClick = { PlayerManager.togglePlayPause() },
                                onClick = { scope.launch { sheetState.bottomSheetState.expand() } },
                                onNextClick = { PlayerManager.next() },
                                onPreviousClick = { PlayerManager.previous() }
                            )
                        }

                        SharedBottomNavigationBar(
                            // Comprobamos la ruta actual de forma segura
                            currentRoute = when {
                                currentDestination?.hasRoute<Routes.Home>() == true -> "home"
                                currentDestination?.hasRoute<Routes.Explore>() == true -> "explore"
                                currentDestination?.hasRoute<Routes.Settings>() == true -> "settings"
                                else -> ""
                            },
                            onHomeClick = { navController.navigate(Routes.Home) { launchSingleTop = true } },
                            onExploreClick = { navController.navigate(Routes.Explore) { launchSingleTop = true } },
                            onSettingsClick = { navController.navigate(Routes.Settings) { launchSingleTop = true } }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable<Routes.Login> {
                        LoginScreen(onLoginSuccess = onLoginSuccess)
                    }

                    composable<Routes.Home> {
                        HomeScreen(
                            onAlbumClick = { id -> navController.navigate(Routes.AlbumDetail(id)) },
                            onExploreClick = { navController.navigate(Routes.Explore) },
                            onSettingsClick = { navController.navigate(Routes.Settings) },
                            onPlayerClick = { scope.launch { sheetState.bottomSheetState.expand() } }
                        )
                    }

                    composable<Routes.Explore> {
                        ExploreScreen(
                            onArtistClick = { id -> navController.navigate(Routes.ArtistDetail(id)) },
                            onAlbumClick = { id -> navController.navigate(Routes.AlbumDetail(id)) },
                            onArtistsViewAllClick = { navController.navigate(Routes.Artists) },
                            onAlbumsViewAllClick = { navController.navigate(Routes.Albums) }
                        )
                    }

                    composable<Routes.Artists> {
                        ArtistsScreen(
                            onArtistClick = { artist -> navController.navigate(Routes.ArtistDetail(artist.id)) }
                        )
                    }

                    composable<Routes.Albums> {
                        AlbumsScreen(
                            onAlbumClick = { album -> navController.navigate(Routes.AlbumDetail(album.id)) }
                        )
                    }

                    composable<Routes.Settings> {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // Detalle de Artista con argumento tipado
                    composable<Routes.ArtistDetail> { backStackEntry ->
                        val route: Routes.ArtistDetail = backStackEntry.toRoute()
                        ArtistDetailScreen(
                            artistId = route.artistId,
                            onBackClick = { navController.popBackStack() },
                            onAlbumClick = { id -> navController.navigate(Routes.AlbumDetail(id)) }
                        )
                    }

                    // Detalle de Álbum con argumento tipado
                    composable<Routes.AlbumDetail> { backStackEntry ->
                        val route: Routes.AlbumDetail = backStackEntry.toRoute()
                        AlbumDetailScreen(
                            albumId = route.albumId,
                            onBackClick = { navController.popBackStack() },
                            onHomeClick = { navController.navigate(Routes.Home) }
                        )
                    }
                }
            }
        }
    }
}