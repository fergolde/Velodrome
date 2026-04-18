package com.example.velodrome.util

import android.util.Log
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
        } catch (e: NoSuchAlgorithmException) {
            Log.e("NavidromeAuth", "MD5 not available", e)
            ""
        }
    }
}

object XmlParser {
    
    /**
     * Simple XML to Map parser
     */
    fun parse(xmlString: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        try {
            // Extract subsonic-response attributes
            if (!xmlString.contains("<subsonic-response")) {
                Log.d("XmlParser", "No subsonic-response found in: $xmlString")
                return result
            }
            
            // Parse status
            val statusRegex = """status="([^"]+)"""".toRegex()
            val statusMatch = statusRegex.find(xmlString)
            if (statusMatch != null) {
                result["status"] = statusMatch.groupValues[1]
            }
            
            // Parse error if present
            if (xmlString.contains("<error")) {
                val errorCodeRegex = """code="(\d+)"""".toRegex()
                val errorMsgRegex = """message="([^"]+)"""".toRegex()
                
                val errorCode = errorCodeRegex.find(xmlString)?.groupValues?.get(1)
                val errorMsg = errorMsgRegex.find(xmlString)?.groupValues?.get(1)
                
                if (errorCode != null) {
                    result["error"] = mapOf(
                        "code" to errorCode.toInt(),
                        "message" to (errorMsg ?: "Unknown error")
                    )
                }
            }
            
            // Parse albumList2
            if (xmlString.contains("<albumList2")) {
                val albumList = parseAlbums(xmlString)
                result["subsonic-response"] = mapOf("albumList2" to albumList)
            }
            
            Log.d("XmlParser", "Parsed albums: ${result["subsonic-response"]}")
            
        } catch (e: Exception) {
            Log.e("XmlParser", "Error parsing XML", e)
        }
        
        return result
    }
    
/**
     * Parse album elements from XML
     * 
     * Navidrome API returns albums with attributes in the opening tag:
     * <album id="123" name="Album Title" artist="Artist" coverArt="al-xxx" year="2024" ...>
     *   <genres><genre>Rock</genre></genres>
     * </album>
     * 
     * We only need the attributes from the opening tag - all relevant data is there!
     */
    private fun parseAlbums(xmlString: String): Map<String, Any> {
        val albumList = mutableListOf<Map<String, Any>>()
        
        // Match album OPENING tags only - attributes contain all data we need
        // The ">" ends the opening tag, any nested content is ignored
        val albumRegex = """<album\s+([^>]*)>""".toRegex()
        val matches = albumRegex.findAll(xmlString)
        val matchCount = matches.count()
        Log.d("XmlParser", "Found $matchCount album opening tags in XML")
        
        for (match in matches) {
            val attrs = match.groupValues[1]
            
            // Create mutable map with explicit type
            val album = mutableMapOf<String, Any>()
            
            // Extract id - use helper function to avoid type issues
            putAttr(album, attrs, "id", """\bid="([^"]+)""")
            putAttr(album, attrs, "title", """\bname="([^"]+)""")
            putAttr(album, attrs, "artist", """\bartist="([^"]+)""")
            putAttr(album, attrs, "artistId", """\bartistId="([^"]+)""")
            putAttr(album, attrs, "coverArt", """\bcoverArt="([^"]+)""")
            // Extract year
            val yearVal = Regex("""\byear="(\d+)"""").find(attrs)?.groupValues?.get(1)
            if (yearVal != null) {
                album["year"] = yearVal.toIntOrNull() as Any
            }
            // Extract genre
            val genreVal = Regex("""\bgenre="([^"]+)"""").find(attrs)?.groupValues?.get(1)
            if (genreVal != null) {
                album["genre"] = genreVal as Any
            }
            
            if (album.isNotEmpty()) {
                albumList.add(album)
                Log.d("XmlParserAlbum", "Parsed album: ${album["title"]}, coverArt=${album["coverArt"]}")
            }
        }
        
        Log.d("XmlParser", "Total albums found: ${albumList.size}")
        
        return mapOf("album" to albumList)
    }
    
    /**
     * Helper to put attribute into map
     */
    private fun putAttr(map: MutableMap<String, Any>, attrs: String, key: String, regex: String) {
        val match = Regex(regex).find(attrs)
        if (match != null) {
            map[key] = match.groupValues[1]
        }
    }
}