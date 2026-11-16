package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.AlbumDao
import com.fergolde.velodrome.data.local.dao.ArtistDao
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.mapper.toDomain
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.repository.SearchSuggestionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sugerencias por prefijo sobre la biblioteca local.
 *
 * Todo sale de Room, así que funciona igual offline y no cuesta una petición
 * por tecla. Un prefijo en blanco devuelve vacío a propósito: `LIKE '%'`
 * listaría la biblioteca entera.
 */
@Singleton
class SearchSuggestionRepositoryImpl @Inject constructor(
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao,
    private val trackDao: TrackDao
) : SearchSuggestionRepository {

    override suspend fun suggestArtists(prefix: String, limit: Int): List<Artist> {
        val normalized = normalizeSuggestionPrefix(prefix) ?: return emptyList()
        return artistDao.searchArtistsByPrefix(normalized, limit).map { it.toDomain() }
    }

    override suspend fun suggestAlbums(prefix: String, limit: Int): List<Album> {
        val normalized = normalizeSuggestionPrefix(prefix) ?: return emptyList()
        return albumDao.searchAlbumsByPrefix(normalized, limit).map { it.toDomain() }
    }

    override suspend fun suggestTracks(prefix: String, limit: Int): List<Track> {
        val normalized = normalizeSuggestionPrefix(prefix) ?: return emptyList()
        return trackDao.searchTracksByPrefix(normalized, limit).map { it.toDomain() }
    }
}

/** Prefijo listo para `LIKE`, o `null` cuando no hay nada que sugerir. */
private fun normalizeSuggestionPrefix(raw: String): String? = raw.trim().ifEmpty { null }
