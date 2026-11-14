package com.fergolde.velodrome.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fergolde.velodrome.R
import com.fergolde.velodrome.ui.theme.DmSansFontFamily

/**
 * Pestañas de la barra inferior compartida.
 *
 * Existen para que la barra no compare literales de ruta sueltos: cada pestaña
 * es el destino tipado de `Routes` que representa. `null` significa "ninguna
 * pestaña activa", el caso del reproductor a pantalla completa.
 */
enum class VeloTab(val route: String) {
    Home("home"),
    Explore("explore"),
    Settings("settings");

    companion object {
        /** Resuelve la pestaña que corresponde a [route], o `null` si no hay coincidencia. */
        fun fromRoute(route: String?): VeloTab? = entries.firstOrNull { it.route == route }
    }
}

/**
 * Aire inferior común de las listas que viven detrás del mini-player.
 *
 * Antes cada pantalla elegía 36dp, 100dp o nada: el último elemento quedaba
 * tapado por el mini-player en cuanto la pantalla no declaraba su propio hueco.
 */
val VeloListBottomClearance = 100.dp

/**
 * Snackbar compartido: mismo color, radio y tipografía en todas las pantallas.
 *
 * Sustituye las copias locales de `SnackbarHost` que había en las pantallas de
 * detalle y en la búsqueda.
 */
@Composable
fun VeloSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = data.visuals.message,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Estado de error con acción de reintento.
 *
 * Antes cada pantalla de detalle dibujaba un `Text` suelto con el mensaje de
 * error: sin botón, la única salida era volver atrás. Este composable es la
 * versión compartida de ese bloque para todos los detalles.
 */
@Composable
fun VeloErrorWithRetry(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = DmSansFontFamily,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = stringResource(R.string.retry),
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Bottom Navigation Bar unificada - reusable en TODOS los screens.
 * Reemplaza las 7+ BottomNavigationBars duplicadas en el proyecto.
 */
@Composable
fun SharedBottomNavigationBar(
    modifier: Modifier = Modifier,
    selectedTab: VeloTab? = VeloTab.Home,
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
                label = { Text(stringResource(R.string.nav_home)) },
                selected = selectedTab == VeloTab.Home,
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
                label = { Text(stringResource(R.string.nav_explore_label)) },
                selected = selectedTab == VeloTab.Explore,
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
                label = { Text(stringResource(R.string.nav_settings)) },
                selected = selectedTab == VeloTab.Settings,
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

/**
 * Sobrecarga transitoria para los llamadores que todavía derivan la ruta a mano
 * (`VelodromeApp`). Traduce la ruta a [VeloTab] y delega en la versión tipada,
 * así ambos llamadores comparten una única lógica de selección.
 */
@Composable
fun SharedBottomNavigationBar(
    modifier: Modifier = Modifier,
    currentRoute: String,
    showHome: Boolean = true,
    showExplore: Boolean = true,
    showSettings: Boolean = true,
    onHomeClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    SharedBottomNavigationBar(
        modifier = modifier,
        selectedTab = VeloTab.fromRoute(currentRoute),
        showHome = showHome,
        showExplore = showExplore,
        showSettings = showSettings,
        onHomeClick = onHomeClick,
        onExploreClick = onExploreClick,
        onSettingsClick = onSettingsClick
    )
}
