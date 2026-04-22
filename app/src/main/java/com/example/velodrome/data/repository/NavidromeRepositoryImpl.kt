package com.example.velodrome.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.data.remote.dto.AlbumDetailDto
import com.example.velodrome.data.remote.dto.AlbumDto
import com.example.velodrome.data.remote.dto.ArtistDto
import com.example.velodrome.data.remote.dto.ArtistDetailDto
import com.example.velodrome.data.remote.dto.GenreDto
import com.example.velodrome.data.remote.dto.GenresDto
import com.example.velodrome.data.remote.dto.SongDto
import com.example.velodrome.data.remote.dto.SubsonicResponse
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.ArtistWithAlbums
import com.example.velodrome.domain.model.AuthResult
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.repository.NavidromeRepository
import com.example.velodrome.util.CredentialsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavidromeRepositoryImpl @Inject constructor(
    private val api: NavidromeApi,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
    private val credentialsManager: CredentialsManager
) : NavidromeRepository {

    private val cacheDir: File = context.cacheDir

    override suspend fun login(username: String, password: String, serverUrl: String): Result<AuthResult> {
        return runCatching {
            try {
                // Save credentials securely (username + password, NO token)
                credentialsManager.saveCredentials(username, password, serverUrl)
                Log.d("NavidromeRepository", "Credentials saved for user: $username, server: $serverUrl")

                // Try ping - auth interceptor will add u, t, s params automatically
                Log.d("NavidromeRepository", "Calling api.ping()...")
                val response = api.ping()
                Log.d("NavidromeRepository", "Ping response: $response")
                Log.d("NavidromeRepository", "Ping response status: ${response.response.status}")

                if (response.response.status == "ok") {
                    Log.d("Velodrome", "Login successful!")
                    AuthResult(success = true, token = password)
                } else {
                    // Login failed - clear credentials
                    credentialsManager.clearCredentials()
                    val errorMsg = response.response.error?.message ?: "Invalid credentials"
                    Log.d("Velodrome", "Error: $errorMsg")
                    AuthResult(success = false, error = errorMsg)
                }
            } catch (e: Exception) {
                Log.e("Velodrome", "Login exception", e)
                // Clear credentials on error
                credentialsManager.clearCredentials()
                AuthResult(success = false, error = e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Check if user is already logged in (has stored credentials)
     */
    override fun isLoggedIn(): Boolean {
        return credentialsManager.hasCredentials()
    }

    /**
     * Force logout - clear all stored credentials
     */
    override fun logout() {
        credentialsManager.clearCredentials()
    }

    override suspend fun getArtists(offset: Int, size: Int): Result<List<Artist>> {
        return runCatching {
            Log.d("Repo", "=== getArtists called ===")
            val response = api.getArtists(size, offset)

            // Artists response has "index" with nested artists or flat "artist" list
            val indexes = response.response.artists?.indexes
            val flatArtists = response.response.artists?.artistList

            val artistsList = when {
                indexes != null -> {
                    // Nested format: index[0].artists[] - flatten all
                    indexes.flatMap { it.artists ?: emptyList() }
                }
                flatArtists != null -> flatArtists
                else -> emptyList()
            }

            Log.d("Repo", "Found ${artistsList.size} artists")

            artistsList.map { artistDto ->
                Artist(
                    id = artistDto.id,
                    name = decodeHtmlEntities(artistDto.name),
                    albumCount = artistDto.albumCount ?: 0,
                    coverUrl = artistDto.coverArt
                )
            }.also { artists ->
                Log.d("Repo", "Mapped ${artists.size} artists")
            }
        }
    }

    override suspend fun getAlbum(albumId: String): Result<Album> {
        return runCatching {
            Log.d("Repo", "=== getAlbum called: $albumId ===")
            val response = api.getAlbum(albumId)
            val albumDto = response.response.album

            Log.d("Repo", "Got album DTO: id=${albumDto?.id}, title=${albumDto?.title}")

            val dto = albumDto ?: AlbumDetailDto(id = albumId)
            Album(
                id = dto.id,
                artistId = dto.artistId ?: "",
                artistName = dto.artist ?: "",
                title = dto.title ?: dto.name ?: "",
                year = dto.year,
                genre = dto.genre,
                coverUrl = dto.coverArt
            )
        }
    }

    override suspend fun getTracks(albumId: String): Result<List<Track>> {
        return runCatching {
            Log.d("Velodrome", "getTracks called for albumId: $albumId")
            val response = api.getAlbum(albumId)
            val songsList = response.response.album?.songs ?: emptyList()
            Log.d("Velodrome", "Found ${songsList.size} songs in album")

            songsList.map { songDto ->
                val effectiveCoverArtId = songDto.coverArt ?: "al-$albumId"
                Track(
                    id = songDto.id,
                    albumId = albumId,
                    title = songDto.title,
                    artistName = songDto.artist ?: "",
                    albumName = songDto.album ?: albumId,
                    durationSec = songDto.duration ?: 0,
                    sizeBytes = songDto.size ?: 0L,
                    bitrate = songDto.bitRate ?: 0,
                    trackNumber = songDto.track ?: 0,
                    isCached = false,
                    coverArtId = effectiveCoverArtId
                )
            }.also { tracks ->
                Log.d("Velodrome", "Mapped ${tracks.size} tracks")
            }
        }
    }

    override suspend fun getStreamUrl(trackId: String): String {
        // For streaming, we need to build URL manually with auth
        val serverUrl = credentialsManager.getServerUrl() ?: ""
        val authParams = credentialsManager.generateAuthParams()
        
        return if (authParams != null) {
            val (username, token, salt) = authParams
            "${serverUrl}rest/stream.view?id=$trackId&u=$username&t=$token&s=$salt&v=1.16.1&c=Velodrome&maxBitRate=320"
        } else {
            // Fallback - shouldn't happen if logged in
            "${serverUrl}rest/stream.view?id=$trackId&maxBitRate=320"
        }
    }

    override suspend fun search(query: String): Result<List<Artist>> {
        return runCatching {
            val response = api.search(query)
            val artists = response.response.searchResult2?.artists ?: emptyList()
            artists.map { dto ->
                Artist(
                    id = dto.id,
                    name = dto.name,
                    albumCount = dto.albumCount ?: 0,
                    coverUrl = dto.coverArt
                )
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
        return credentialsManager.getServerUrl() ?: "https://your-navidrome-server.com/"
    }

    override fun setServerUrl(url: String) {
        // Update stored server URL
        val currentUser = credentialsManager.getUsername()
        val currentPass = credentialsManager.getPassword()
        if (currentUser != null && currentPass != null) {
            credentialsManager.saveCredentials(currentUser, currentPass, url)
        }
    }

    // Home screen methods - Album list helpers
    private fun mapAlbumDto(dto: AlbumDto): Album {
        return Album(
            id = dto.id,
            artistId = dto.artistId ?: "",
            artistName = dto.artist ?: "",
            title = dto.title ?: dto.name ?: "",
            year = dto.year,
            genre = dto.genre,
            coverUrl = dto.coverArt
        )
    }

    override suspend fun getLatestAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getLatestAlbums called ===")
            val response = api.getAlbumList2(type = "newest", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            Log.d("Repo", "Found ${albums.size} albums from API")

            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getTopAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getTopAlbums (frequent) ===")
            val response = api.getAlbumList2(type = "frequent", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            Log.d("Repo", "Found ${albums.size} top albums")

            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getRecentlyPlayedAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getRecentlyPlayedAlbums (recent) ===")
            val response = api.getAlbumList2(type = "recent", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            Log.d("Repo", "Found ${albums.size} recently played albums")

            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getRandomAlbums(size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getRandomAlbums (random) ===")
            val response = api.getAlbumList2(type = "random", size = size)
            val albums = response.response.albumList2?.albums ?: emptyList()
            Log.d("Repo", "Found ${albums.size} random albums")

            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getAllAlbums(size: Int): Result<List<Album>> {
        return getAllAlbumsFromServer(offset = 0, size = size)
    }

    override suspend fun getAllAlbumsFromServer(offset: Int, size: Int): Result<List<Album>> {
        return runCatching {
            Log.d("Repo", "=== getAllAlbumsFromServer offset=$offset size=$size ===")
            val response = api.getAlbumList2(type = "alphabeticalByName", size = size, offset = offset)
            val albums = response.response.albumList2?.albums ?: emptyList()
            Log.d("Repo", "Found ${albums.size} albums at offset $offset")

            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getAlbumsByYear(year: Int, size: Int): Result<List<Album>> {
        return runCatching {
            val response = api.getAlbumList2(
                type = "alphabeticalByName",
                size = size,
                fromYear = year,
                toYear = year
            )
            val albums = response.response.albumList2?.albums ?: emptyList()
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getAlbumsByGenre(genre: String, size: Int): Result<List<Album>> {
        return runCatching {
            val response = api.getAlbumList2(
                type = "alphabeticalByName",
                size = size,
                genre = genre
            )
            val albums = response.response.albumList2?.albums ?: emptyList()
            albums.map { mapAlbumDto(it) }
        }
    }

    override suspend fun getGenres(): Result<List<String>> {
        return runCatching {
            val response = api.getGenres()
            val genres = response.response.genres?.genres ?: emptyList()
            Log.d("Velodrome", "Found ${genres.size} genres")
            genres.mapNotNull { it.value ?: it.name }
        }
    }

    override suspend fun getArtist(artistId: String): Result<ArtistWithAlbums> {
        return runCatching {
            Log.d("Repo", "=== getArtist called: $artistId ===")
            val response = api.getArtist(artistId)
            val artistDto = response.response.artist

            Log.d("Repo", "Got artist DTO: id=${artistDto?.id}, name=${artistDto?.name}")

            val dto = artistDto ?: ArtistDetailDto(id = artistId, name = "Unknown")
            val albums = dto.albums?.map { mapAlbumDto(it) } ?: emptyList()

            Log.d("Repo", "Found ${albums.size} albums for artist ${dto.name}")

            val artist = Artist(
                id = dto.id,
                name = dto.name,
                albumCount = dto.albumCount ?: 0,
                coverUrl = dto.coverArt
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

    // ============ Song helpers ============
    private fun mapSongDto(dto: SongDto, albumId: String): Track {
        val effectiveCoverArtId = dto.coverArt ?: "al-$albumId"
        return Track(
            id = dto.id,
            albumId = albumId,
            title = dto.title,
            artistName = dto.artist ?: "",
            albumName = dto.album ?: albumId,
            durationSec = dto.duration ?: 0,
            sizeBytes = dto.size ?: 0L,
            bitrate = dto.bitRate ?: 0,
            trackNumber = dto.track ?: 0,
            isCached = false,
            coverArtId = effectiveCoverArtId
        )
    }

    override suspend fun getSongsByGenre(genre: String, count: Int, offset: Int): Result<List<Track>> {
        return runCatching {
            Log.d("NavidromeRepository", "getSongsByGenre: genre=$genre, count=$count, offset=$offset")
            val response = api.getSongsByGenre(genre, count, offset)
            val songDtos = response.response.songsByGenre?.song ?: emptyList()
            Log.d("NavidromeRepository", "Found ${songDtos.size} songs from genre $genre")
            songDtos.map { mapSongDto(it, it.albumId ?: genre) }
        }
    }

    override suspend fun getRandomSongsByGenre(genre: String, size: Int): Result<List<Track>> {
        return runCatching {
            Log.d("NavidromeRepository", "getRandomSongsByGenre: genre=$genre, size=$size")
            val response = api.getRandomSongs(size, genre)
            val songDtos = response.response.randomSongs?.song ?: emptyList()
            Log.d("NavidromeRepository", "Found ${songDtos.size} random songs from genre $genre")
            songDtos.map { mapSongDto(it, it.albumId ?: genre) }
        }
    }

    override suspend fun getRandomSongs(size: Int): Result<List<Track>> {
        return runCatching {
            Log.d("NavidromeRepository", "getRandomSongs: size=$size")
            val response = api.getRandomSongs(size, null)
            Log.d("NavidromeRepository", "getRandomSongs: response=$response")
            val songDtos = response.response.randomSongs?.song ?: emptyList()
            Log.d("NavidromeRepository", "Found ${songDtos.size} random songs")
            songDtos.map { mapSongDto(it, it.albumId ?: "") }
        }
    }

    /**
     * Decode common HTML entities that may appear in JSON values
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