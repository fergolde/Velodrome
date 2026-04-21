package com.example.velodrome.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.velodrome.R
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.screen.homescreen.AlbumCover

// Theme tokens needed for MiniPlayer
private val SurfaceContainer = androidx.compose.ui.graphics.Color(0xFF222532)
//private val AccentPurple = androidx.compose.ui.graphics.Color(0xFFB6A0FF)
private val TextPrimary = androidx.compose.ui.graphics.Color(0xFFF0F0FD)
private val TextSecondary = androidx.compose.ui.graphics.Color(0xFFAAAAB7)
private val BackgroundDark = androidx.compose.ui.graphics.Color(0xFF0C0E17)

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    currentTrack: Track?,
    isPlaying: Boolean,
    currentPosition: Long = 0L,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {}
) {
    // Convert position from ms to seconds for progress calculation
    val positionSec = (currentPosition / 1000).toInt()
    val progress = if (currentTrack != null && currentTrack.durationSec > 0) {
        positionSec.toFloat() / currentTrack.durationSec.toFloat()
    } else {
        0f
    }

    Column(modifier = modifier) {
        // Progress bar at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(SurfaceContainer)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BackgroundDark.copy(alpha = 0.95f))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainer)
            ) {
                AlbumCover(
                    coverArtId = currentTrack?.coverArtId,
                    contentDescription = null,
                    size = 48.dp,
                    cornerRadius = 8.dp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTrack?.title ?: stringResource(R.string.player_unknown_track),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = currentTrack?.artistName ?: stringResource(R.string.artists_unknown),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = stringResource(R.string.player_previous),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.clickable { onPreviousClick() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onPlayPauseClick() }
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = BackgroundDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.SkipNext,
                contentDescription = stringResource(R.string.player_next),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.clickable { onNextClick() }
            )
        }
    }
}