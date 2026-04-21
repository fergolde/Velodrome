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
            
            // Parse artists (getArtists.view)
            // Note: <artists> tag may have attributes like <artists lastModified="...">
            if (xmlString.contains("<artists")) {
                Log.d("XmlParser", "Found <artists> tag in XML, parsing...")
                val artistsList = parseArtists(xmlString)
                result["subsonic-response"] = (result["subsonic-response"] as? Map<String, Any> ?: emptyMap()) + mapOf("artists" to artistsList)
            } else {
                Log.d("XmlParser", "No <artists> tag found in XML. XML sample: ${xmlString.take(500)}")
            }

            // Parse single album (getAlbum.view) - contains <song> children
            // Note: <album> tag may have attributes, and contains <song> elements
            if (xmlString.contains("<album ") && !xmlString.contains("<albumList2")) {
                Log.d("XmlParser", "Found <album> tag in XML, parsing...")
                val albumData = parseAlbumWithSongs(xmlString)
                result["subsonic-response"] = (result["subsonic-response"] as? Map<String, Any> ?: emptyMap()) + mapOf("album" to albumData)
            }
            
            Log.d("XmlParser", "Parsed response with albums and/or artists")
            
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
     * Parse artist elements from XML
     * 
     * Navidrome API returns artists with attributes in the opening tag:
     * <artist id="123" name="Artist Name" albumCount="5" coverArt="ar-123"/>
     */
    private fun parseArtists(xmlString: String): Map<String, Any> {
        val artistList = mutableListOf<Map<String, Any>>()
        
        // Match artist OPENING tags - attributes contain all data we need
        val artistRegex = """<artist\s+([^>]*)>""".toRegex()
        val matches = artistRegex.findAll(xmlString)
        val matchCount = matches.count()
        Log.d("XmlParser", "Found $matchCount artist opening tags in XML")
        
        for (match in matches) {
            val attrs = match.groupValues[1]
            
            val artist = mutableMapOf<String, Any>()
            
            // Extract id
            putAttr(artist, attrs, "id", """\bid="([^"]+)""")
            // Extract name
            putAttr(artist, attrs, "name", """\bname="([^"]+)""")
            // Extract albumCount
            val albumCountVal = Regex("""\balbumCount="(\d+)"""").find(attrs)?.groupValues?.get(1)
            if (albumCountVal != null) {
                artist["albumCount"] = albumCountVal.toIntOrNull() ?: 0
            }
            // Extract coverArt
            putAttr(artist, attrs, "coverArt", """\bcoverArt="([^"]+)""")
            
            if (artist.isNotEmpty()) {
                artistList.add(artist)
                Log.d("XmlParserArtist", "Parsed artist: ${artist["name"]}, albumCount=${artist["albumCount"]}")
            }
        }
        
Log.d("XmlParser", "Total artists found: ${artistList.size}")

        return mapOf("artist" to artistList)
    }

    /**
     * Parse album element with nested song children from getAlbum response
     *
     * Navidrome API returns album with song children:
     * <album id="xxx" name="Album Title" artist="Artist" ...>
     *   <genres>...</genres>
     *   <artists>...</artists>
     *   <song id="xxx" title="Track Name" track="1" duration="180" ...>
     *     <genres>...</genres>
     *     <artists>...</artists>
     *   </song>
     *   <song id="yyy" ...>...</song>
     * </album>
     */
    private fun parseAlbumWithSongs(xmlString: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        // Match album OPENING tag to extract attributes
        val albumRegex = """<album\s+([^>]*)>""".toRegex()
        val albumMatch = albumRegex.find(xmlString)
        if (albumMatch != null) {
            val attrs = albumMatch.groupValues[1]
            Log.d("XmlParserAlbumWithSongs", "Found album attrs: $attrs")

            // Extract album attributes
            putAttr(result, attrs, "id", """\bid="([^"]+)""")
            putAttr(result, attrs, "name", """\bname="([^"]+)""")
            putAttr(result, attrs, "artist", """\bartist="([^"]+)""")
            putAttr(result, attrs, "artistId", """\bartistId="([^"]+)""")
            putAttr(result, attrs, "coverArt", """\bcoverArt="([^"]+)""")

            // Extract year
            val yearVal = Regex("""\byear="(\d+)"""").find(attrs)?.groupValues?.get(1)
            if (yearVal != null) {
                result["year"] = yearVal.toIntOrNull() ?: yearVal
            }
            // Extract genre
            val genreVal = Regex("""\bgenre="([^"]+)"""").find(attrs)?.groupValues?.get(1)
            if (genreVal != null) {
                result["genre"] = genreVal
            }
        }

        // Parse all song elements inside the album
        val songList = mutableListOf<Map<String, Any>>()
        val songRegex = """<song\s+([^>]*)>""".toRegex()
        val songMatches = songRegex.findAll(xmlString)
        val songMatchCount = songMatches.count()
        Log.d("XmlParserAlbumWithSongs", "Found $songMatchCount song tags in album XML")

        for (songMatch in songMatches) {
            val attrs = songMatch.groupValues[1]
            val song = mutableMapOf<String, Any>()

            // Extract song attributes - these contain all the data we need
            putAttr(song, attrs, "id", """\bid="([^"]+)""")
            putAttr(song, attrs, "title", """\btitle="([^"]+)""")
            putAttr(song, attrs, "track", """\btrack="([^"]+)""")
            putAttr(song, attrs, "duration", """\bduration="([^"]+)""")
            putAttr(song, attrs, "size", """\bsize="([^"]+)""")
            putAttr(song, attrs, "bitRate", """\bbitRate="([^"]+)""")
            putAttr(song, attrs, "albumId", """\balbumId="([^"]+)""")
            putAttr(song, attrs, "artistId", """\bartistId="([^"]+)""")
            putAttr(song, attrs, "artist", """\bartist="([^"]+)""")
            putAttr(song, attrs, "album", """\balbum="([^"]+)""")
            putAttr(song, attrs, "path", """\bpath="([^"]+)""")
            putAttr(song, attrs, "coverArt", """\bcoverArt="([^"]+)""")

            if (song.isNotEmpty()) {
                songList.add(song)
            }
        }

        Log.d("XmlParserAlbumWithSongs", "Parsed ${songList.size} songs from album")
        result["song"] = songList

        return result
    }

    /**
     * Helper to put attribute into map with HTML entity decoding
     */
    private fun putAttr(map: MutableMap<String, Any>, attrs: String, key: String, regex: String) {
        val match = Regex(regex).find(attrs)
        if (match != null) {
            map[key] = decodeHtmlEntities(match.groupValues[1])
        }
    }

    /**
     * Decode common HTML entities that may appear in XML attributes
     * Navidrome encodes & as &amp; in XML
     * Also handles character references like &#39; (decimal) and &#x27; (hex)
     */
    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&#27;", "'")
    }
}