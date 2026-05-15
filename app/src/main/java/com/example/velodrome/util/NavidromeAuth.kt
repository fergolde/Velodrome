package com.example.velodrome.util

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.random.Random

object NavidromeAuth {

    private const val SALT_LENGTH = 8

    /**
     * Generate a random salt string
     */
    fun generateSalt(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..SALT_LENGTH)
            .map { chars[Random.nextInt(chars.length)] }
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
     * Calculate MD5 hash of a string
     */
    private fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray(charset("UTF-8")))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: NoSuchAlgorithmException) {
            ""
        }
    }
}