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
            // Auth params added automatically by AuthInterceptor
            val responseBody = api.getArtists(size, offset)
            val response = parseXmlResponse(responseBody)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val artistsData = subsonicResponse?.get("artists") as? Map<String, Any>
            val artistsList = artistsData?.get("artist") as? List<Any> ?: emptyList()

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

    @Suppress("UNCHECKED_CAST")
    override suspend fun getAlbum(albumId: String): Result<Album> {
        return runCatching {
            val responseBody = api.getAlbum(albumId)
            val response = parseXmlResponse(responseBody)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val albumData = subsonicResponse?.get("album") as? Map<String, Any>
            val albumMap = albumData?.get("album") as? Map<String, Any>

            Album(
                id = albumMap?.get("id") as? String ?: "",
                artistId = albumMap?.get("artistId") as? String ?: "",
                artistName = albumMap?.get("artist") as? String ?: "",
                title = albumMap?.get("name") as? String ?: "",
                year = (albumMap?.get("year") as? Number)?.toInt(),
                genre = albumMap?.get("genre") as? String,
                coverUrl = albumMap?.get("coverArt") as? String
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getTracks(albumId: String): Result<List<Track>> {
        return runCatching {
            val responseBody = api.getAlbum(albumId)
            val response = parseXmlResponse(responseBody)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val albumData = subsonicResponse?.get("album") as? Map<String, Any>
            val songsList = albumData?.get("song") as? List<Any> ?: emptyList()

            songsList.mapNotNull { songMap ->
                (songMap as? Map<String, Any>)?.let { sm ->
                    Track(
                        id = sm["id"] as? String ?: "",
                        albumId = albumId,
                        title = sm["title"] as? String ?: "",
                        durationSec = (sm["duration"] as? Number)?.toInt() ?: 0,
                        sizeBytes = (sm["size"] as? Number)?.toLong() ?: 0L,
                        bitrate = (sm["bitRate"] as? Number)?.toInt() ?: 0,
                        trackNumber = (sm["track"] as? Number)?.toInt() ?: 0,
                        isCached = false
                    )
                }
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
            val response = parseXmlResponse(responseBody)
            
            val subsonicResponse = response["subsonic-response"] as? Map<String, Any>
            val genresData = subsonicResponse?.get("genres") as? Map<String, Any>
            val genresList = genresData?.get("genre") as? List<Any> ?: emptyList()

            genresList.mapNotNull { genreMap ->
                (genreMap as? Map<String, Any>)?.let { gm ->
                    gm["value"] as? String
                }
            }
        }
    }
}