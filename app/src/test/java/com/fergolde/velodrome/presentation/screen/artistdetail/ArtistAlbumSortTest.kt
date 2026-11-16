package com.fergolde.velodrome.presentation.screen.artistdetail

import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.repository.DEFAULT_ARTIST_ALBUM_SORT_ORDER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Orden y filtros de álbumes del detalle de artista (S1), probados sobre las
 * funciones puras para que la decisión viva fuera de Compose y de Room.
 */
class ArtistAlbumSortTest {

    private fun album(id: String, title: String, year: Int?) = Album(
        id = id,
        artistId = "a1",
        artistName = "Artist",
        title = title,
        year = year,
        genre = null,
        coverUrl = null
    )

    private fun track(id: String, albumId: String, playCount: Int) = Track(
        id = id,
        albumId = albumId,
        title = "t-$id",
        durationSec = 180,
        sizeBytes = 0L,
        trackNumber = 1,
        playCount = playCount
    )

    // --- Año ---

    @Test
    fun `año ascendente mantiene el orden de hoy con los años desconocidos primero`() {
        val unknown = album("u", "Unknown", null)
        val old = album("old", "Old", 1990)
        val modern = album("mod", "Modern", 2020)

        val sorted = sortArtistAlbums(listOf(modern, unknown, old), AlbumSortOrder.YEAR_ASC)

        assertEquals(listOf(unknown, old, modern), sorted)
    }

    @Test
    fun `año ascendente es estable entre álbumes del mismo año`() {
        val first = album("1", "First", 2000)
        val second = album("2", "Second", 2000)

        val sorted = sortArtistAlbums(listOf(first, second), AlbumSortOrder.YEAR_ASC)

        assertEquals(listOf(first, second), sorted)
    }

    @Test
    fun `año descendente invierte y deja los años desconocidos al final`() {
        val unknown = album("u", "Unknown", null)
        val old = album("old", "Old", 1990)
        val modern = album("mod", "Modern", 2020)

        val sorted = sortArtistAlbums(listOf(unknown, old, modern), AlbumSortOrder.YEAR_DESC)

        assertEquals(listOf(modern, old, unknown), sorted)
    }

    // --- Nombre ---

    @Test
    fun `nombre ordena alfabéticamente sin distinguir mayúsculas`() {
        val b = album("b", "beta", 1990)
        val a = album("a", "Alpha", 2020)
        val c = album("c", "Gamma", 2000)

        val sorted = sortArtistAlbums(listOf(b, a, c), AlbumSortOrder.TITLE_ASC)

        assertEquals(listOf(a, b, c), sorted)
    }

    // --- Más tocadas (historial local) ---

    @Test
    fun `más tocadas ordena por reproducciones locales descendente`() {
        val few = album("few", "Few", 1990)
        val many = album("many", "Many", 2020)
        val none = album("none", "None", 2000)

        val sorted = sortArtistAlbums(
            albums = listOf(few, none, many),
            order = AlbumSortOrder.MOST_PLAYED,
            playsByAlbum = mapOf("few" to 3, "many" to 12)
        )

        assertEquals(listOf(many, few, none), sorted)
    }

    @Test
    fun `más tocadas desempata por año y luego por nombre`() {
        val older = album("older", "Zeta", 1990)
        val newer = album("newer", "Alpha", 2020)

        val sorted = sortArtistAlbums(
            albums = listOf(newer, older),
            order = AlbumSortOrder.MOST_PLAYED,
            playsByAlbum = mapOf("older" to 5, "newer" to 5)
        )

        assertEquals(listOf(older, newer), sorted)
    }

    @Test
    fun `más tocadas sin historial conserva el orden de entrada`() {
        val first = album("1", "First", 1990)
        val second = album("2", "Second", 2020)

        val sorted = sortArtistAlbums(listOf(first, second), AlbumSortOrder.MOST_PLAYED)

        assertEquals(listOf(first, second), sorted)
    }

