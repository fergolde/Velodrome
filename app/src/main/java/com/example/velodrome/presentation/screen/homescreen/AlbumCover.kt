package com.example.velodrome.presentation.screen.homescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.velodrome.ui.theme.SurfaceDark
import com.example.velodrome.ui.theme.TextSecondary
import com.example.velodrome.util.CredentialsManager

/**
 * Album cover image component with Coil for loading images.
 *
 * @param coverUrl URL of the cover image
 * @param contentDescription Description for accessibility
 * @param modifier Modifier for the component
 * @param size Size of the cover (default 160.dp)
 * @param cornerRadius Corner radius for the shape
 */
@Composable
fun AlbumCover(
    coverArtId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    cornerRadius: Dp = 12.dp
) {
    // Build the cover art URL from the coverArt ID
    val coverUrl = remember(coverArtId, size) {
        val url = CredentialsManager.getCoverArtUrl(coverArtId, size.value.toInt())
        Log.d("AlbumCover", "coverArtId='$coverArtId' -> URL=${url?.take(80)}")
        url
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl.isNullOrBlank()) {
            Log.w("AlbumCover", "NULL cover URL for coverArtId: $coverArtId")
            PlaceholderCover(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Album cover with custom size.
 *
 * @param coverUrl URL of the cover image
 * @param modifier Modifier for the component
 */
@Composable
fun AlbumCoverSmall(
    coverArtId: String?,
    modifier: Modifier = Modifier
) {
    AlbumCover(
        coverArtId = coverArtId,
        contentDescription = null,
        modifier = modifier,
        size = 120.dp,
        cornerRadius = 8.dp
    )
}

/**
 * Album cover for featured section.
 *
 * @param coverArtId The coverArt ID from the API
 * @param modifier Modifier for the component
 */
@Composable
fun AlbumCoverLarge(
    coverArtId: String?,
    modifier: Modifier = Modifier
) {
    AlbumCover(
        coverArtId = coverArtId,
        contentDescription = null,
        modifier = modifier,
        size = 180.dp,
        cornerRadius = 16.dp
    )
}

/**
 * Placeholder shown when no cover image is available.
 *
 * @param modifier Modifier for the component
 */
@Composable
private fun PlaceholderCover(
    modifier: Modifier = Modifier
) {
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

/**
 * Full-size album cover that fills parent.
 *
 * @param coverUrl URL of the cover image
 * @param contentDescription Description for accessibility
 * @param modifier Modifier for the component
 */
@Composable
fun AlbumCoverFull(
    coverUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl.isNullOrBlank()) {
            PlaceholderCover(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}