package com.example.velodrome.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.velodrome.di.CredentialsEntryPoint
import com.example.velodrome.ui.theme.SurfaceDark
import com.example.velodrome.ui.theme.TextSecondary
import dagger.hilt.android.EntryPointAccessors

@Composable
fun AlbumCover(
    coverArtId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    cornerRadius: Dp = 12.dp
) {
    val context = LocalContext.current

    // HILT SAFE ACCESS
    val credentialsManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CredentialsEntryPoint::class.java
        ).credentialsManager()
    }

    // Generate URL from coverArtId
    val remoteUrl = remember(coverArtId, size) {
        coverArtId?.let {
            credentialsManager.getCoverArtUrl(it, size.value.toInt())
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        when {
            coverArtId.isNullOrBlank() -> {
                PlaceholderCover(modifier = Modifier.fillMaxSize())
            }

            !remoteUrl.isNullOrBlank() -> {
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
                PlaceholderCover(modifier = Modifier.fillMaxSize())
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