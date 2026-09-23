package com.fergolde.velodrome.presentation.screen.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.request.ImageRequest

enum class ArtworkSize(val dp: Dp) {
    Thumbnail(64.dp),
    Avatar(96.dp),
    Card(128.dp),
    Grid(192.dp),
    Detail(512.dp),
}

fun canonicalArtworkSize(requestedSize: Dp): ArtworkSize = when {
    requestedSize <= 0.dp -> ArtworkSize.Detail
    requestedSize <= 64.dp -> ArtworkSize.Thumbnail
    requestedSize <= 112.dp -> ArtworkSize.Avatar
    requestedSize <= 144.dp -> ArtworkSize.Card
    requestedSize <= 352.dp -> ArtworkSize.Grid
    else -> ArtworkSize.Detail
}

fun artworkImageRequest(
    context: Context,
    coverArtId: String,
    requestedSize: Dp,
    density: Float,
): ImageRequest {
    val size = canonicalArtworkSize(requestedSize)
    val pixels = (size.dp.value * density).toInt().coerceAtLeast(1)
    return ImageRequest.Builder(context)
        .data(coverArtId)
        .size(pixels, pixels)
        .build()
}

@Composable
fun rememberArtworkImageRequest(coverArtId: String, requestedSize: Dp): ImageRequest {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    return remember(coverArtId, requestedSize, density) {
        artworkImageRequest(context, coverArtId, requestedSize, density)
    }
}
