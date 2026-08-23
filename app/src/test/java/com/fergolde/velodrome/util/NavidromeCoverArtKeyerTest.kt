package com.fergolde.velodrome.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NavidromeCoverArtKeyerTest {

    private fun coverUrl(token: String, salt: String) =
        "https://music.example.com/rest/getCoverArt.view" +
            "?id=al-123&size=400&u=fernando&t=$token&s=$salt&v=1.16.1&c=Velodrome"

    @Test
    fun `strips rotating auth params`() {
        val normalized = NavidromeCoverArtKeyer.normalize(coverUrl("abc123", "salt1"))
        assertEquals(
            "https://music.example.com/rest/getCoverArt.view?id=al-123&size=400&v=1.16.1&c=Velodrome",
            normalized
        )
    }

    @Test
    fun `different tokens produce same key`() {
        val a = NavidromeCoverArtKeyer.normalize(coverUrl("token-a", "salt-a"))
        val b = NavidromeCoverArtKeyer.normalize(coverUrl("token-b", "salt-b"))
        assertEquals(a, b)
    }

    @Test
    fun `same id different size produces different key`() {
        val small = NavidromeCoverArtKeyer.normalize(coverUrl("t", "s").replace("size=400", "size=100"))
        val big = NavidromeCoverArtKeyer.normalize(coverUrl("t", "s"))
        assertNotEquals(small, big)
    }

    @Test
    fun `already normalized url is unchanged`() {
        val clean = "https://music.example.com/rest/getCoverArt.view?id=al-123&size=400&v=1.16.1&c=Velodrome"
        assertEquals(clean, NavidromeCoverArtKeyer.normalize(clean))
    }

    @Test
    fun `malformed url returned as-is`() {
        val broken = "not a url"
        assertEquals(broken, NavidromeCoverArtKeyer.normalize(broken))
    }
}
