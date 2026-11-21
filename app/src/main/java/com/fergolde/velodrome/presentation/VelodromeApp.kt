package com.fergolde.velodrome.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fergolde.velodrome.di.CredentialsEntryPoint
import com.fergolde.velodrome.presentation.components.MiniPlayer
import com.fergolde.velodrome.presentation.components.SharedBottomNavigationBar
import com.fergolde.velodrome.presentation.navigation.Routes
import com.fergolde.velodrome.presentation.player.PlayerScreen
import com.fergolde.velodrome.presentation.player.SharedPlayerViewModel
import com.fergolde.velodrome.presentation.screen.albumdetail.AlbumDetailScreen
import com.fergolde.velodrome.presentation.screen.albums.AlbumsScreen
import com.fergolde.velodrome.presentation.screen.artistdetail.ArtistDetailScreen
import com.fergolde.velodrome.presentation.screen.artists.ArtistsScreen
import com.fergolde.velodrome.presentation.screen.explore.ExploreScreen
import com.fergolde.velodrome.presentation.screen.home.HomeScreen
import com.fergolde.velodrome.presentation.screen.playlistdetail.PlaylistDetailScreen
import com.fergolde.velodrome.presentation.screen.login.LoginScreen
import com.fergolde.velodrome.presentation.screen.settings.SettingsScreen
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
                startDestination = if (isLoggedIn == true) Routes.Home else Routes.Login,
                onLoginSuccess = {
                    isLoggedIn = true
                    navController.navigate(Routes.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLogout = {
                    isLoggedIn = false
                    navController.navigate(Routes.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
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
    onLogout: () -> Unit,
    sharedPlayerViewModel: SharedPlayerViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // currentPosition intentionally NOT collected here: it ticks at 1Hz and would
    // recompose the whole app tree. MiniPlayer collects it internally (leaf-scoped).
    val currentTrack by sharedPlayerViewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by sharedPlayerViewModel.isPlaying.collectAsStateWithLifecycle()
    val hasSong = currentTrack != null
    val scope = rememberCoroutineScope()
    val localContext = LocalContext.current

    val isHome = currentDestination?.hasRoute<Routes.Home>() == true
    val isExplore = currentDestination?.hasRoute<Routes.Explore>() == true
    val isSettings = currentDestination?.hasRoute<Routes.Settings>() == true
    val isArtists = currentDestination?.hasRoute<Routes.Artists>() == true
    val isAlbums = currentDestination?.hasRoute<Routes.Albums>() == true
    val isLogin = currentDestination?.hasRoute<Routes.Login>() == true

    // Destino activo de la navegación principal. El reproductor y los detalles
    // no son pestañas: la cadena vacía deja la barra sin selección.
    val currentRoute = when {
        isHome -> "home"
        isExplore || isArtists || isAlbums -> "explore"
        isSettings -> "settings"
        else -> ""
    }
    // Se navega solo si no se está ya en el destino raíz, sin apilar copias
    // del mismo destino.
    val navigateToHomeTab: () -> Unit = {
        if (!isHome) {
            navController.navigate(Routes.Home) {
                launchSingleTop = true
                popUpTo(Routes.Home) { inclusive = false }
            }
        }
    }
    val navigateToExploreTab: () -> Unit = {
        if (!isExplore) {
            navController.navigate(Routes.Explore) {
                launchSingleTop = true
                popUpTo(Routes.Home) { inclusive = false }
            }
        }
    }
    val navigateToSettingsTab: () -> Unit = {
        if (!isSettings) {
            navController.navigate(Routes.Settings) {
                launchSingleTop = true
                popUpTo(Routes.Home) { inclusive = false }
            }
        }
    }

    // Con Material3 1.5.0 (BOM 2026.11.01) el valor por defecto de skipHiddenState
    // pasó a true y sheetPeekHeight = 0.dp deshabilita el ancla PartiallyExpanded:
    // el sheet se asienta en Hidden y el Saver restaura Hidden. Si Hidden no está
    // en enabledValues, el siguiente arranque revienta con "The initial value
    // must be one of the enabled values". Se declara Hidden explícito para que
    // el valor inicial y el restaurado siempre estén permitidos.
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )

    // La pantalla del reproductor es pesada y ejecuta una animación infinita del
    // ecualizador mientras suena música. BottomSheetScaffold compone sheetContent
    // aunque esté colapsado del todo (peekHeight = 0), así que solo se renderiza
    // mientras está expandido o en camino a estarlo. El estado colapsado asentado
    // es Hidden; cualquier expansión cambia antes targetValue, y durante el
    // colapso currentValue sigue en Expanded hasta asentarse, así el contenido
    // permanece visible durante todo el arrastre y se desmonta al quedar oculto.
    val isPlayerSheetActive =
        sheetState.bottomSheetState.currentValue == SheetValue.Expanded ||
            sheetState.bottomSheetState.targetValue == SheetValue.Expanded

    // Interceptar botón atrás
    BackHandler(enabled = !isLogin) {
        when {
            isHome -> {
                (localContext as? android.app.Activity)?.finish()
            }
            isExplore || isSettings -> {
                navController.navigate(Routes.Home) {
                    launchSingleTop = true
                    popUpTo(Routes.Home) { inclusive = false }
                }
            }
            else -> {
                navController.popBackStack()
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 0.dp,
        sheetMaxWidth = Int.MAX_VALUE.dp,
        sheetDragHandle = null,
        sheetContainerColor = MaterialTheme.colorScheme.background,
        sheetContent = {
            if (isPlayerSheetActive) {
                PlayerScreen(
                    onMinimizeClick = {
                        scope.launch { sheetState.bottomSheetState.hide() }
                    },
                    onHomeClick = {
                        navController.navigate(Routes.Home) { launchSingleTop = true }
                        scope.launch { sheetState.bottomSheetState.hide() }
                    },
                    onExploreClick = {
                        navController.navigate(Routes.Explore) { launchSingleTop = true }
                        scope.launch { sheetState.bottomSheetState.hide() }
                    },
                    onSettingsClick = {
                        navController.navigate(Routes.Settings) { launchSingleTop = true }
                        scope.launch { sheetState.bottomSheetState.hide() }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            Scaffold(
                bottomBar = {
                    val showBars = !isLogin
                    if (showBars) {
                        Column {
                            if (hasSong && sheetState.bottomSheetState.currentValue != SheetValue.Expanded) {
                                MiniPlayer(
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    positionFlow = sharedPlayerViewModel.currentPosition,
                                    onPlayPauseClick = { sharedPlayerViewModel.togglePlayPause() },
                                    onClick = { scope.launch { sheetState.bottomSheetState.expand() } },
                                    onNextClick = { sharedPlayerViewModel.next() },
                                    onPreviousClick = { sharedPlayerViewModel.previous() }
                                )
                            }
                            // La navegación principal es siempre la bottom bar,
                            // también en tablet: el ancho de ventana solo cambia
                            // las grillas y el ancho de los detalles.
                            SharedBottomNavigationBar(
                                currentRoute = currentRoute,
                                onHomeClick = navigateToHomeTab,
                                onExploreClick = navigateToExploreTab,
                                onSettingsClick = navigateToSettingsTab,
                            )
                        }
                    }
                }
            ) { innerPaddingNavHost ->
                val navHostModifier =
                    Modifier.padding(bottom = innerPaddingNavHost.calculateBottomPadding())
                // El grafo de destinos se envuelve en una lambda para separar el
                // host de navegación del resto del scaffold.
                val navHost: @Composable (Modifier) -> Unit = { hostModifier ->
                    Box(modifier = hostModifier) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            // No transitions in either direction: forward nav would
                            // default to a 700ms crossfade, and back would still show
                            // the system predictive-back scale during the edge swipe.
                            // enableOnBackInvokedCallback=false in the manifest kills
                            // that system animation; these kill any in-app crossfade.
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) {
                            composable<Routes.Login> {
                                LoginScreen(onLoginSuccess = onLoginSuccess)
                            }
                            composable<Routes.Home> {
                                HomeScreen(
                                    onAlbumClick = { id -> navController.navigate(Routes.AlbumDetail(id)) },
                                    onPlaylistClick = { id -> navController.navigate(Routes.PlaylistDetail(id)) }
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
                                    onNavigateBack = { navController.popBackStack() },
                                    onLogout = onLogout
                                )
                            }
                            composable<Routes.ArtistDetail> { backStackEntry ->
                                ArtistDetailScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onAlbumClick = { id -> navController.navigate(Routes.AlbumDetail(id)) }
                                )
                            }
                            composable<Routes.PlaylistDetail> {
                                PlaylistDetailScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                            composable<Routes.AlbumDetail> { backStackEntry ->
                                AlbumDetailScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
                navHost(navHostModifier)
            }
        }
    }
}
