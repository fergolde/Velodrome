package com.example.velodrome.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.velodrome.ui.theme.SurfaceDark
import com.example.velodrome.ui.theme.TextSecondary

@Composable
fun AlbumCover(
    coverArtId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .then(if (size > 0.dp) Modifier.size(size) else Modifier)
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        when {
            coverArtId.isNullOrBlank() -> {
                PlaceholderCover(modifier = Modifier.fillMaxSize())
            }

            else -> {
                // Coil 3 + NavidromeImageInterceptor se encarga de la autenticación
                // pasando directamente el coverArtId
                AsyncImage(
                    model = coverArtId,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun PlaceholderCover(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Album,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(48.dp)
        )
    }
}