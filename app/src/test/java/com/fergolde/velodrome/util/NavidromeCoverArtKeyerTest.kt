package com.fergolde.velodrome.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NavidromeCoverArtKeyerTest {

    private val scope = "acct-1"
    private val otherScope = "acct-2"

    private fun coverUrl(token: String, salt: String) =
        "https://music.example.com/rest/getCoverArt.view" +
            "?id=al-123&size=400&u=fernando&t=$token&s=$salt&v=1.16.1&c=Velodrome"

    @Test
    fun `strips rotating auth params`() {
        val normalized = NavidromeCoverArtKeyer.normalize(coverUrl("abc123", "salt1"), scope)
        assertEquals(
            "acct-1|https://music.example.com/rest/getCoverArt.view" +
                "?id=al-123&size=400&v=1.16.1&c=Velodrome",
            normalized
        )
    }

    @Test
    fun `different tokens produce same key`() {
        val a = NavidromeCoverArtKeyer.normalize(coverUrl("token-a", "salt-a"), scope)
        val b = NavidromeCoverArtKeyer.normalize(coverUrl("token-b", "salt-b"), scope)
        assertEquals(a, b)
    }

    @Test
    fun `same id different size produces different key`() {
        val small = NavidromeCoverArtKeyer.normalize(
            coverUrl("t", "s").replace("size=400", "size=100"), scope
        )
        val big = NavidromeCoverArtKeyer.normalize(coverUrl("t", "s"), scope)
        assertNotEquals(small, big)
    }

    @Test
    fun `already normalized url is still scoped`() {
        val clean = "https://music.example.com/rest/getCoverArt.view?id=al-123&size=400&v=1.16.1&c=Velodrome"
        assertEquals("acct-1|$clean", NavidromeCoverArtKeyer.normalize(clean, scope))
    }

    @Test
    fun `malformed url is still scoped`() {
        val broken = "not a url"
        assertEquals("acct-1|not a url", NavidromeCoverArtKeyer.normalize(broken, scope))
    }

    @Test
    fun `different accounts never share a cache key`() {
        // Two accounts on the same server must not be served each other's
        // artwork from cache.
        val mine = NavidromeCoverArtKeyer.normalize(coverUrl("t", "s"), scope)
        val theirs = NavidromeCoverArtKeyer.normalize(coverUrl("t", "s"), otherScope)
        assertNotEquals(mine, theirs)
    }
}
