package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.AlbumDao
import com.fergolde.velodrome.data.local.dao.ArtistDao
import com.fergolde.velodrome.data.local.dao.ScrobbleDao
import com.fergolde.velodrome.data.local.dao.TrackDao
import com.fergolde.velodrome.data.local.queue.QueueSnapshotStore
import com.fergolde.velodrome.domain.repository.ServerMigrationRepository
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.util.ServerDataResetNotifier
import com.fergolde.velodrome.util.ServerVersion
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navidrome 0.64.0 re-encoded every internal ID (media files, albums,
 * playlists) to a canonical 128-bit base62 format, so any locally cached ID
 * from an older server stops resolving. This repository detects the crossing
 * and discards the local library, queue, pending scrobbles and sync stamps;
 * the next sync then runs as a full sync.
 *
 * Server metadata comes from the Subsonic response envelope, so the check
 * never needs an extra endpoint beyond `ping`.
 */
@Singleton
class ServerMigrationRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val artistDao: ArtistDao,
    private val albumDao: AlbumDao,
    private val trackDao: TrackDao,
    private val scrobbleDao: ScrobbleDao,
    private val queueSnapshotStore: QueueSnapshotStore,
    private val resetNotifier: ServerDataResetNotifier
) : ServerMigrationRepository {

    override suspend fun checkAndMigrate(currentServerVersion: String?): Boolean {
        val storedVersion = settingsRepository.lastServerVersion.first()
        // Tracks can be cached without their album/artist rows (e.g. random
        // songs), so all three tables must be checked.
        val hasLocalData = albumDao.getAlbumCount() > 0 ||
            artistDao.getArtistCount() > 0 ||
            trackDao.getTrackCount() > 0

        val shouldReset = ServerVersion.requiresIdMigrationReset(
            storedVersion = storedVersion,
            currentVersion = currentServerVersion,
            hasLocalData = hasLocalData
        )

        if (shouldReset) {
            // Pending scrobbles carry the same stale IDs: the server cannot
            // resolve them anymore and the worker would retry forever.
            artistDao.deleteAll()
            albumDao.deleteAll()
            trackDao.deleteAll()
            scrobbleDao.deleteAll()
            queueSnapshotStore.clear()

            // Zeroed stamps force the full-sync path on the next run.
            settingsRepository.setLastSyncTimestamp(0)
            settingsRepository.setLastSyncOffset(0)
            settingsRepository.setLastServerCheckAt(0)

            resetNotifier.notifyReset()
        }

        if (currentServerVersion != null) {
            settingsRepository.setLastServerVersion(currentServerVersion)
        }

        return shouldReset
    }
}
