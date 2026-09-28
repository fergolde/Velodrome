package com.fergolde.velodrome.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The account scope partitions every on-disk cache so a second account signing
 * in on the same device cannot read the first account's data.
 */
class AccountScopeTest {

    private fun managerWith(username: String?, serverUrl: String?): CredentialsManager {
        val prefs: android.content.SharedPreferences = io.mockk.mockk(relaxed = true)
        val editor: android.content.SharedPreferences.Editor = io.mockk.mockk(relaxed = true)
        val store = mutableMapOf<String, String>()
        io.mockk.every { prefs.edit() } returns editor
        io.mockk.every { editor.putString(any(), any()) } answers {
            store[firstArg()] = secondArg(); editor
        }
        io.mockk.every { prefs.getString(any(), any()) } answers { store[firstArg()] ?: secondArg() }
        val manager = CredentialsManager(prefs)
        if (username != null && serverUrl != null) manager.saveCredentials(username, "pw", serverUrl)
        return manager
    }

    @Test
    fun `same account always resolves to the same scope`() {
        val a = managerWith("fernando", "https://music.example.com/").getAccountScope()
        val b = managerWith("fernando", "https://music.example.com/").getAccountScope()
        assertEquals(a, b)
    }

    @Test
    fun `different users on the same server get different scopes`() {
        val a = managerWith("fernando", "https://music.example.com/").getAccountScope()
        val b = managerWith("guest", "https://music.example.com/").getAccountScope()
        assertNotEquals(a, b)
    }

    @Test
    fun `same user on different servers gets different scopes`() {
        val a = managerWith("fernando", "https://one.example.com/").getAccountScope()
        val b = managerWith("fernando", "https://two.example.com/").getAccountScope()
        assertNotEquals(a, b)
    }

    @Test
    fun `scope does not leak the username or the server url`() {
        val scope = managerWith("fernando", "https://music.example.com/").getAccountScope()
        assertTrue(scope.none { it.isLetter() && it.isUpperCase() })
        assertTrue("scope must not contain the username", !scope.contains("fernando"))
        assertTrue("scope must not contain the host", !scope.contains("example"))
    }

    @Test
    fun `unconfigured account falls back to a stable sentinel`() {
        val manager = managerWith(null, null)
        assertEquals(NO_ACCOUNT_SCOPE, manager.getAccountScope())
    }

    @Test
    fun `audio cache keys are partitioned per account`() {
        val mine = NavidromeCacheKeyFactory.trackCacheKey("acct-1", "track-1")
        val theirs = NavidromeCacheKeyFactory.trackCacheKey("acct-2", "track-1")
        assertNotEquals(
            "a second account must not resolve the first account's cached audio",
            mine, theirs
        )
        assertEquals(NavidromeCacheKeyFactory.trackCacheKey("acct-1", "track-1"), mine)
    }
}
