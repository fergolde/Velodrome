package com.fergolde.velodrome.util

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CredentialsManagerTest {

    private lateinit var prefs: SharedPreferences
    private val store = mutableMapOf<String, String>()
    private var getStringCalls = 0

    @Before
    fun setup() {
        store.clear()
        getStringCalls = 0
        prefs = mockk {
            every { getString(any(), any()) } answers {
                getStringCalls++
                store[firstArg()] ?: secondArg()
            }
            every { edit() } returns mockk {
                every { putString(any(), any()) } answers {
                    store[firstArg()] = secondArg()
                    self as SharedPreferences.Editor
                }
                every { clear() } answers {
                    store.clear()
                    self as SharedPreferences.Editor
                }
                every { apply() } returns Unit
            }
        }
        store["username"] = "fernando"
        store["password"] = "secret"
        store["server_url"] = "https://music.example.com"
    }

    @Test
    fun `repeated calls within TTL read encrypted prefs only once per key`() {
        val manager = CredentialsManager(prefs)

        val first = manager.getValidAuthParams()!!
        repeat(200) { manager.getValidAuthParams() }

        assertEquals(3, getStringCalls) // username + password + serverUrl, una sola vez cada uno
        assertEquals("fernando", first.first)
        // Token estable dentro de la sesión
        val last = manager.getValidAuthParams()!!
        assertEquals(first.second, last.second)
        assertEquals(first.third, last.third)
    }

    @Test
    fun `saveCredentials invalidates cache and regenerates token`() {
        val manager = CredentialsManager(prefs)
        val before = manager.getValidAuthParams()!!
        val readsAfterFirst = getStringCalls

        manager.saveCredentials("other", "newpass", "https://new.example.com")

        val after = manager.getValidAuthParams()!!
        assertEquals("other", after.first)
        assertNotEquals(before.second, after.second)
        // Re-leyó los tres campos tras la invalidación
        assertEquals(readsAfterFirst + 3, getStringCalls)
    }

    @Test
    fun `clearCredentials leaves getters empty and auth params null`() {
        val manager = CredentialsManager(prefs)
        manager.getValidAuthParams()

        manager.clearCredentials()

        assertNull(manager.getUsername())
        assertNull(manager.getPassword())
        assertNull(manager.getServerUrl())
        assertNull(manager.getValidAuthParams())
    }
}
