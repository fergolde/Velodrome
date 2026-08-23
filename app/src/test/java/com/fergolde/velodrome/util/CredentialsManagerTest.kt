package com.fergolde.velodrome.util

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CredentialsManagerTest {

    private val prefs: SharedPreferences = mockk(relaxed = true)
    private val prefsEditor: SharedPreferences.Editor = mockk(relaxed = true)
    private val store = mutableMapOf<String, String>()
    private var getStringCalls = 0

    @Before
    fun setup() {
        store.clear()
        getStringCalls = 0
        every { prefs.edit() } returns prefsEditor
        every { prefsEditor.apply() } just runs
        every { prefsEditor.putString(any(), any()) } answers {
            store[firstArg()] = secondArg()
            prefsEditor
        }
        every { prefsEditor.clear() } answers {
            store.clear()
            prefsEditor
        }
        every { prefs.getString(any(), any()) } answers {
            getStringCalls++
            store[firstArg()] ?: secondArg()
        }
    }

    // El manager se instancia por test: la memoización vive por instancia,
    // así cada test arranca con la caché fría.
    private fun newManager() = CredentialsManager(prefs)

    private fun seedFull() {
        store["username"] = "user"
        store["password"] = "pass"
        store["server_url"] = "https://server.com/"
    }

    // ========= PERSISTENCE =========

    @Test
    fun saveCredentials_callsEditAndApply() {
        val manager = newManager()

        manager.saveCredentials("user", "pass", "https://server.com/")

        verify { prefs.edit() }
        verify { prefsEditor.apply() }
    }

    @Test
    fun saveCredentials_invalidatesCacheAndRegeneratesToken() {
        val manager = newManager()
        seedFull()
        val before = manager.getValidAuthParams()!!
        val readsAfterFirstLoad = getStringCalls

        manager.saveCredentials("other", "newpass", "https://new.example.com")

        val after = manager.getValidAuthParams()!!
        assertEquals("other", after.first)
        assertNotEquals(before.second, after.second)
        // Re-leyó los tres campos tras la invalidación
        assertEquals(readsAfterFirstLoad + 3, getStringCalls)
    }

    @Test
    fun clearCredentials_clearsAll() {
        val manager = newManager()

        manager.clearCredentials()

        verify { prefsEditor.clear() }
    }

    // ========= GETTERS (memoizados: leen una vez por invalidación) =========

    @Test
    fun getUsername_returnsFromPrefs() {
        store["username"] = "user"

        assertEquals("user", newManager().getUsername())
    }

    @Test
    fun getUsername_returnsNullWhenMissing() {
        assertNull(newManager().getUsername())
    }

    @Test
    fun getPassword_returnsFromPrefs() {
        store["password"] = "pass"

        assertEquals("pass", newManager().getPassword())
    }

    @Test
    fun getServerUrl_returnsFromPrefs() {
        store["server_url"] = "https://server.com/"

        assertEquals("https://server.com/", newManager().getServerUrl())
    }

    @Test
    fun repeatedCallsWithinTtl_readEncryptedPrefsOnlyOncePerKey() {
        seedFull()
        val manager = newManager()

        val first = manager.getValidAuthParams()!!
        repeat(200) { manager.getValidAuthParams() }

        assertEquals(3, getStringCalls) // username + password + server_url, una vez cada uno
        val last = manager.getValidAuthParams()!!
        assertEquals(first.second, last.second) // token estable dentro de la sesión
        assertEquals(first.third, last.third)
    }

    // ========= hasCredentials =========

    @Test
    fun hasCredentials_allPresent_returnsTrue() {
        seedFull()

        assertTrue(newManager().hasCredentials())
    }

    @Test
    fun hasCredentials_missingUsername_returnsFalse() {
        store["password"] = "pass"
        store["server_url"] = "https://server.com/"

        assertFalse(newManager().hasCredentials())
    }

    @Test
    fun hasCredentials_blankPassword_returnsFalse() {
        store["username"] = "user"
        store["password"] = ""
        store["server_url"] = "https://server.com/"

        assertFalse(newManager().hasCredentials())
    }

    @Test
    fun hasCredentials_missingServerUrl_returnsFalse() {
        store["username"] = "user"
        store["password"] = "pass"

        assertFalse(newManager().hasCredentials())
    }

    // ========= SESSION MANAGEMENT =========

    @Test
    fun clearCredentials_leavesGettersEmptyAndAuthParamsNull() {
        seedFull()
        val manager = newManager()
        manager.getValidAuthParams()

        manager.clearCredentials()

        assertNull(manager.getUsername())
        assertNull(manager.getPassword())
        assertNull(manager.getServerUrl())
        assertNull(manager.getValidAuthParams())
    }

    @Test
    fun getValidAuthParams_withCredentials_returnsTriple() {
        seedFull()

        val result = newManager().getValidAuthParams()

        assertNotNull(result)
        assertEquals("user", result!!.first)
        assertTrue(result.second.isNotEmpty()) // token md5
        assertTrue(result.third.isNotEmpty()) // salt
    }

    @Test
    fun getValidAuthParams_withoutCredentials_returnsNull() {
        assertNull(newManager().getValidAuthParams())
    }

    @Test
    fun getValidAuthParams_cachesToken() {
        seedFull()
        val manager = newManager()

        val first = manager.getValidAuthParams()
        val second = manager.getValidAuthParams()

        assertEquals(first!!.second, second!!.second)
        assertEquals(first.third, second.third)
    }

    // ========= URL HELPERS =========

    @Test
    fun getCoverArtUrl_returnsFormattedUrl() {
        seedFull()

        val url = newManager().getCoverArtUrl("cov-1", 300)

        assertNotNull(url)
        assertTrue(url!!.contains("id=cov-1"))
        assertTrue(url.contains("size=300"))
        assertTrue(url.contains("u=user"))
        assertTrue(url.contains("t="))
        assertTrue(url.contains("s="))
    }

    @Test
    fun getCoverArtUrl_blankCoverArtId_returnsNull() {
        val manager = newManager()

        assertNull(manager.getCoverArtUrl("", 300))
        assertNull(manager.getCoverArtUrl(null, 300))
    }

    @Test
    fun getCoverArtUrl_nullServerUrl_returnsNull() {
        assertNull(newManager().getCoverArtUrl("cov-1", 300))
    }

    @Test
    fun getStreamUrl_returnsFormattedUrl() {
        seedFull()

        val url = newManager().getStreamUrl("track-1")

        assertTrue(url.contains("id=track-1"))
        assertTrue(url.contains("u=user"))
        assertTrue(url.contains("maxBitRate=999"))
    }

    @Test
    fun getStreamUrl_nullCredentials_returnsEmpty() {
        assertEquals("", newManager().getStreamUrl("track-1"))
    }
}