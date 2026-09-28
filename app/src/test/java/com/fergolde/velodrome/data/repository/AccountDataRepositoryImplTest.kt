package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.AlbumDao
import com.fergolde.velodrome.data.local.dao.ArtistDao
import com.fergolde.velodrome.data.local.dao.ScrobbleDao
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.queue.QueueSnapshotStore
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.util.ServerDataResetNotifier
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountDataRepositoryImplTest {

    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val artistDao: ArtistDao = mockk(relaxed = true)
    private val albumDao: AlbumDao = mockk(relaxed = true)
    private val trackDao: TrackDao = mockk(relaxed = true)
    private val scrobbleDao: ScrobbleDao = mockk(relaxed = true)
    private val queueSnapshotStore: QueueSnapshotStore = mockk(relaxed = true)
    private val resetNotifier: ServerDataResetNotifier = mockk(relaxed = true)

    private val repository = AccountDataRepositoryImpl(
        settingsRepository = settingsRepository,
        artistDao = artistDao,
        albumDao = albumDao,
        trackDao = trackDao,
        scrobbleDao = scrobbleDao,
        queueSnapshotStore = queueSnapshotStore,
        resetNotifier = resetNotifier
    )

    @Test
    fun `purge wipes the previous account's library and queue`() = runTest {
        repository.purgeAccountData()

        coVerify { artistDao.deleteAll() }
        coVerify { albumDao.deleteAll() }
        coVerify { trackDao.deleteAll() }
        coVerify { queueSnapshotStore.clear() }
        verify { resetNotifier.notifyReset() }
    }

    @Test
    fun `purge drops pending scrobbles so they are not replayed as the next account`() = runTest {
        // The real leak: ScrobbleWorker submits whatever is pending using the
        // CURRENT credentials, so leftovers would land in the next account's
        // listening history.
        repository.purgeAccountData()

        coVerify { scrobbleDao.deleteAll() }
    }

    @Test
    fun `purge zeroes sync stamps so the next account runs a full sync`() = runTest {
        repository.purgeAccountData()

        coVerify { settingsRepository.setLastSyncTimestamp(0) }
        coVerify { settingsRepository.setLastSyncOffset(0) }
        coVerify { settingsRepository.setLastServerCheckAt(0) }
    }

    @Test
    fun `purge clears the stored server version`() = runTest {
        // Left set, the next account would be compared against the previous
        // server's version and could skip the ID-migration reset.
        repository.purgeAccountData()

        coVerify { settingsRepository.setLastServerVersion(null) }
    }

    @Test
    fun `purge does not clear user preferences`() = runTest {
        repository.purgeAccountData()

        coVerify(exactly = 0) { settingsRepository.setAccentColor(any()) }
        coVerify(exactly = 0) { settingsRepository.setEqEnabled(any()) }
        coVerify(exactly = 0) { settingsRepository.setMusicCacheSizeGb(any()) }
    }

    @Test
    fun `purge notifies exactly once`() = runTest {
        // The notifier drives AudioPlayerManager clearing its in-memory queue;
        // a second signal would re-run that teardown for no reason.
        repository.purgeAccountData()

        verify(exactly = 1) { resetNotifier.notifyReset() }
    }
}
