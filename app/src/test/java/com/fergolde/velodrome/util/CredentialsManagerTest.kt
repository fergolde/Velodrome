package com.fergolde.velodrome.util

import android.content.SharedPreferences
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CredentialsManagerTest {

    private val prefs: SharedPreferences = mockk(relaxed = true)
    private val prefsEditor: SharedPreferences.Editor = mockk(relaxed = true)
    private lateinit var manager: CredentialsManager

    @Before
    fun setup() {
        every { prefs.edit() } returns prefsEditor
        every { prefsEditor.apply() } just runs
        manager = CredentialsManager(prefs)
    }

    @Test
    fun saveCredentials_callsEditAndApply() {
        manager.saveCredentials("user", "pass", "https://server.com/")

        verify { prefs.edit() }
        verify { prefsEditor.apply() }
    }

    @Test
    fun saveCredentials_invalidatesCache() {
        // Stub so getValidAuthParams can work after save
        every { prefs.getString("username", null) } returns "user"
        every { prefs.getString("password", null) } returns "pass"
        every { prefs.getString("server_url", null) } returns "https://server.com/"

        manager.saveCredentials("user", "pass", "https://server.com/")

        // After saveCredentials, cache is invalidated.
        // getValidAuthParams should compute new token (not cached).
        val result = manager.getValidAuthParams()
        assertNotNull(result)
        assertEquals("user", result!!.first)
    }

    @Test
    fun clearCredentials_clearsAll() {
        manager.clearCredentials()

        verify { prefsEditor.clear() }
    }

    @Test
    fun getUsername_returnsFromPrefs() {
        every { prefs.getString("username", null) } returns "user"

        assertEquals("user", manager.getUsername())
    }

    @Test
    fun getUsername_returnsNullWhenMissing() {
        every { prefs.getString("username", null) } returns null

        assertNull(manager.getUsername())
    }

    @Test
    fun getPassword_returnsFromPrefs() {
        every { prefs.getString("password", null) } returns "pass"

        assertEquals("pass", manager.getPassword())
    }

    @Test
    fun getServerUrl_returnsFromPrefs() {
        every { prefs.getString("server_url", null) } returns "https://server.com/"

        assertEquals("https://server.com/", manager.getServerUrl())
    }

    @Test
    fun hasCredentials_allPresent_returnsTrue() {
        every { prefs.getString("username", null) } returns "user"
        every { prefs.getString("password", null) } returns "pass"
        every { prefs.getString("server_url", null) } returns "https://server.com/"

        assertTrue(manager.hasCredentials())
    }

    @Test
    fun hasCredentials_missingUsername_returnsFalse() {
        every { prefs.getString("username", null) } returns null
        every { prefs.getString("password", null) } returns "pass"
        every { prefs.getString("server_url", null) } returns "https://server.com/"

        assertFalse(manager.hasCredentials())
    }

    @Test
    fun hasCredentials_blankPassword_returnsFalse() {
        every { prefs.getString("username", null) } returns "user"
        every { prefs.getString("password", null) } returns ""
        every { prefs.getString("server_url", null) } returns "https://server.com/"

        assertFalse(manager.hasCredentials())
    }

    @Test
    fun hasCredentials_missingServerUrl_returnsFalse() {
        every { prefs.getString("username", null) } returns "user"
        every { prefs.getString("password", null) } returns "pass"
        every { prefs.getString("server_url", null) } returns null

        assertFalse(manager.hasCredentials())
    }

    @Test
    fun invalidateAuth_clearsCache() {
        manager.invalidateAuth()

        // After invalidation, getValidAuthParams should compute new token (not cached)
        val result = manager.getValidAuthParams()
        assertNotNull(result)
    }

    @Test
    fun getValidAuthParams_withCredentials_returnsTriple() {
        every { prefs.getString("username", null) } returns "user"
        every { prefs.getString("password", null) } returns "pass"
        every { prefs.getString("server_url", null) } returns "https://server.com/"

        val result = manager.getValidAuthParams()

        assertNotNull(result)
        assertEquals("user", result!!.first)
        assertNotNull(result.second) // token
        assertNotNull(result.third) // salt
    }

    @Test
    fun getValidAuthParams_withoutCredentials_returnsNull() {
        every { prefs.getString(any(), any()) } returns null

        val result = manager.getValidAuthParams()
        assertNull(result)
    }

    @Test
    fun getValidAuthParams_cachesToken() {
        every { prefs.getString("username", null) } returns "user"
        every { prefs.getString("password", null) } returns "pass"
        every { prefs.getString("server_url", null) } returns "https://server.com/"

        val first = manager.getValidAuthParams()
        val second = manager.getValidAuthParams()

        // Same token and salt should be returned from cache
        assertEquals(first!!.second, second!!.second)
        assertEquals(first.third, second.third)
    }

    @Test
    fun getCoverArtUrl_returnsFormattedUrl() {
        every { prefs.getString("username", null) } returns "user"
        every { prefs.getString("password", null) } returns "pass"
        every { prefs.getString("server_url", null) } returns "https://server.com/"

        val url = manager.getCoverArtUrl("cov-1", 300)

        assertNotNull(url)
        assertTrue(url!!.contains("id=cov-1"))
        assertTrue(url.contains("size=300"))
        assertTrue(url.contains("u=user"))
        assertTrue(url.contains("t="))
        assertTrue(url.contains("s="))
    }

    @Test
    fun getCoverArtUrl_blankCoverArtId_returnsNull() {
        assertNull(manager.getCoverArtUrl("", 300))
        assertNull(manager.getCoverArtUrl(null, 300))
    }

    @Test
    fun getCoverArtUrl_nullServerUrl_returnsNull() {
        every { prefs.getString(any(), any()) } returns null

        assertNull(manager.getCoverArtUrl("cov-1", 300))
    }

    @Test
    fun getStreamUrl_returnsFormattedUrl() {
        every { prefs.getString("username", null) } returns "user"
        every { prefs.getString("password", null) } returns "pass"
        every { prefs.getString("server_url", null) } returns "https://server.com/"

        val url = manager.getStreamUrl("track-1")

        assertTrue(url.contains("id=track-1"))
        assertTrue(url.contains("u=user"))
    }

    @Test
    fun getStreamUrl_nullCredentials_returnsEmpty() {
        every { prefs.getString(any(), any()) } returns null

        assertEquals("", manager.getStreamUrl("track-1"))
    }
}
