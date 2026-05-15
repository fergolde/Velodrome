package com.example.velodrome.presentation.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.velodrome.R
import com.example.velodrome.domain.model.Album

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onAlbumClick: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            ShuffleButton(onShuffle = { viewModel.playShuffle() })
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Recien añadidos
        item {
            SectionHeader(title = stringResource(R.string.home_recently_added), subtitle = stringResource(R.string.home_new_arrivals))
            Spacer(modifier = Modifier.height(16.dp))
            RecentAlbumsRow(
                albums = state.latestAlbums,
                onAlbumClick = onAlbumClick
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Aleatorios
        item {
            SectionHeader(title = stringResource(R.string.home_random), subtitle = stringResource(R.string.home_discover))
            Spacer(modifier = Modifier.height(16.dp))
            RecentAlbumsRow(
                albums = state.randomAlbums,
                onAlbumClick = onAlbumClick
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Mas reproducidos
        item {
            SectionHeader(title = stringResource(R.string.home_most_played), subtitle = stringResource(R.string.home_your_favorites))
            Spacer(modifier = Modifier.height(16.dp))
            RecentAlbumsRow(
                albums = state.topAlbums,
                onAlbumClick = onAlbumClick
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Recientemente reproducidos
        item {
            SectionHeader(title = stringResource(R.string.home_recently_played), subtitle = stringResource(R.string.home_just_for_you))
            Spacer(modifier = Modifier.height(16.dp))
            RecentAlbumsRow(
                albums = state.recentlyPlayedAlbums,
                onAlbumClick = onAlbumClick
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Explora tu Biblioteca - Feature Cards
        item {
            SectionHeader(title = "Explora tu Biblioteca", subtitle = "MIXES Y DESCUBRIMIENTO")
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    FeatureCard(
                        title = "Local Offline",
                        subtitle = "Sin gastar datos",
                        icon = Icons.Default.OfflinePin,
                        backgroundColor = Color(0xFF0F6E56),
                        onClick = { viewModel.playOfflineOnly() }
                    )
                }
                item {
                    FeatureCard(
                        title = "Top 100",
                        subtitle = "Tus favoritas",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        backgroundColor = Color(0xFF854F0B),
                        onClick = { viewModel.playTop100() }
                    )
                }
                item {
                    FeatureCard(
                        title = "Tesoros Ocultos",
                        subtitle = "Descubrimiento",
                        icon = Icons.Default.TravelExplore,
                        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        onClick = { viewModel.playDiscovery() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ShuffleButton(
    onShuffle: () -> Unit
) {
    Button(
        onClick = onShuffle,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(28.dp)
    ) {
        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.home_shuffle), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    onViewAllClick: (() -> Unit)? = null,
    showActionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column {
        Text(subtitle, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            val actionText = showActionText ?: if (onViewAllClick != null) stringResource(R.string.view_all) else null
            if (actionText != null) {
                Text(
                    text = actionText,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = { onActionClick?.invoke() ?: onViewAllClick?.invoke() })
                )
            }
        }
    }
}

@Composable
fun RecentAlbumsRow(
    albums: List<Album>,
    onAlbumClick: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(albums) { album ->
            AlbumItem(
                album = album,
                onClick = { onAlbumClick(album.id) }
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(width = 160.dp, height = 100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
