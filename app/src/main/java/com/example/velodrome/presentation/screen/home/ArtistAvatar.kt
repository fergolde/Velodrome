package com.example.velodrome.presentation.screen.home

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
import com.example.velodrome.data.datasource.CacheService
import com.example.velodrome.di.CredentialsEntryPoint
import com.example.velodrome.ui.theme.TextSecondary
import dagger.hilt.android.EntryPointAccessors
import java.io.File

@Composable
fun ArtistAvatar(
    coverArtId: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {

    val context = LocalContext.current

    // 🔥 HILT SAFE ACCESS (sin singleton roto)
    val credentialsManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CredentialsEntryPoint::class.java
        ).credentialsManager()
    }

    val imageCacheDataSource = CacheService.imageCacheDataSource

    var cachedImagePath by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // 🔥 URL SEGURA
    val remoteUrl = remember(coverArtId, size) {
        coverArtId?.let {
            credentialsManager.getCoverArtUrl(
                it,
                size.value.toInt()
            )
        }
    }

    // 🔥 LOAD IMAGE
    LaunchedEffect(coverArtId, remoteUrl) {

        if (coverArtId.isNullOrBlank() || remoteUrl.isNullOrBlank()) {
            cachedImagePath = null
            return@LaunchedEffect
        }

        isLoading = true

        try {
            val path = imageCacheDataSource?.getImage(remoteUrl)
            cachedImagePath = path
            Log.d("ArtistAvatar", "Loaded image: $path")

        } catch (e: Exception) {
            Log.e("ArtistAvatar", "Error loading image", e)
        } finally {
            isLoading = false
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
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(cachedImagePath!!))
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
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
                PlaceholderAvatar(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

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