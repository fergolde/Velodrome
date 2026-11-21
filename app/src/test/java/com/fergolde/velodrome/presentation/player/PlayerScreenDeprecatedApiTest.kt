package com.fergolde.velodrome.presentation.player

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guarda de regresión: `allWarningsAsErrors = false`, así que volver a las
 * APIs deprecadas de Material3 en PlayerScreen.kt solo dejaría un warning.
 * Cubre el sheet (`enabledValues = setOf(Hidden, Expanded)` equivale al viejo
 * `skipPartiallyExpanded = true`) y el Slider con SliderState sincronizado
 * por recomposición.
 */
class PlayerScreenDeprecatedApiTest {

    private val source: String = listOf(
        "src/main/java/com/fergolde/velodrome/presentation/player/PlayerScreen.kt",
        "app/src/main/java/com/fergolde/velodrome/presentation/player/PlayerScreen.kt",
    )
        .map(::File)
        .firstOrNull(File::isFile)
        ?.readText()
        ?: error("PlayerScreen.kt no encontrado (cwd=${File(".").absolutePath})")

    @Test
    fun `no usa rememberModalBottomSheetState`() {
        assertFalse(
            "Deprecada en Material3 1.5.0; usa rememberBottomSheetState",
            "rememberModalBottomSheetState" in source,
        )
    }

    @Test
    fun `no usa el overload deprecado de Slider con value`() {
        assertFalse(
            "Slider(value = ...) está deprecado; usa Slider(state = rememberSliderState(), ...)",
            Regex("""Slider\(\s*value\s*=""").containsMatchIn(source),
        )
    }

    @Test
    fun `el sheet solo habilita Hidden y Expanded desde Hidden`() {
        assertTrue(
            "initialValue = SheetValue.Hidden",
            Regex("""initialValue\s*=\s*SheetValue\.Hidden""").containsMatchIn(source),
        )
        assertTrue(
            "enabledValues = setOf(Hidden, Expanded)",
            Regex("""enabledValues\s*=\s*setOf\(\s*SheetValue\.Hidden\s*,\s*SheetValue\.Expanded\s*\)""")
                .containsMatchIn(source),
        )
    }

    @Test
    fun `el slider sincroniza el SliderState conservando la rama de arrastre`() {
        assertTrue("rememberSliderState()", "rememberSliderState()" in source)
        assertTrue(
            "Slider(state = sliderState)",
            Regex("""Slider\(\s*state\s*=\s*sliderState""").containsMatchIn(source),
        )
        assertTrue(
            "sync por recomposición",
            "sliderState.value = if (isDragging) dragProgress else progress" in source,
        )
    }
}
