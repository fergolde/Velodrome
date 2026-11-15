package com.fergolde.velodrome.presentation.components

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * S1: el ancho de ventana decide la navegación principal.
 *
 * El teléfono (`Compact`) conserva la bottom bar histórica tal cual; la tablet
 * (`Medium` y `Expanded`) pasa a un rail lateral con los mismos destinos. La
 * decisión y la lista de destinos viven en funciones puras para poder fijarlas
 * en un test JVM, sin inflar el árbol de Compose.
 */
class NavigationLayoutTest {

    @Test
    fun `el ancho compacto conserva la bottom bar del telefono`() {
        assertEquals(
            VeloNavigationLayout.BottomBar,
            navigationLayoutFor(WindowWidthSizeClass.Compact)
        )
    }

    @Test
    fun `medium y expanded usan el rail lateral`() {
        assertEquals(
            VeloNavigationLayout.SideRail,
            navigationLayoutFor(WindowWidthSizeClass.Medium)
        )
        assertEquals(
            VeloNavigationLayout.SideRail,
            navigationLayoutFor(WindowWidthSizeClass.Expanded)
        )
    }

    @Test
    fun `la barra y el rail exponen los mismos tres destinos en el mismo orden`() {
        assertEquals(
            listOf(VeloTab.Home, VeloTab.Explore, VeloTab.Settings),
            visibleVeloTabs()
        )
    }

    @Test
    fun `ocultar destinos funciona igual en cualquier layout`() {
        assertEquals(
            listOf(VeloTab.Explore),
            visibleVeloTabs(showHome = false, showSettings = false)
        )
        assertEquals(
            emptyList<VeloTab>(),
            visibleVeloTabs(showHome = false, showExplore = false, showSettings = false)
        )
    }
}
