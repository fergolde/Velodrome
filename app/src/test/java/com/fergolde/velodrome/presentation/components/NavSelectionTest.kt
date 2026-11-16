package com.fergolde.velodrome.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** S1/T2: la selección de la navegación principal no regresó. */
class NavSelectionTest {

    @Test
    fun `los destinos raiz marcan su propia pestaña`() {
        assertEquals(VeloTab.Home, VeloTab.fromRoute("home"))
        assertEquals(VeloTab.Explore, VeloTab.fromRoute("explore"))
        assertEquals(VeloTab.Settings, VeloTab.fromRoute("settings"))
    }

    @Test
    fun `el reproductor y los detalles dejan la navegacion sin seleccion`() {
        assertNull(VeloTab.fromRoute(""))
        assertNull(VeloTab.fromRoute(null))
        assertNull(VeloTab.fromRoute("albumdetail"))
        assertNull(VeloTab.fromRoute("playlistdetail"))
    }

    @Test
    fun `ocultar una pestaña conserva el orden de las demas`() {
        assertEquals(listOf(VeloTab.Home, VeloTab.Settings), visibleVeloTabs(showExplore = false))
        assertEquals(listOf(VeloTab.Explore), visibleVeloTabs(showHome = false, showSettings = false))
    }
}
