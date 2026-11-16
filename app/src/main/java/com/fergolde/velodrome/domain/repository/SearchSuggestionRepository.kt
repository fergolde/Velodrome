package com.fergolde.velodrome.domain.repository

import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.Track

/**
 * Sugerencias de búsqueda resueltas contra la biblioteca local.
 *
 * Son por prefijo (no por coincidencia en cualquier posición) y no tocan la
 * red: mientras el usuario escribe se sugiere lo que ya está sincronizado.
 */
interface SearchSuggestionRepository {
    suspend fun suggestArtists(prefix: String, limit: Int = DEFAULT_SUGGESTION_LIMIT): List<Artist>

    suspend fun suggestAlbums(prefix: String, limit: Int = DEFAULT_SUGGESTION_LIMIT): List<Album>

    suspend fun suggestTracks(prefix: String, limit: Int = DEFAULT_SUGGESTION_LIMIT): List<Track>

    companion object {
        const val DEFAULT_SUGGESTION_LIMIT = 5
    }
}
