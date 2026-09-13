package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.AlbumDao
import com.fergolde.velodrome.data.local.dao.ArtistDao
import com.fergolde.velodrome.data.local.dao.ScrobbleDao
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.queue.QueueSnapshotStore
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.util.ServerDataResetNotifier
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerMigrationRepositoryImplTest {

    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val artistDao: ArtistDao = mockk(relaxed = true)
    private val albumDao: AlbumDao = mockk(relaxed = true)
    private val trackDao: TrackDao = mockk(relaxed = true)
    private val scrobbleDao: ScrobbleDao = mockk(relaxed = true)
    private val queueSnapshotStore: QueueSnapshotStore = mockk(relaxed = true)
    private val resetNotifier: ServerDataResetNotifier = mockk(relaxed = true)

    private val repository = ServerMigrationRepositoryImpl(
        settingsRepository = settingsRepository,
        artistDao = artistDao,
        albumDao = albumDao,
        trackDao = trackDao,
        scrobbleDao = scrobbleDao,
        queueSnapshotStore = queueSnapshotStore,
        resetNotifier = resetNotifier
    )

    private fun storedVersion(version: String?) {
        every { settingsRepository.lastServerVersion } returns flowOf(version)
    }

    private fun localDataPresent(present: Boolean) {
        coEvery { albumDao.getAlbumCount() } returns if (present) 10 else 0
        coEvery { artistDao.getArtistCount() } returns if (present) 5 else 0
        coEvery { trackDao.getTrackCount() } returns if (present) 25 else 0
    }

    @Test
    fun `crossing canonical ids boundary wipes local state and notifies`() = runTest {
        storedVersion("0.63.2")
        localDataPresent(true)

        val reset = repository.checkAndMigrate("0.64.0")

        assertTrue(reset)
        coVerify { artistDao.deleteAll() }
        coVerify { albumDao.deleteAll() }
        coVerify { trackDao.deleteAll() }
        coVerify { scrobbleDao.deleteAll() }
        coVerify { queueSnapshotStore.clear() }
        coVerify { settingsRepository.setLastSyncTimestamp(0) }
        coVerify { settingsRepository.setLastSyncOffset(0) }
        coVerify { settingsRepository.setLastServerCheckAt(0) }
        verify { resetNotifier.notifyReset() }
        coVerify { settingsRepository.setLastServerVersion("0.64.0") }
    }

    @Test
    fun `same canonical version only records the version`() = runTest {
        storedVersion("0.64.0")
        localDataPresent(true)

        val reset = repository.checkAndMigrate("0.64.1")

        assertFalse(reset)
        coVerify(exactly = 0) { trackDao.deleteAll() }
        coVerify(exactly = 0) { queueSnapshotStore.clear() }
        verify(exactly = 0) { resetNotifier.notifyReset() }
        coVerify { settingsRepository.setLastServerVersion("0.64.1") }
    }

    @Test
    fun `unknown stored version with local data resets once`() = runTest {
        storedVersion(null)
        localDataPresent(true)

        val reset = repository.checkAndMigrate("0.64.0")

        assertTrue(reset)
        coVerify { trackDao.deleteAll() }
        coVerify { settingsRepository.setLastServerVersion("0.64.0") }
    }

    @Test
    fun `cached tracks alone count as local data`() = runTest {
        storedVersion(null)
        coEvery { albumDao.getAlbumCount() } returns 0
        coEvery { artistDao.getArtistCount() } returns 0
        coEvery { trackDao.getTrackCount() } returns 3

        val reset = repository.checkAndMigrate("0.64.0")

        assertTrue(reset)
        coVerify { trackDao.deleteAll() }
    }

    @Test
    fun `server predating migration does not reset`() = runTest {
        storedVersion("0.63.2")
        localDataPresent(true)

        val reset = repository.checkAndMigrate("0.63.2")

        assertFalse(reset)
        coVerify(exactly = 0) { trackDao.deleteAll() }
        verify(exactly = 0) { resetNotifier.notifyReset() }
        coVerify { settingsRepository.setLastServerVersion("0.63.2") }
    }

    @Test
    fun `missing server version never resets nor overwrites stored value`() = runTest {
        storedVersion("0.63.2")
        localDataPresent(true)

        val reset = repository.checkAndMigrate(null)

        assertFalse(reset)
        coVerify(exactly = 0) { trackDao.deleteAll() }
        coVerify(exactly = 0) { settingsRepository.setLastServerVersion(any()) }
    }
}
