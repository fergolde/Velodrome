package com.example.velodrome.presentation.screen.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.velodrome.R
import com.example.velodrome.domain.model.Album
import com.example.velodrome.presentation.components.VeloSectionHeader
import com.example.velodrome.ui.theme.DmSansFontFamily
import com.example.velodrome.ui.theme.VeloPalette

// ─── SCREEN ──────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onAlbumClick: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {

        // ── Shuffle button ─────────────────────────────────────────────────
        item {
            ShuffleButton(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                onShuffle = { viewModel.playShuffle() }
            )
            Spacer(Modifier.height(36.dp))
        }

        // ── Recién Añadidos ────────────────────────────────────────────────
        item {
            VeloSectionHeader(
                eyebrow = stringResource(R.string.home_new_arrivals),
                title   = stringResource(R.string.home_recently_added),
                modifier = Modifier.padding(horizontal = 20.dp),
                onViewAll = null
            )
            Spacer(Modifier.height(16.dp))
            AlbumCarousel(albums = state.latestAlbums, onAlbumClick = onAlbumClick)
            Spacer(Modifier.height(36.dp))
        }

        // ── Aleatorios ─────────────────────────────────────────────────────
        item {
            VeloSectionHeader(
                eyebrow = stringResource(R.string.home_discover),
                title   = stringResource(R.string.home_random),
                modifier = Modifier.padding(horizontal = 20.dp),
                onViewAll = null
            )
            Spacer(Modifier.height(16.dp))
            AlbumCarousel(albums = state.randomAlbums, onAlbumClick = onAlbumClick)
            Spacer(Modifier.height(36.dp))
        }

        // ── Más Reproducidos ───────────────────────────────────────────────
        item {
            VeloSectionHeader(
                eyebrow = stringResource(R.string.home_your_favorites),
                title   = stringResource(R.string.home_most_played),
                modifier = Modifier.padding(horizontal = 20.dp),
                onViewAll = null
            )
            Spacer(Modifier.height(16.dp))
            AlbumCarousel(albums = state.topAlbums, onAlbumClick = onAlbumClick)
            Spacer(Modifier.height(36.dp))
        }

        // ── Recientemente Reproducidos ─────────────────────────────────────
        item {
            VeloSectionHeader(
                eyebrow = stringResource(R.string.home_just_for_you),
                title   = stringResource(R.string.home_recently_played),
                modifier = Modifier.padding(horizontal = 20.dp),
                onViewAll = null
            )
            Spacer(Modifier.height(16.dp))
            AlbumCarousel(albums = state.recentlyPlayedAlbums, onAlbumClick = onAlbumClick)
            Spacer(Modifier.height(36.dp))
        }

        // ── Explora ────────────────────────────────────────────────────────
        item {
            VeloSectionHeader(
                eyebrow = "Mixes y Descubrimiento",
                title   = "Explora",
                modifier = Modifier.padding(horizontal = 20.dp),
                onViewAll = null
            )
            Spacer(Modifier.height(16.dp))
            FeatureCardsRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                onOfflineClick    = { viewModel.playOfflineOnly() },
                onTop100Click     = { viewModel.playTop100() },
                onDiscoveryClick  = { viewModel.playDiscovery() },
            )
            Spacer(Modifier.height(100.dp)) // mini-player clearance
        }
    }
}


// ─── SHUFFLE BUTTON ───────────────────────────────────────────────────────────

@Composable
fun ShuffleButton(
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "shuffleScale",
    )

    Button(
        onClick = {
            pressed = true
            onShuffle()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
        ),
        shape = RoundedCornerShape(26.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.home_shuffle),
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(120)
            pressed = false
        }
    }
}

// ─── SECTION HEADER ──────────────────────────────────────────────────────────



// ─── ALBUM CAROUSEL ───────────────────────────────────────────────────────────

@Composable
fun AlbumCarousel(
    albums: List<Album>,
    onAlbumClick: (String) -> Unit,
    artSize: Dp = 130.dp,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            VeloAlbumCard(
                album = album,
                artSize = artSize,
                onClick = { onAlbumClick(album.id) },
            )
        }
    }
}

@Composable
fun VeloAlbumCard(
    album: Album,
    artSize: Dp = 130.dp,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(artSize)
            .clickable(onClick = onClick),
    ) {
        AlbumCover(
            coverArtId      = album.coverUrl,
            contentDescription = album.title,
            size            = artSize,
            cornerRadius    = 14.dp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = album.title,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = album.artistName,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

// ─── FEATURE CARDS ───────────────────────────────────────────────────────────

@Composable
private fun FeatureCardsRow(
    modifier: Modifier = Modifier,
    onOfflineClick:   () -> Unit,
    onTop100Click:    () -> Unit,
    onDiscoveryClick: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VeloFeatureCard(
            modifier = Modifier.weight(1f),
            title    = stringResource(R.string.home_feature_offline_title),
            subtitle = stringResource(R.string.home_feature_offline_subtitle),
            icon     = Icons.Default.OfflinePin,
            bgColor  = VeloPalette.FeatureOffline,
            onClick  = onOfflineClick,
        )

        VeloFeatureCard(
            modifier = Modifier.weight(1f),
            title    = stringResource(R.string.home_feature_top100_title),
            subtitle = stringResource(R.string.home_feature_top100_subtitle),
            icon     = Icons.AutoMirrored.Filled.TrendingUp,
            bgColor  = VeloPalette.FeatureTop100,
            onClick  = onTop100Click,
        )

        VeloFeatureCard(
            modifier = Modifier.weight(1f),
            title    = stringResource(R.string.home_feature_discovery_title),
            subtitle = stringResource(R.string.home_feature_discovery_subtitle),
            icon     = Icons.Default.TravelExplore,
            bgColor  = VeloPalette.FeatureDiscovery,
            onClick  = onDiscoveryClick,
        )
    }
}

@Composable
fun VeloFeatureCard(
    title:    String,
    subtitle: String,
    icon:     ImageVector,
    bgColor:  Color,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier  = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        color     = bgColor,
        tonalElevation = 0.dp,
    ) {
        Box {
            // Decorative circle in top-right corner
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .offset(x = 10.dp, y = (-10).dp)
                    .align(Alignment.TopEnd)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp),
                )
                Column {
                    Text(
                        text = title,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.White,
                    )
                    Text(
                        text = subtitle,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}