package com.example.velodrome.presentation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.example.velodrome.presentation.components.MiniPlayer
import com.example.velodrome.presentation.navigation.Screen
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

object PlayerState {
    var isVisible by mutableStateOf(false)
    private var _hasSong by mutableStateOf(false)

    fun updateHasSong(has: Boolean) {
        _hasSong = has
    }

    val hasSong: Boolean get() = _hasSong
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
                startDestination = if (isLoggedIn == true) Screen.Home.route else Screen.Login.route,
                onLoginSuccess = { isLoggedIn = true }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    navController: NavHostController,
    startDestination: String,
    onLoginSuccess: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    // Track song state from PlayerManager
    val currentTrack by PlayerManager.currentTrack.collectAsState()
    val isPlaying by PlayerManager.isPlaying.collectAsState()
    val currentPosition by PlayerManager.currentPosition.collectAsState()
    val hasSong = currentTrack != null
    val scope = rememberCoroutineScope()

    // Bottom sheet state
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    // Peek height: 0dp — mini player now lives in the bottom bar
    val sheetPeekHeight = 0.dp

    // Sheet content: only PlayerScreen when expanded
    val sheetContent: @Composable ColumnScope.() -> Unit = {
        when (sheetState.bottomSheetState.currentValue) {
            SheetValue.Expanded -> {
                PlayerScreen(
                    onMinimizeClick = {
                        scope.launch { sheetState.bottomSheetState.partialExpand() }
                    },
                    onHomeClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onExploreClick = {
                        navController.navigate(Screen.Explore.route)
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onQueueClick = { }
                )
            }
            SheetValue.PartiallyExpanded,
            SheetValue.Hidden -> { /* no-op */ }
        }
    }

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = sheetPeekHeight,
        sheetContent = sheetContent,
        sheetDragHandle = {
            if (hasSong) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFAAAAAA))
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues: PaddingValues ->
        Scaffold(
            bottomBar = {
                Column {
                    // MiniPlayer above NavBar — only when sheet is collapsed
                    if (hasSong && sheetState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                        MiniPlayer(
                            modifier = Modifier.fillMaxWidth(),
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            onPlayPauseClick = { PlayerManager.togglePlayPause() },
                            onClick = {
                                scope.launch { sheetState.bottomSheetState.expand() }
                            },
                            onNextClick = { PlayerManager.next() },
                            onPreviousClick = { PlayerManager.previous() }
                        )
                    }
                    // NavBar — always visible
                    SharedBottomNavigationBar(
                        currentRoute = currentRoute,
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
        ) { scaffoldPadding: PaddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
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
                            onPlayerClick = {
                                scope.launch { sheetState.bottomSheetState.expand() }
                            },
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
        }
    }
}