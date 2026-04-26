package com.example.velodrome.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.velodrome.presentation.screen.home.AlbumCover

@Composable
fun UniversalOptionsSheet(
    title: String,
    subtitle: String,
    coverArtId: String?,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumCover(
                coverArtId = coverArtId,
                contentDescription = title,
                size = 44.dp,
                cornerRadius = 8.dp
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))

        SheetOptionItem(
            icon = Icons.Default.PlayArrow,
            iconTint = MaterialTheme.colorScheme.primary,
            iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            title = "Reproducir ahora",
            subtitle = "Salta a esta selección inmediatamente",
            onClick = onPlayNow
        )
        SheetOptionItem(
            icon = Icons.Default.SkipNext,
            iconTint = Color(0xFF0F6E56),
            iconBackground = Color(0xFF0F6E56).copy(alpha = 0.12f),
            title = "Reproducir siguiente",
            subtitle = "Se pone justo después de la actual",
            onClick = onPlayNext
        )
        SheetOptionItem(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            iconTint = Color(0xFF854F0B),
            iconBackground = Color(0xFF854F0B).copy(alpha = 0.12f),
            title = "Añadir al final",
            subtitle = "Se añade al final de la cola",
            onClick = onAddToQueue
        )
    }
}

@Composable
private fun SheetOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}