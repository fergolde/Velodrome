package com.fergolde.velodrome.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Bottom Navigation Bar unificada - reusable en TODOS los screens.
 * Reemplaza las 7+ BottomNavigationBars duplicadas en el proyecto.
 */
@Composable
fun SharedBottomNavigationBar(
    modifier: Modifier = Modifier,
    currentRoute: String = "home",
    showHome: Boolean = true,
    showExplore: Boolean = true,
    showSettings: Boolean = true,
    onHomeClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {

    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 0.dp
    ) {
        // Home
        if (showHome) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Home") },
                selected = currentRoute == "home",
                onClick = {
                    onHomeClick() 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        // Explore
        if (showExplore) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                label = { Text("Explore") },
                selected = currentRoute == "explore",
                onClick = {
                    onExploreClick() 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        // Settings
        if (showSettings) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Settings") },
                selected = currentRoute == "settings",
                onClick = {
                    onSettingsClick() 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}