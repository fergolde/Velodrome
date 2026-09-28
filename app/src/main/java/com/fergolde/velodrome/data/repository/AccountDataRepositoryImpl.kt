package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.AlbumDao
import com.fergolde.velodrome.data.local.dao.ArtistDao
import com.fergolde.velodrome.data.local.dao.ScrobbleDao
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.queue.QueueSnapshotStore
import com.fergolde.velodrome.domain.repository.AccountDataRepository
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.util.ServerDataResetNotifier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drops everything tied to the account that just signed out.
 *
 * The library tables, the pending scrobbles and the queue snapshot all belong
 * to one account: leaving them behind means the next login sees the previous
 * user's library, and worse, `ScrobbleWorker` would submit the previous user's
 * listening history under the NEW account's token.
 */
@Singleton
class AccountDataRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao,
    private val trackDao: TrackDao,
    private val scrobbleDao: ScrobbleDao,
    private val queueSnapshotStore: QueueSnapshotStore,
    private val resetNotifier: ServerDataResetNotifier
) : AccountDataRepository {

    override suspend fun purgeAccountData() {
        artistDao.deleteAll()
        albumDao.deleteAll()
        trackDao.deleteAll()
        scrobbleDao.deleteAll()
        queueSnapshotStore.clear()

        // Zeroed stamps force the full-sync path for the next account.
        settingsRepository.setLastSyncTimestamp(0)
        settingsRepository.setLastSyncOffset(0)
        settingsRepository.setLastServerCheckAt(0)
        settingsRepository.setLastServerVersion(null)

        // Drops the in-memory player queue and radio snapshot keyed by the old
        // account's server IDs.
        resetNotifier.notifyReset()
    }
}
