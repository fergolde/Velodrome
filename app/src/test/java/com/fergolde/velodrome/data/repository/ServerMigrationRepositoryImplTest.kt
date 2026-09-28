package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.AlbumDao
import com.fergolde.velodrome.data.local.dao.ArtistDao
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.domain.repository.AccountDataRepository
import com.fergolde.velodrome.domain.repository.SettingsRepository
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
    private val accountDataRepository: AccountDataRepository = mockk(relaxed = true)

    private val repository = ServerMigrationRepositoryImpl(
        settingsRepository = settingsRepository,
        artistDao = artistDao,
        albumDao = albumDao,
        trackDao = trackDao,
        accountDataRepository = accountDataRepository
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
        coVerify { accountDataRepository.purgeAccountData() }
        coVerify { settingsRepository.setLastServerVersion("0.64.0") }
    }

    @Test
    fun `same canonical version only records the version`() = runTest {
        storedVersion("0.64.0")
        localDataPresent(true)

        val reset = repository.checkAndMigrate("0.64.1")

        assertFalse(reset)
        coVerify(exactly = 0) { accountDataRepository.purgeAccountData() }
        coVerify { settingsRepository.setLastServerVersion("0.64.1") }
    }

    @Test
    fun `unknown stored version with local data resets once`() = runTest {
        storedVersion(null)
        localDataPresent(true)

        val reset = repository.checkAndMigrate("0.64.0")

        assertTrue(reset)
        coVerify { accountDataRepository.purgeAccountData() }
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
        coVerify { accountDataRepository.purgeAccountData() }
    }

    @Test
    fun `server predating migration does not reset`() = runTest {
        storedVersion("0.63.2")
        localDataPresent(true)

        val reset = repository.checkAndMigrate("0.63.2")

        assertFalse(reset)
        coVerify(exactly = 0) { accountDataRepository.purgeAccountData() }
        coVerify { settingsRepository.setLastServerVersion("0.63.2") }
    }

    @Test
    fun `missing server version never resets nor overwrites stored value`() = runTest {
        storedVersion("0.63.2")
        localDataPresent(true)

        val reset = repository.checkAndMigrate(null)

        assertFalse(reset)
        coVerify(exactly = 0) { accountDataRepository.purgeAccountData() }
        coVerify(exactly = 0) { settingsRepository.setLastServerVersion(any()) }
    }
}
