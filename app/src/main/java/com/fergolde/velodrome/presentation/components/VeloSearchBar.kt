package com.fergolde.velodrome.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fergolde.velodrome.R
import com.fergolde.velodrome.ui.theme.DmSansFontFamily
import com.fergolde.velodrome.ui.theme.VeloPalette

/**
 * Barra de búsqueda del proyecto.
 *
 * @param onFocusChanged avisa de si el campo tiene el foco; quien lo use puede
 *   abrir o cerrar el desplegable de sugerencias de [VeloSearchDropdown]. Por
 *   defecto no hace nada, así las pantallas que no usan desplegable conservan
 *   el comportamiento de siempre.
 */
@Composable
fun VeloSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = stringResource(R.string.explore_search_hint),
    onFocusChanged: (Boolean) -> Unit = {},
) {
    // El overload con `value` está deprecado: el estado vive en TextFieldState.
    // La firma hacia el padre no cambia (String + callback): este puente
    // vuelca las ediciones con snapshotFlow y refleja limpiezas externas
    // (botón X, sugerencias) solo cuando difieren, para no mover el cursor
    // mientras se escribe.
    val textState = rememberTextFieldState(query)
    LaunchedEffect(textState) {
        snapshotFlow { textState.text.toString() }.collect(onQueryChange)
    }
    LaunchedEffect(query) {
        if (query != textState.text.toString()) {
            textState.setTextAndPlaceCursorAtEnd(query)
        }
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = ButtonDefaults.outlinedButtonBorder(true).copy(
            brush = SolidColor(VeloPalette.Border),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            BasicTextField(
                state = textState,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                lineLimits = TextFieldLineLimits.SingleLine,
                decorator = TextFieldDecorator { innerTextField ->
                    if (textState.text.isEmpty()) {
                        Text(
                            text = hint,
                            fontFamily = DmSansFontFamily,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                },
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.action_clear_search),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Desplegable anclado a la barra de búsqueda, al estilo autocomplete.
 *
 * Se dibuja dentro de la composición y no en una ventana propia a propósito:
 * `DropdownMenu` y `ExposedDropdownMenuBox` abren un `Popup` con foco, que le
 * quitaría el foco al `BasicTextField` de [VeloSearchBar] y cortaría el filtrado
 * mientras se escribe; el proyecto solo usa `DropdownMenu` para menús de
 * opciones sin campo de texto enfocado (ArtistDetailScreen). Aquí el campo
 * conserva el foco y el teclado, y la capa transparente se encarga del cierre
 * por toque fuera.
 *
 * @param anchorOffset esquina superior izquierda del desplegable respecto al
 *   contenedor que lo pinta; normalmente el borde inferior de la barra.
 * @param width ancho del desplegable, que se iguala al de la barra para que se
 *   lea como una continuación de ella.
 * @param onDismiss se llama al tocar fuera de la tarjeta.
 */
@Composable
fun VeloSearchDropdown(
    anchorOffset: IntOffset,
    width: Dp,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Capa transparente por debajo de la barra: cierra el desplegable al
        // tocar fuera de la tarjeta. Empieza en el borde inferior de la barra,
        // así que tocar el campo de texto sigue funcionando con el desplegable
        // abierto (recolocar el cursor, seguir escribiendo).
        Box(
            modifier = Modifier
                .offset { anchorOffset }
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
        )
        // La tarjeta va después de la capa para ganar el toque en su área.
        Surface(
            modifier = Modifier
                .offset { anchorOffset }
                .width(width)
                .heightIn(max = 360.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, VeloPalette.Border),
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                content = content,
            )
        }
    }
}
