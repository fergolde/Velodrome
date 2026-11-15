package com.fergolde.velodrome.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * La barra inferior selecciona contra pestañas tipadas en lugar de literales de
 * ruta repartidos por la UI. Este test fija el mapeo ruta -> pestaña y el caso
 * "ninguna pestaña seleccionada" (reproductor a pantalla completa).
 */
class VeloTabTest {

    @Test
    fun `cada ruta soportada mapea a su pestaña`() {
        assertEquals(VeloTab.Home, VeloTab.fromRoute("home"))
        assertEquals(VeloTab.Explore, VeloTab.fromRoute("explore"))
        assertEquals(VeloTab.Settings, VeloTab.fromRoute("settings"))
    }

    @Test
    fun `una ruta desconocida o vacia no selecciona ninguna pestaña`() {
        assertNull(VeloTab.fromRoute("player"))
        assertNull(VeloTab.fromRoute(""))
        assertNull(VeloTab.fromRoute(null))
    }

    @Test
    fun `las rutas declaradas son estables`() {
        assertEquals(
            listOf("home", "explore", "settings"),
            VeloTab.entries.map { it.route }
        )
    }
}
