package com.example.velodrome.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.ArtistWithAlbums
import com.example.velodrome.domain.model.AuthResult
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.repository.NavidromeRepository
import com.example.velodrome.util.CredentialsManager
import com.example.velodrome.util.XmlParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavidromeRepositoryImpl @Inject constructor(
    private val api: NavidromeApi,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : NavidromeRepository {

    private val cacheDir: File = context.cacheDir

    // Helper method to parse XML responses
    private fun parseXmlResponse(responseBody: okhttp3.ResponseBody): Map<String, Any> {
        val xmlString = responseBody.string()
        Log.d("Velodrome", "XML response: $xmlString")
        return XmlParser.parse(xmlString)
    }

    override suspend fun login(username: String, password: String, serverUrl: String): Result<AuthResult> {
        return runCatching {
            try {
                // Save credentials securely (username + password, NO token)
                CredentialsManager.saveCredentials(username, password, serverUrl)
                Log.d("Velodrome", "Credentials saved for user: $username")

                // Try ping - auth interceptor will add u, t, s params automatically
                val responseBody = api.ping()
                val xmlString = responseBody.string()
                Log.d("Velodrome", "Ping response: $xmlString")

                // Parse XML response
                val response = XmlParser.parse(xmlString)
                val status = response["status"] as? String

                if (status == "ok") {
                    Log.d("Velodrome", "Login successful!")
                    AuthResult(success = true, token = password)
                } else {
                    // Login failed - clear credentials
                    CredentialsManager.clearCredentials()
                    val errorMap = response["error"] as? Map<*, *>
                    val errorMsg = errorMap?.get("message")?.toString() ?: "Invalid credentials"
                    Log.d("Velodrome", "Error: $errorMsg")
                    AuthResult(success = false, error = errorMsg)
                }
            } catch (e: Exception) {
                Log.e("Velodrome", "Login exception", e)
                // Clear credentials on error
                CredentialsManager.clearCredentials()
                AuthResult(success = false, error = e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Check if user is already logged in (has stored credentials)
     */
    override fun isLoggedIn(): Boolean {
        return CredentialsManager.hasCredentials()
    }

    /**
     * Force logout - clear all stored credentials
     */
    override fun logout() {
        CredentialsManager.clearCredentials()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getArtists(offset: Int, size: Int): Result<List<Artist>> {
        return runCatching {
            Log.d("Repo", "=== getArtists called ===")
            // Auth params added automatically by AuthInterceptor
            val responseBody = api.getArtists(size, offset)
            val response = parseXmlResponse(responseBody)
            
            Log.d("Repo", "Response keys: ${response.keys}")
            Log.d("Repo", "Subsonic response: ${response["subsonic-response"]}")
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val artistsData = subsonicResponse?.get("artists") as? Map<String, Any>
            Log.d("Repo", "Artists data: $artistsData")
            val artistsList = artistsData?.get("artist") as? List<Any> ?: emptyList()
            Log.d("Repo", "Found ${artistsList.size} artists")

            artistsList.mapNotNull { artistMap ->
                (artistMap as? Map<String, Any>)?.let { am ->
                    val artist = Artist(
                        id = am["id"] as? String ?: "",
                        name = decodeHtmlEntities(am["name"] as? String ?: ""),
                        albumCount = (am["albumCount"] as? Number)?.toInt() ?: 0,
                        coverUrl = am["coverArt"] as? String
                    )
                    Log.d("Repo", "Created Artist: ${artist.name}, albumCount: ${artist.albumCount}")
                    artist
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getAlbum(albumId: String): Result<Album> {
        return runCatching {
            Log.d("Repo", "=== getAlbum called: $albumId ===")
            val responseBody = api.getAlbum(albumId)
            val xmlString = responseBody.string()
            Log.d("Repo", "getAlbum XML (first 800): ${xmlString.take(800)}")
            
            // Use regex to extract album info from XML
            // <album id="xxx" name="xxx" artist="xxx" artistId="xxx" coverArt="xxx" year="xxx" ...>
            val albumIdRegex = """<album\s+id="([^"]+)"\s+name="([^"]+)"\s+artist="([^"]+)"\s+artistId="([^"]+)"""".toRegex()
            val albumMatch = albumIdRegex.find(xmlString)
            
            val extractedAlbumId = albumMatch?.groupValues?.get(1) ?: albumId
            val title = decodeHtmlEntities(albumMatch?.groupValues?.get(2) ?: "")
            val artist = decodeHtmlEntities(albumMatch?.groupValues?.get(3) ?: "")
            val artistId = albumMatch?.groupValues?.get(4) ?: ""
            
            // Extract coverArt
            val coverArtRegex = """coverArt="([^"]+)"""".toRegex()
            val coverArtMatch = coverArtRegex.find(xmlString)
            val coverArt = coverArtMatch?.groupValues?.get(1)
            
            // Extract year
            val yearRegex = """year="(\d+)"""".toRegex()
            val yearMatch = yearRegex.find(xmlString)
            val year = yearMatch?.groupValues?.get(1)?.toIntOrNull()
            
            // Extract genre
            val genreRegex = """genre="([^"]+)"""".toRegex()
            val genreMatch = genreRegex.find(xmlString)
            val genre = genreMatch?.groupValues?.get(1)
            
            Log.d("Repo", "Parsed album: id=$extractedAlbumId, title=$title, artist=$artist, coverArt=$coverArt, year=$year")
            
            Album(
                id = extractedAlbumId,
                artistId = artistId,
                artistName = artist,
                title = title,
                year = year,
                genre = genre,
                coverUrl = coverArt
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getTracks(albumId: String): Result<List<Track>> {
        return runCatching {
            Log.d("Velodrome", "getTracks called for albumId: $albumId")
            val responseBody = api.getAlbum(albumId)
            val xmlString = responseBody.string()
            Log.d("Velodrome", "getTracks XML (first 500): ${xmlString.take(500)}")
            val response = XmlParser.parse(xmlString)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val albumData = subsonicResponse?.get("album") as? Map<String, Any>
            val songsList = albumData?.get("song") as? List<Any> ?: emptyList()

            songsList.mapNotNull { songMap ->
                (songMap as? Map<String, Any>)?.let { sm ->
                    val durationValue = sm["duration"]
                    val durationSec = when (durationValue) {
                        is Number -> durationValue.toInt()
                        is String -> durationValue.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val sizeValue = sm["size"]
                    val sizeBytes = when (sizeValue) {
                        is Number -> sizeValue.toLong()
                        is String -> sizeValue.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    val bitrateValue = sm["bitRate"]
                    val bitrate = when (bitrateValue) {
                        is Number -> bitrateValue.toInt()
                        is String -> bitrateValue.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val trackValue = sm["track"]
                    val trackNumber = when (trackValue) {
                        is Number -> trackValue.toInt()
                        is String -> trackValue.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val coverArtValue = sm["coverArt"] as? String
                    val albumNameValue = sm["album"] as? String ?: albumId
                    // Fallback: si no hay coverArt, usar albumId como coverArtId (formato Subsonic: "al-{id}")
                    val effectiveCoverArtId = coverArtValue ?: "al-$albumId"
                    Track(
                        id = sm["id"] as? String ?: "",
                        albumId = albumId,
                        title = sm["title"] as? String ?: "",
                        artistName = sm["artist"] as? String ?: "",
                        albumName = albumNameValue,
                        durationSec = durationSec,
                        sizeBytes = sizeBytes,
                        bitrate = bitrate,
                        trackNumber = trackNumber,
                        isCached = false,
                        coverArtId = effectiveCoverArtId
                    )
                }
            }.also { tracks ->
                Log.d("Velodrome", "Parsed ${tracks.size} tracks from album $albumId")
            }
        }
    }

    override suspend fun getStreamUrl(trackId: String): String {
        // For streaming, we need to build URL manually with auth
        val serverUrl = CredentialsManager.getServerUrl() ?: ""
        val authParams = CredentialsManager.generateAuthParams()
        
        return if (authParams != null) {
            val (username, token, salt) = authParams
            "${serverUrl}rest/stream.view?id=$trackId&u=$username&t=$token&s=$salt&v=1.16.1&c=Velodrome&maxBitRate=320"
        } else {
            // Fallback - shouldn't happen if logged in
            "${serverUrl}rest/stream.view?id=$trackId&maxBitRate=320"
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun search(query: String): Result<List<Artist>> {
        return runCatching {
            val responseBody = api.search(query)
            val response = parseXmlResponse(responseBody)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val searchResult = subsonicResponse?.get("searchResult") as? Map<String, Any>
            val artistsList = searchResult?.get("artist") as? List<Any> ?: emptyList()

            artistsList.mapNotNull { artistMap ->
                (artistMap as? Map<String, Any>)?.let { am ->
                    Artist(
                        id = am["id"] as? String ?: "",
                        name = am["name"] as? String ?: "",
                        albumCount = (am["albumCount"] as? Number)?.toInt() ?: 0,
                        coverUrl = am["coverArt"] as? String
                    )
                }
            }
        }
    }

    override suspend fun getCacheSize(): Long {
        return cacheDir.walkTopDown()
            .filter { it.isFile && it.extension == "mp3" }
            .sumOf { it.length() }
    }

    override suspend fun setCacheLimit(sizeGb: Int) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey("cache_limit_gb")] = sizeGb
        }
    }

    override suspend fun getCacheLimit(): Int {
        val prefs = dataStore.data.first()
        return prefs[intPreferencesKey("cache_limit_gb")] ?: 2
    }

    override fun getServerUrl(): String {
        return CredentialsManager.getServerUrl() ?: "https://your-navidrome-server.com/"
    }

    override fun setServerUrl(url: String) {
        // Update stored server URL
        val currentUser = CredentialsManager.getUsername()
        val currentPass = CredentialsManager.getPassword()
        if (currentUser != null && currentPass != null) {
            CredentialsManager.saveCredentials(currentUser, currentPass, url)
        }
    }

    // Home screen methods
    @Suppress("UNCHECKED_CAST")
    override suspend fun getLatestAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getLatestAlbums called ===")
            val responseBody = api.getAlbumList2(type = "newest", size = size)
            val response = parseXmlResponse(responseBody)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val albumList = subsonicResponse?.get("albumList2") as? Map<String, Any>
            val albums = albumList?.get("album") as? List<Any> ?: emptyList()
            Log.d("Repo", "Found ${albums.size} albums from API")

            albums.mapNotNull { albumMap ->
                (albumMap as? Map<String, Any>)?.let { am ->
                    val coverArt = am["coverArt"] as? String
                    Log.d("RepoAlbum", "Album: title='${am["title"]}', coverArt='$coverArt'")
                    Album(
                        id = am["id"] as? String ?: "",
                        artistId = am["artistId"] as? String ?: "",
                        artistName = am["artist"] as? String ?: "",
                        title = am["title"] as? String ?: "",
                        year = (am["year"] as? Number)?.toInt(),
                        genre = am["genre"] as? String,
                        coverUrl = coverArt
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getTopAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getTopAlbums (frequent) ===")
            val responseBody = api.getAlbumList2(type = "frequent", size = size)
            val response = parseXmlResponse(responseBody)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val albumList = subsonicResponse?.get("albumList2") as? Map<String, Any>
            val albums = albumList?.get("album") as? List<Any> ?: emptyList()
            Log.d("Repo", "Found ${albums.size} top albums")

            albums.mapNotNull { albumMap ->
                (albumMap as? Map<String, Any>)?.let { am ->
                    Album(
                        id = am["id"] as? String ?: "",
                        artistId = am["artistId"] as? String ?: "",
                        artistName = am["artist"] as? String ?: "",
                        title = am["title"] as? String ?: "",
                        year = (am["year"] as? Number)?.toInt(),
                        genre = am["genre"] as? String,
                        coverUrl = am["coverArt"] as? String
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getRecentlyPlayedAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getRecentlyPlayedAlbums (recent) ===")
            val responseBody = api.getAlbumList2(type = "recent", size = size)
            val response = parseXmlResponse(responseBody)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val albumList = subsonicResponse?.get("albumList2") as? Map<String, Any>
            val albums = albumList?.get("album") as? List<Any> ?: emptyList()
            Log.d("Repo", "Found ${albums.size} recently played albums")

            albums.mapNotNull { albumMap ->
                (albumMap as? Map<String, Any>)?.let { am ->
                    Album(
                        id = am["id"] as? String ?: "",
                        artistId = am["artistId"] as? String ?: "",
                        artistName = am["artist"] as? String ?: "",
                        title = am["title"] as? String ?: "",
                        year = (am["year"] as? Number)?.toInt(),
                        genre = am["genre"] as? String,
                        coverUrl = am["coverArt"] as? String
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getRandomAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getRandomAlbums (random) ===")
            val responseBody = api.getAlbumList2(type = "random", size = size)
            val response = parseXmlResponse(responseBody)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val albumList = subsonicResponse?.get("albumList2") as? Map<String, Any>
            val albums = albumList?.get("album") as? List<Any> ?: emptyList()
            Log.d("Repo", "Found ${albums.size} random albums")

            albums.mapNotNull { albumMap ->
                (albumMap as? Map<String, Any>)?.let { am ->
                    Album(
                        id = am["id"] as? String ?: "",
                        artistId = am["artistId"] as? String ?: "",
                        artistName = am["artist"] as? String ?: "",
                        title = am["title"] as? String ?: "",
                        year = (am["year"] as? Number)?.toInt(),
                        genre = am["genre"] as? String,
                        coverUrl = am["coverArt"] as? String
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getAllAlbums(size: Int): Result<List<Album>> {
        return getAllAlbumsFromServer(offset = 0, size = size)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getAllAlbumsFromServer(offset: Int, size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getAllAlbumsFromServer offset=$offset size=$size ===")
            val responseBody = api.getAlbumList2(type = "alphabeticalByName", size = size, offset = offset)
            val response = parseXmlResponse(responseBody)

            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val albumList = subsonicResponse?.get("albumList2") as? Map<String, Any>
            val albums = albumList?.get("album") as? List<Any> ?: emptyList()
            Log.d("Repo", "Found ${albums.size} albums at offset $offset")

            albums.mapNotNull { albumMap ->
                (albumMap as? Map<String, Any>)?.let { am ->
                    Album(
                        id = am["id"] as? String ?: "",
                        artistId = am["artistId"] as? String ?: "",
                        artistName = am["artist"] as? String ?: "",
                        title = am["title"] as? String ?: "",
                        year = (am["year"] as? Number)?.toInt(),
                        genre = am["genre"] as? String,
                        coverUrl = am["coverArt"] as? String
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getAlbumsByYear(year: Int, size: Int): Result<List<Album>> {
        return runCatching {
            val responseBody = api.getAlbumList2(
                type = "alphabeticalByName",
                size = size,
                fromYear = year,
                toYear = year
            )
            val response = parseXmlResponse(responseBody)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val albumList = subsonicResponse?.get("albumList2") as? Map<String, Any>
            val albums = albumList?.get("album") as? List<Any> ?: emptyList()

            albums.mapNotNull { albumMap ->
                (albumMap as? Map<String, Any>)?.let { am ->
                    Album(
                        id = am["id"] as? String ?: "",
                        artistId = am["artistId"] as? String ?: "",
                        artistName = am["artist"] as? String ?: "",
                        title = am["title"] as? String ?: "",
                        year = (am["year"] as? Number)?.toInt(),
                        genre = am["genre"] as? String,
                        coverUrl = am["coverArt"] as? String
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getAlbumsByGenre(genre: String, size: Int): Result<List<Album>> {
        return runCatching {
            val responseBody = api.getAlbumList2(
                type = "alphabeticalByName",
                size = size,
                genre = genre
            )
            val response = parseXmlResponse(responseBody)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val albumList = subsonicResponse?.get("albumList2") as? Map<String, Any>
            val albums = albumList?.get("album") as? List<Any> ?: emptyList()

            albums.mapNotNull { albumMap ->
                (albumMap as? Map<String, Any>)?.let { am ->
                    Album(
                        id = am["id"] as? String ?: "",
                        artistId = am["artistId"] as? String ?: "",
                        artistName = am["artist"] as? String ?: "",
                        title = am["title"] as? String ?: "",
                        year = (am["year"] as? Number)?.toInt(),
                        genre = am["genre"] as? String,
                        coverUrl = am["coverArt"] as? String
                    )
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getGenres(): Result<List<String>> {
        return runCatching {
            val responseBody = api.getGenres()
            val xmlString = responseBody.string()
            Log.d("Velodrome", "Genres XML: $xmlString")
            
            // Parse genre names from text content: <genre songCount="X" albumCount="Y">GenreName</genre>
            val genreRegex = """<genre[^>]*>([^<]+)</genre>""".toRegex()
            val genres = genreRegex.findAll(xmlString).map { it.groupValues[1] }.toList()
            
            Log.d("Velodrome", "Parsed genres: $genres")
            genres
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getArtist(artistId: String): Result<ArtistWithAlbums> {
        return runCatching {
            Log.d("Repo", "=== getArtist called: $artistId ===")
            val responseBody = api.getArtist(artistId)
            val xmlString = responseBody.string()
            Log.d("Repo", "Raw XML for artist: ${xmlString.take(500)}")
            
            // Parse the XML manually - the nested albums are inside <artist><album>...</album></artist>
            // But XmlParser flattens them, so we need to extract artist info from <artist> and albums from <album> within it
            
            // Extract artist ID and name from <artist> tag
            val artistIdRegex = """<artist\s+id="([^"]+)"\s+name="([^"]+)"""".toRegex()
            val artistMatch = artistIdRegex.find(xmlString)
            val artistName = decodeHtmlEntities(artistMatch?.groupValues?.get(2) ?: "")
            val extractedArtistId = artistMatch?.groupValues?.get(1) ?: artistId
            
            // Extract coverArt and albumCount
            val coverArtRegex = """coverArt="([^"]+)"""".toRegex()
            val coverArtMatch = coverArtRegex.find(xmlString)
            val coverArt = coverArtMatch?.groupValues?.get(1)
            
            val albumCountRegex = """albumCount="(\d+)"""".toRegex()
            val albumCountMatch = albumCountRegex.find(xmlString)
            val albumCount = albumCountMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            
            Log.d("Repo", "Extracted artist: $artistName, albumCount: $albumCount, coverArt: $coverArt")
            
            // Now extract all <album> tags from within the response
            val albumRegex = """<album\s+id="([^"]+)"\s+name="([^"]+)"\s+artist="([^"]+)"\s+artistId="([^"]+)"\s+coverArt="([^"]+)"""".toRegex()
            val albumMatches = albumRegex.findAll(xmlString)
            
            val albums = albumMatches.map { match ->
                val albumId = match.groupValues[1]
                val title = decodeHtmlEntities(match.groupValues[2])
                val artist = decodeHtmlEntities(match.groupValues[3])
                val artistId = match.groupValues[4]
                val coverArt = match.groupValues[5]
                
                // Extract year if present
                val yearRegex = """year="(\d+)"""".toRegex()
                val yearMatch = yearRegex.find(xmlString.substring(match.range.first))
                val year = yearMatch?.groupValues?.get(1)?.toIntOrNull()
                
                Log.d("Repo", "Album: id=$albumId, title=$title, year=$year")
                
                Album(
                    id = albumId,
                    artistId = artistId,
                    artistName = artist,
                    title = title,
                    year = year,
                    genre = null,
                    coverUrl = coverArt
                )
            }.toList()
            
            Log.d("Repo", "Total albums found: ${albums.size}")
            
            val artist = Artist(
                id = extractedArtistId,
                name = artistName,
                albumCount = albumCount,
                coverUrl = coverArt
            )

            ArtistWithAlbums(artist = artist, albums = albums)
        }
    }

    override suspend fun scrobble(trackId: String, time: Long?, submission: Boolean): Result<Unit> {
        return runCatching {
            api.scrobble(trackId, time, submission)
            Log.d("NavidromeRepository", "Scrobbled track: $trackId, submission: $submission")
        }
    }

    override suspend fun getSongsByGenre(genre: String, count: Int, offset: Int): Result<List<Track>> {
        return runCatching {
            Log.d("NavidromeRepository", "getSongsByGenre: genre=$genre, count=$count, offset=$offset")
            val responseBody = api.getSongsByGenre(genre, count, offset)
            val xmlString = responseBody.string()
            Log.d("NavidromeRepository", "getSongsByGenre XML (first 1000): ${xmlString.take(1000)}")
            
            // Parse songs directly using regex
            val songs = parseSongsFromXml(xmlString)
            Log.d("NavidromeRepository", "Parsed ${songs.size} songs from genre $genre")
            songs
        }
    }

    override suspend fun getRandomSongsByGenre(genre: String, size: Int): Result<List<Track>> {
        return runCatching {
            Log.d("NavidromeRepository", "getRandomSongsByGenre: genre=$genre, size=$size")
            val responseBody = api.getRandomSongs(size, genre)
            val xmlString = responseBody.string()
            Log.d("NavidromeRepository", "getRandomSongsByGenre XML (first 1000): ${xmlString.take(1000)}")
            
            // Parse songs directly using regex
            val songs = parseSongsFromXml(xmlString)
            Log.d("NavidromeRepository", "Parsed ${songs.size} random songs from genre $genre")
            songs
        }
    }

    override suspend fun getRandomSongs(size: Int): Result<List<Track>> {
        return runCatching {
            Log.d("NavidromeRepository", "getRandomSongs: size=$size")
            val responseBody = api.getRandomSongs(size, null)
            val xmlString = responseBody.string()
            Log.d("NavidromeRepository", "getRandomSongs XML (first 1000): ${xmlString.take(1000)}")
            
            // Parse songs directly using regex
            val songs = parseSongsFromXml(xmlString)
            Log.d("NavidromeRepository", "Parsed ${songs.size} random songs")
            songs
        }
    }

    /**
     * Parse song elements directly from XML using regex
     */
    private fun parseSongsFromXml(xmlString: String): List<Track> {
        val songs = mutableListOf<Track>()
        
        // Match all <song> elements with their attributes
        val songRegex = """<song\s+([^>]*)>""".toRegex()
        val matches = songRegex.findAll(xmlString)
        
        for (match in matches) {
            val attrs = match.groupValues[1]
            val songMap = mutableMapOf<String, Any>()
            
            // Extract common attributes using regex
            putAttr(songMap, attrs, "id", """\bid="([^"]+)""")
            putAttr(songMap, attrs, "parent", """\bparent="([^"]+)""")
            putAttr(songMap, attrs, "title", """\btitle="([^"]+)""")
            putAttr(songMap, attrs, "album", """\balbum="([^"]+)""")
            putAttr(songMap, attrs, "artist", """\bartist="([^"]+)""")
            putAttr(songMap, attrs, "track", """\btrack="([^"]+)""")
            putAttr(songMap, attrs, "year", """\byear="([^"]+)""")
            putAttr(songMap, attrs, "genre", """\bgenre="([^"]+)""")
            putAttr(songMap, attrs, "coverArt", """\bcoverArt="([^"]+)""")
            putAttr(songMap, attrs, "size", """\bsize="([^"]+)""")
            putAttr(songMap, attrs, "contentType", """\bcontentType="([^"]+)""")
            putAttr(songMap, attrs, "suffix", """\bsuffix="([^"]+)""")
            putAttr(songMap, attrs, "duration", """\bduration="([^"]+)""")
            putAttr(songMap, attrs, "bitRate", """\bbitRate="([^"]+)""")
            
            if (songMap.isNotEmpty()) {
                try {
                    songs.add(parseTrackFromSongMap(songMap))
                } catch (e: Exception) {
                    Log.w("NavidromeRepository", "Error parsing song: ${e.message}")
                }
            }
        }
        
        return songs
    }

    /**
     * Helper to put attribute from regex match with HTML entity decoding
     */
    private fun putAttr(map: MutableMap<String, Any>, attrs: String, key: String, regex: String) {
        val match = Regex(regex).find(attrs)
        if (match != null) {
            map[key] = decodeHtmlEntities(match.groupValues[1])
        }
    }

    /**
     * Helper to parse a Track from a song map (common parsing logic)
     */
    private fun parseTrackFromSongMap(sm: Map<String, Any>): Track {
        val albumId = sm["albumId"] as? String ?: sm["album"] as? String ?: ""
        val durationValue = sm["duration"]
        val durationSec = when (durationValue) {
            is Number -> durationValue.toInt()
            is String -> durationValue.toIntOrNull() ?: 0
            else -> 0
        }
        val sizeValue = sm["size"]
        val sizeBytes = when (sizeValue) {
            is Number -> sizeValue.toLong()
            is String -> sizeValue.toLongOrNull() ?: 0L
            else -> 0L
        }
        val bitrateValue = sm["bitRate"]
        val bitrate = when (bitrateValue) {
            is Number -> bitrateValue.toInt()
            is String -> bitrateValue.toIntOrNull() ?: 0
            else -> 0
        }
        val trackValue = sm["track"]
        val trackNumber = when (trackValue) {
            is Number -> trackValue.toInt()
            is String -> trackValue.toIntOrNull() ?: 0
            else -> 0
        }
        val coverArtValue = sm["coverArt"] as? String
        val albumNameValue = sm["album"] as? String ?: albumId
        val effectiveCoverArtId = coverArtValue ?: "al-$albumId"

        return Track(
            id = sm["id"] as? String ?: "",
            albumId = albumId,
            title = decodeHtmlEntities(sm["title"] as? String ?: ""),
            artistName = decodeHtmlEntities(sm["artist"] as? String ?: ""),
            albumName = albumNameValue,
            durationSec = durationSec,
            sizeBytes = sizeBytes,
            bitrate = bitrate,
            trackNumber = trackNumber,
            isCached = false,
            coverArtId = effectiveCoverArtId
        )
    }

    /**
     * Decode common HTML entities that may appear in XML attributes
     * Navidrome encodes & as &amp; in XML
     */
    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}