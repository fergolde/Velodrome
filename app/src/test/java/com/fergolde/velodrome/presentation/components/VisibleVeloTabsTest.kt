package com.fergolde.velodrome.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Navegación principal: siempre la bottom bar.
 *
 * El rail lateral de tablet se retiró por decisión de producto, así que ya no
 * existe una decisión de layout que fijar. Este archivo conserva lo único que
 * seguía siendo compartido y puro: la lista de destinos que la barra dibuja,
 * para que su orden y su filtrado no cambien al quitar el rail.
 */
class VisibleVeloTabsTest {

    @Test
    fun `las tres pestañas se dibujan en el orden declarado`() {
        assertEquals(
            listOf(VeloTab.Home, VeloTab.Explore, VeloTab.Settings),
            visibleVeloTabs()
        )
    }

    @Test
    fun `ocultar destinos conserva el orden de los demas`() {
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
