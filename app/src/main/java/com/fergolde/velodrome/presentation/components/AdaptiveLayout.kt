package com.fergolde.velodrome.presentation.components

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ancho máximo de la columna central de un detalle en tablet (S1).
 *
 * Es el mismo tope para álbum, artista y playlist, para que los tres detalles se
 * lean igual. Vive aquí, y no en cada pantalla, porque antes la constante estaba
 * triplicada y podía desincronizarse. Queda por encima de los 840dp de ancho
 * máximo de la clase `Medium`, así que solo recorta de verdad en `Expanded`.
 */
val DetailsContentMaxWidth: Dp = 880.dp

/**
 * Columnas de una grilla según la clase de ancho de la ventana (S1).
 *
 * `Compact` es el teléfono y conserva las columnas de siempre; `Medium` (tablet
 * vertical) y `Expanded` (tablet ancha) suman columnas. Cualquier clase que no
 * se reconozca cae en [compact], el layout con menos superficie de cambio.
 *
 * @param compact columnas en teléfono.
 * @param medium columnas en tablet vertical.
 * @param expanded columnas en tablet ancha.
 */
fun adaptiveGridColumns(
    widthSizeClass: WindowWidthSizeClass,
    compact: Int,
    medium: Int,
    expanded: Int,
): Int = when (widthSizeClass) {
    WindowWidthSizeClass.Medium -> medium
    WindowWidthSizeClass.Expanded -> expanded
    else -> compact
}

/**
 * Ancho máximo del contenido de un detalle según la clase de ancho (S1).
 *
 * `Dp.Infinity` significa "sin tope": el teléfono conserva el ancho completo y
 * la tablet acota la columna central a [DetailsContentMaxWidth].
 */
fun detailsContentMaxWidth(widthSizeClass: WindowWidthSizeClass): Dp =
    if (widthSizeClass == WindowWidthSizeClass.Compact) Dp.Infinity else DetailsContentMaxWidth

/**
 * Clase de ancho de ventana vigente, leída desde la composición (S1).
 *
 * La Activity anfitriona se toma del `CompositionLocal` [LocalActivity], que es
 * la única fuente de tamaño en este proyecto: las pantallas no reciben un
 * `Context` ni un `WindowSizeClass` por parámetro. Sin Activity (previews y
 * tests) se asume `Compact`, el layout del teléfono, que es el que no cambia.
 *
 * No usa `remember`: `calculateWindowSizeClass` ya es `@Composable` y se
 * recalcula solo cuando la composición se invalida por cambio de tamaño, así
 * que memorizarlo solo agregaría una clave que mantener.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun currentWindowWidthSizeClass(): WindowWidthSizeClass {
    val activity = LocalActivity.current
    return if (activity == null) {
        WindowWidthSizeClass.Compact
    } else {
        calculateWindowSizeClass(activity).widthSizeClass
    }
}
