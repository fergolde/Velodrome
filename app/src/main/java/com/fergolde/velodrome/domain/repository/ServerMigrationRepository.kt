package com.fergolde.velodrome.domain.repository

/**
 * Detects server-side data migrations that invalidate locally cached IDs and
 * discards local state so the next sync runs from scratch.
 */
interface ServerMigrationRepository {

    /**
     * Records [currentServerVersion] and, when the server crossed into the
     * canonical ID format (Navidrome 0.64.0), wipes locally cached server data
     * and forces a full re-sync.
     *
     * @return true when local data was reset.
     */
    suspend fun checkAndMigrate(currentServerVersion: String?): Boolean
}
