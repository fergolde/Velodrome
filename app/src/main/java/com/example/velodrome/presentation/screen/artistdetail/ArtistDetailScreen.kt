package com.example.velodrome.presentation.screen.artistdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.velodrome.R
import com.example.velodrome.domain.model.Album
import com.example.velodrome.presentation.screen.home.AlbumCover
import com.example.velodrome.presentation.screen.home.ArtistAvatar

@Composable
fun ArtistDetailScreen(
    artistId: String,
    onBackClick: () -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Usamos Box para que el botón de atrás flote sobre la lista
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            uiState.error != null -> {
                Text(
                    text = uiState.error ?: stringResource(R.string.error_loading),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                ArtistAlbumsList(
                    artist = uiState.artist,
                    albums = uiState.albums,
                    onAlbumClick = onAlbumClick
                )
            }
        }

        // BOTÓN DE ATRÁS: Flota arriba a la izquierda
        // Usamos statusBarsPadding() AQUÍ para que no toque el borde físico pero el contenido sí
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding() // Se ajusta debajo del reloj/iconos de sistema
                .padding(8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f)) // Fondo sutil para legibilidad
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.nav_back),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ArtistAlbumsList(
    artist: com.example.velodrome.domain.model.Artist?,
    albums: List<Album>,
    onAlbumClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // HEADER DEL ARTISTA: Ahora es vertical y centrado para aprovechar el borde superior
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp), // Un poco de espacio respecto al borde superior
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Imagen circular grande
                Box(
                    modifier = Modifier
                        .statusBarsPadding() // Espacio para que la imagen no empiece "dentro" del notch
                        .padding(top = 24.dp) // Padding extra para que quede debajo del botón de atrás
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    ArtistAvatar(
                        coverArtId = artist?.coverUrl,
                        contentDescription = artist?.name,
                        size = 180.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nombre del artista
                Text(
                    text = artist?.name ?: "",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = stringResource(R.string.artist_detail_albums_count, artist?.albumCount ?: 0).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de acción rápidos
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { /* Implementar play all */ },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reproducir")
                    }

                    FilledTonalIconButton(onClick = { /* Shuffle */ }) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                    }

                    FilledTonalIconButton(onClick = { /* Queue */ }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Título de sección
        item {
            Text(
                text = stringResource(R.string.artist_detail_albums),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Grid de álbumes (2 columnas)
        val albumPairs = albums.chunked(2)
        items(albumPairs) { rowAlbums ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowAlbums.forEach { album ->
                    ArtistAlbumCard(
                        album = album,
                        onClick = { onAlbumClick(album.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowAlbums.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ArtistAlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AlbumCover(
                coverArtId = album.coverUrl,
                contentDescription = album.title,
                size = 200.dp,
                cornerRadius = 12.dp,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            fontSize = 14.sp
        )
        Text(
            text = album.year?.toString() ?: "",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}