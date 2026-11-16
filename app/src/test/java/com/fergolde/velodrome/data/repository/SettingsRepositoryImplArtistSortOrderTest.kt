package com.fergolde.velodrome.data.repository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.fergolde.velodrome.domain.repository.DEFAULT_ARTIST_ALBUM_SORT_ORDER
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * La preferencia de orden de álbumes (S1) tiene que sobrevivir el reinicio de
 * la app: se guarda en DataStore bajo una clave propia y sin valor guardado
 * devuelve el orden que el detalle de artista ya usaba (año ascendente).
 */
class SettingsRepositoryImplArtistSortOrderTest {

    private lateinit var tempDir: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("velodrome-settings-test").toFile()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(tempDir, "settings.preferences_pb")
        }
        repository = SettingsRepositoryImpl(
            dataStore = dataStore,
            cachePrefs = mockk<SharedPreferences>(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        tempDir.deleteRecursively()
    }

    @Test
    fun `sin preferencia guardada el orden es año ascendente`() = runTest {
        assertEquals(DEFAULT_ARTIST_ALBUM_SORT_ORDER, repository.artistAlbumSortOrder.first())
    }

    @Test
    fun `el orden elegido se guarda y se vuelve a leer`() = runTest {
        repository.setArtistAlbumSortOrder("title_asc")

        assertEquals("title_asc", repository.artistAlbumSortOrder.first())
    }

    @Test
    fun `cambiar el orden no toca el resto de preferencias`() = runTest {
        repository.setAccentColor("#123456")
        repository.setArtistAlbumSortOrder("random")

        assertEquals("#123456", repository.accentColor.first())
        assertEquals("random", repository.artistAlbumSortOrder.first())
    }
}