    @Test
    fun `el historial local suma reproducciones por álbum`() {
        val tracks = listOf(
            track("t1", "al1", 4),
            track("t2", "al1", 6),
            track("t3", "al2", 1)
        )

        val plays = localPlaysByAlbum(tracks)

        assertEquals(mapOf("al1" to 10, "al2" to 1), plays)
    }

    // --- Aleatorio ---

    @Test
    fun `aleatorio es determinista con la misma semilla`() {
        val albums = (1..8).map { album("$it", "Album $it", 2000 + it) }

        val first = sortArtistAlbums(albums, AlbumSortOrder.RANDOM, randomSeed = 42)
        val second = sortArtistAlbums(albums, AlbumSortOrder.RANDOM, randomSeed = 42)

        assertEquals(first, second)
        assertEquals(albums.toSet(), first.toSet())
    }

    @Test
    fun `semillas distintas dan órdenes distintos`() {
        val albums = (1..8).map { album("$it", "Album $it", 2000 + it) }

        val first = sortArtistAlbums(albums, AlbumSortOrder.RANDOM, randomSeed = 1)
        val second = sortArtistAlbums(albums, AlbumSortOrder.RANDOM, randomSeed = 2)

        assertNotEquals(first, second)
    }

    // --- Filtro por año ---

    @Test
    fun `los años disponibles son los del artista sin repetir y ordenados`() {
        val albums = listOf(
            album("1", "A", 2020),
            album("2", "B", 1990),
            album("3", "C", 2020),
            album("4", "D", null)
        )

        assertEquals(listOf(1990, 2020), albumYearsOf(albums))
        assertEquals(emptyList<Int>(), albumYearsOf(emptyList()))
    }

    @Test
    fun `el filtro por año deja solo los álbumes de ese año`() {
        val old = album("old", "Old", 1990)
        val modern = album("mod", "Modern", 2020)

        val filtered = visibleArtistAlbums(
            albums = listOf(modern, old),
            order = AlbumSortOrder.YEAR_ASC,
            selectedYear = 2020
        )

        assertEquals(listOf(modern), filtered)
    }

    @Test
    fun `sin filtro devuelve todos los álbumes aunque no tengan año`() {
        val unknown = album("u", "Unknown", null)
        val modern = album("mod", "Modern", 2020)

        val filtered = visibleArtistAlbums(listOf(unknown, modern), AlbumSortOrder.YEAR_ASC, null)

        assertEquals(listOf(unknown, modern), filtered)
    }

    @Test
    fun `el filtro se aplica antes del orden`() {
        val playedA = album("a", "Alpha", 2020)
        val playedB = album("b", "Beta", 2020)
        val other = album("c", "Gamma", 1990)

        val visible = visibleArtistAlbums(
            albums = listOf(other, playedA, playedB),
            order = AlbumSortOrder.MOST_PLAYED,
            selectedYear = 2020,
            playsByAlbum = mapOf("a" to 1, "b" to 9, "c" to 50)
        )

        assertEquals(listOf(playedB, playedA), visible)
        assertTrue(other !in visible)
    }

    // --- Preferencia persistida ---

    @Test
    fun `la clave guardada sobrevive el viaje de ida y vuelta`() {
        AlbumSortOrder.entries.forEach { order ->
            assertEquals(order, AlbumSortOrder.fromStorageKey(order.storageKey))
        }
    }

    @Test
    fun `sin preferencia guardada el orden por defecto es año ascendente`() {
        assertEquals(AlbumSortOrder.YEAR_ASC, AlbumSortOrder.fromStorageKey(null))
        assertEquals(AlbumSortOrder.YEAR_ASC, AlbumSortOrder.fromStorageKey(""))
        assertEquals(AlbumSortOrder.YEAR_ASC, AlbumSortOrder.fromStorageKey("desconocido"))
        assertEquals(AlbumSortOrder.YEAR_ASC.storageKey, DEFAULT_ARTIST_ALBUM_SORT_ORDER)
    }
}
