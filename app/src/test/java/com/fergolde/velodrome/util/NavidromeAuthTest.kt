package com.fergolde.velodrome.util

import org.junit.Assert.*
import org.junit.Test

class NavidromeAuthTest {

    @Test
    fun generateSalt_hasCorrectLength() {
        val salt = NavidromeAuth.generateSalt()
        assertEquals(8, salt.length)
    }

    @Test
    fun generateSalt_containsOnlyValidChars() {
        val salt = NavidromeAuth.generateSalt()
        val valid = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        assertTrue(salt.all { it in valid })
    }

    @Test
    fun generateSalt_producesDifferentValues() {
        val salts = (1..100).map { NavidromeAuth.generateSalt() }
        val distinct = salts.distinct()
        assertTrue("Expected at least 90 unique salts out of 100", distinct.size > 90)
    }

    @Test
    fun calculateToken_returnsMd5OfPasswordPlusSalt() {
        val token = NavidromeAuth.calculateToken("pass123", "abc12345")
        val expected = md5("pass123abc12345")
        assertEquals(expected, token)
    }

    @Test
    fun calculateToken_emptyPassword() {
        val token = NavidromeAuth.calculateToken("", "salt1234")
        val expected = md5("salt1234")
        assertEquals(expected, token)
    }

    @Test
    fun calculateToken_emptySalt() {
        val token = NavidromeAuth.calculateToken("password", "")
        val expected = md5("password")
        assertEquals(expected, token)
    }

    @Test
    fun calculateToken_differentSaltDifferentToken() {
        val token1 = NavidromeAuth.calculateToken("pass", "salt1abc")
        val token2 = NavidromeAuth.calculateToken("pass", "salt2xyz")
        assertNotEquals(token1, token2)
    }

    @Test
    fun calculateToken_differentPasswordDifferentToken() {
        val token1 = NavidromeAuth.calculateToken("pass1", "salt1234")
        val token2 = NavidromeAuth.calculateToken("pass2", "salt1234")
        assertNotEquals(token1, token2)
    }

    @Test
    fun calculateToken_hexOnly() {
        val token = NavidromeAuth.calculateToken("test", "salt1234")
        assertTrue(token.matches(Regex("^[0-9a-f]+$")))
        assertEquals(32, token.length)
    }

    private fun md5(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
