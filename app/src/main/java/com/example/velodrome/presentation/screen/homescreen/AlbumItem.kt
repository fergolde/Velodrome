package com.example.velodrome.presentation.screen.homescreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.velodrome.domain.model.Album
import com.example.velodrome.ui.theme.TextPrimary
import com.example.velodrome.ui.theme.TextSecondary

/**
 * Reusable album item component.
 * Displays album cover, title, and artist name.
 *
 * @param album The album to display
 * @param onClick Callback when album is clicked
 * @param modifier Modifier for the component
 */
@Composable
fun AlbumItem(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        // Album cover with Coil
        AlbumCover(
            coverArtId = album.coverUrl,
            contentDescription = album.title,
            size = 160.dp,
            cornerRadius = 12.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Album title
        Text(
            text = album.title,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Artist name
        Text(
            text = album.artistName,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Compact album item for lists.
 * Shows smaller cover and less details.
 *
 * @param album The album to display
 * @param onClick Callback when album is clicked
 * @param modifier Modifier for the component
 */
@Composable
fun AlbumItemCompact(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        // Small album cover
        AlbumCoverSmall(
            coverArtId = album.coverUrl
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = album.title,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = album.artistName,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Large album item for featured sections.
 *
 * @param album The album to display
 * @param onClick Callback when album is clicked
 * @param modifier Modifier for the component
 */
@Composable
fun AlbumItemLarge(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(180.dp)
            .clickable { onClick() }
    ) {
        // Large album cover
        AlbumCoverLarge(
            coverArtId = album.coverUrl
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.title,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = album.artistName,
            color = TextSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        album.year?.let { year ->
            Text(
                text = year.toString(),
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}