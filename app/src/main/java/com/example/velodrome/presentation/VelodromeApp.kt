package com.example.velodrome.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.velodrome.di.CredentialsEntryPoint
import com.example.velodrome.presentation.components.MiniPlayer
import com.example.velodrome.presentation.components.SharedBottomNavigationBar
import com.example.velodrome.presentation.navigation.Routes
import com.example.velodrome.presentation.player.PlayerScreen
import com.example.velodrome.presentation.player.SharedPlayerViewModel
import com.example.velodrome.presentation.screen.albumdetail.AlbumDetailScreen
import com.example.velodrome.presentation.screen.albums.AlbumsScreen
import com.example.velodrome.presentation.screen.artistdetail.ArtistDetailScreen
import com.example.velodrome.presentation.screen.artists.ArtistsScreen
import com.example.velodrome.presentation.screen.explore.ExploreScreen
import com.example.velodrome.presentation.screen.home.HomeScreen
import com.example.velodrome.presentation.screen.login.LoginScreen
import com.example.velodrome.presentation.screen.settings.SettingsScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@Composable
fun VelodromeMainApp(
    sharedPlayerViewModel: SharedPlayerViewModel
) {
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
                onLoginSuccess = { isLoggedIn = true },
                sharedPlayerViewModel = sharedPlayerViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    navController: NavHostController,
    startDestination: Any,
    onLoginSuccess: () -> Unit,
    sharedPlayerViewModel: SharedPlayerViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Estado del reproductor desde SharedPlayerViewModel
    val currentTrack by sharedPlayerViewModel.currentTrack.collectAsState()
    val isPlaying by sharedPlayerViewModel.isPlaying.collectAsState()
    val currentPosition by sharedPlayerViewModel.currentPosition.collectAsState()
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
        sheetPeekHeight = 0.dp,
        sheetMaxWidth = Int.MAX_VALUE.dp,
        sheetDragHandle = null,
        sheetContainerColor = MaterialTheme.colorScheme.background,
        sheetContent = {
            PlayerScreen(
                onMinimizeClick = {
                    scope.launch { sheetState.bottomSheetState.partialExpand() }
                },
                onHomeClick = {
                    navController.navigate(Routes.Home) { launchSingleTop = true }
                    scope.launch { sheetState.bottomSheetState.partialExpand() }
                },
                onExploreClick = {
                    navController.navigate(Routes.Explore) { launchSingleTop = true }
                    scope.launch { sheetState.bottomSheetState.partialExpand() }
                },
                onSettingsClick = {
                    navController.navigate(Routes.Settings) { launchSingleTop = true }
                    scope.launch { sheetState.bottomSheetState.partialExpand() }
                }
            )
        }
    ) { innerPadding ->
        // Handle back press: from Explore or Settings go to Home, from Home exit app
        BackHandler(enabled = true) {
            val currentRoute = currentDestination?.route
            when {
                currentRoute == "Home" -> {
                    (context as? android.app.Activity)?.finish()
                }
                currentRoute == "Explore" || currentRoute == "Settings" -> {
                    navController.navigate(Routes.Home) {
                        launchSingleTop = true
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                else -> {
                    navController.popBackStack()
                }
            }
        }

        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
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
                                    onPlayPauseClick = { sharedPlayerViewModel.togglePlayPause() },
                                    onClick = { scope.launch { sheetState.bottomSheetState.expand() } },
                                    onNextClick = { sharedPlayerViewModel.next() },
                                    onPreviousClick = { sharedPlayerViewModel.previous() }
                                )
                            }

                            SharedBottomNavigationBar(
                                currentRoute = when {
                                    currentDestination?.route?.startsWith("Home") == true -> "home"
                                    currentDestination?.route?.startsWith("Explore") == true ||
                                    currentDestination?.route?.startsWith("Artists") == true ||
                                    currentDestination?.route?.startsWith("Albums") == true -> "explore"
                                    currentDestination?.route?.startsWith("Settings") == true -> "settings"
                                    else -> ""
                                },
                                onHomeClick = {
                                    navController.navigate(Routes.Home) {
                                        launchSingleTop = true
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    }
                                },
                                onExploreClick = {
                                    navController.navigate(Routes.Explore) {
                                        launchSingleTop = true
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    }
                                },
                                onSettingsClick = {
                                    navController.navigate(Routes.Settings) {
                                        launchSingleTop = true
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            ) { innerPaddingNavHost ->
                Box(modifier = Modifier.padding(bottom = innerPaddingNavHost.calculateBottomPadding())) {
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

                        composable<Routes.ArtistDetail> { backStackEntry ->
                            val route: Routes.ArtistDetail = backStackEntry.toRoute()
                            ArtistDetailScreen(
                                onBackClick = { navController.popBackStack() },
                                onAlbumClick = { id -> navController.navigate(Routes.AlbumDetail(id)) }
                            )
                        }

                        composable<Routes.AlbumDetail> { backStackEntry ->
                            val route: Routes.AlbumDetail = backStackEntry.toRoute()
                            AlbumDetailScreen(
                                onBackClick = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}