package com.example.velodrome.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.velodrome.ui.theme.TextSecondary

@Composable
fun ArtistAvatar(
    coverArtId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            coverArtId.isNullOrBlank() -> {
                PlaceholderAvatar(modifier = Modifier.fillMaxSize())
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
private fun PlaceholderAvatar(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}