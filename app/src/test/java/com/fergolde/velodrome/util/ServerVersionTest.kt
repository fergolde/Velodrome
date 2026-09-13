package com.fergolde.velodrome.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerVersionTest {

    @Test
    fun `parse handles semver, prefixes and suffixes`() {
        assertEquals(Triple(0, 64, 0), ServerVersion.parse("0.64.0"))
        assertEquals(Triple(0, 64, 0), ServerVersion.parse("v0.64.0"))
        assertEquals(Triple(0, 64, 0), ServerVersion.parse("0.64.0-SNAPSHOT"))
        assertEquals(Triple(0, 64, 0), ServerVersion.parse(" 0.64.0+build7 "))
        assertEquals(Triple(0, 64, 0), ServerVersion.parse("0.64"))
        assertEquals(Triple(1, 0, 0), ServerVersion.parse("1"))
        assertEquals(Triple(1, 2, 3), ServerVersion.parse("1.2.3.4"))
    }

    @Test
    fun `parse rejects absent or non numeric values`() {
        assertNull(ServerVersion.parse(null))
        assertNull(ServerVersion.parse(""))
        assertNull(ServerVersion.parse("   "))
        assertNull(ServerVersion.parse("dev"))
        assertNull(ServerVersion.parse("vSNAPSHOT"))
    }

    @Test
    fun `no reset when server predates canonical ids`() {
        assertFalse(
            ServerVersion.requiresIdMigrationReset(
                storedVersion = "0.60.0",
                currentVersion = "0.63.2",
                hasLocalData = true
            )
        )
    }

    @Test
    fun `reset when stored version crosses canonical ids boundary`() {
        assertTrue(
            ServerVersion.requiresIdMigrationReset(
                storedVersion = "0.63.2",
                currentVersion = "0.64.0",
                hasLocalData = true
            )
        )
        assertTrue(
            ServerVersion.requiresIdMigrationReset(
                storedVersion = "0.59.0",
                currentVersion = "0.65.0-SNAPSHOT",
                hasLocalData = true
            )
        )
    }

    @Test
    fun `no reset when stored version already canonical`() {
        assertFalse(
            ServerVersion.requiresIdMigrationReset(
                storedVersion = "0.64.0",
                currentVersion = "0.64.0",
                hasLocalData = true
            )
        )
        assertFalse(
            ServerVersion.requiresIdMigrationReset(
                storedVersion = "0.64.0",
                currentVersion = "0.64.1",
                hasLocalData = true
            )
        )
    }

    @Test
    fun `unknown stored version resets only when local data exists`() {
        assertTrue(
            ServerVersion.requiresIdMigrationReset(
                storedVersion = null,
                currentVersion = "0.64.0",
                hasLocalData = true
            )
        )
        assertTrue(
            ServerVersion.requiresIdMigrationReset(
                storedVersion = "dev",
                currentVersion = "0.64.0",
                hasLocalData = true
            )
        )
        assertFalse(
            ServerVersion.requiresIdMigrationReset(
                storedVersion = null,
                currentVersion = "0.64.0",
                hasLocalData = false
            )
        )
    }

    @Test
    fun `no reset when current version is unknown`() {
        assertFalse(
            ServerVersion.requiresIdMigrationReset(
                storedVersion = "0.63.2",
                currentVersion = null,
                hasLocalData = true
            )
        )
        assertFalse(
            ServerVersion.requiresIdMigrationReset(
                storedVersion = "0.63.2",
                currentVersion = "dev",
                hasLocalData = true
            )
        )
    }
}
