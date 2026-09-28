package com.fergolde.velodrome.util

import java.security.MessageDigest
import java.security.SecureRandom

object NavidromeAuth {

    private const val SALT_LENGTH = 8
    private const val SALT_ALPHABET =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    // The salt is half of the credential material in the token scheme, so it
    // must come from a CSPRNG. kotlin.random.Random is a non-cryptographic
    // PRNG whose internal state is predictable from observed output.
    private val secureRandom = SecureRandom()

    /**
     * Generate a random salt string
     */
    fun generateSalt(): String {
        return (1..SALT_LENGTH)
            .map { SALT_ALPHABET[secureRandom.nextInt(SALT_ALPHABET.length)] }
            .joinToString("")
    }

    /**
     * Calculate token = md5(password + salt)
     */
    fun calculateToken(password: String, salt: String): String {
        val input = password + salt
        return md5(input)
    }

    /**
     * Calculate MD5 hash of a string.
     *
     * MD5 is mandated by the Subsonic token scheme and is guaranteed present on
     * every JVM. A missing implementation is a broken platform, not a runtime
     * condition to swallow: returning "" would silently send an empty token and
     * surface as an unexplained 401 instead of a real error.
     */
    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
            .digest(input.toByteArray(charset("UTF-8")))
        return digest.joinToString("") { "%02x".format(it) }
    }
}