package com.example.velodrome.presentation.screen.homescreen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.velodrome.data.datasource.CacheService
import com.example.velodrome.ui.theme.TextSecondary
import com.example.velodrome.util.CredentialsManager
import java.io.File

/**
 * Artist avatar image component with local cache.
 * Uses the same image cache as AlbumCover.
 *
 * @param coverUrl The coverArt ID from API (e.g., "ar-123")
 * @param contentDescription Description for accessibility
 * @param modifier Modifier for the component
 * @param size Size of the avatar (default 80.dp)
 */
@Composable
fun ArtistAvatar(
    coverArtId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    // Get ImageCacheDataSource from global singleton
    val imageCacheDataSource = CacheService.imageCacheDataSource

    var cachedImagePath by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Build the cover art URL from the coverArt ID
    val remoteUrl = remember(coverArtId, size) {
        CredentialsManager.getCoverArtUrl(coverArtId, size.value.toInt())
    }

    // Load image with cache
    LaunchedEffect(coverArtId, size) {
        if (coverArtId.isNullOrBlank()) {
            cachedImagePath = null
            return@LaunchedEffect
        }

        if (imageCacheDataSource != null && remoteUrl != null) {
            isLoading = true
            try {
                val path = imageCacheDataSource.getImage(remoteUrl)
                cachedImagePath = path
                Log.d("ArtistAvatar", "Image loaded: path=$path")
            } catch (e: Exception) {
                Log.e("ArtistAvatar", "Error loading image: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            coverArtId.isNullOrBlank() -> {
                PlaceholderAvatar(modifier = Modifier.fillMaxSize())
            }
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(size / 3),
                    color = TextSecondary
                )
            }
            cachedImagePath != null -> {
                // Load from local cache
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(cachedImagePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            remoteUrl != null -> {
                // Fallback: load from remote URL directly
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(remoteUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                PlaceholderAvatar(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * Placeholder shown when no artist image is available.
 */
@Composable
private fun PlaceholderAvatar(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        ),
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