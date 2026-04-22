package com.example.velodrome.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.velodrome.presentation.UiConstants.miniPlayerMargintoNavigationBar
import com.example.velodrome.presentation.player.PlayerManager

@Composable
fun MiniPlayerOverlay(
    onPlayerClick: () -> Unit
) {
    val currentTrack by PlayerManager.currentTrack.collectAsState()
    val currentPosition by PlayerManager.currentPosition.collectAsState()
    val isPlaying by PlayerManager.isPlaying.collectAsState()

    if (currentTrack != null) {
        MiniPlayer(
            modifier = Modifier.fillMaxWidth(),
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            onPlayPauseClick = { PlayerManager.togglePlayPause() },
            onClick = onPlayerClick,
            onNextClick = { PlayerManager.next() },
            onPreviousClick = { PlayerManager.previous() }
        )
        Spacer(modifier = Modifier.height(miniPlayerMargintoNavigationBar))
    }
}