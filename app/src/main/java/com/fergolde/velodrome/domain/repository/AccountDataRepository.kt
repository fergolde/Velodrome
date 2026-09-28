package com.fergolde.velodrome.domain.repository

/**
 * Discards locally cached data that belongs to the account that is signing out.
 *
 * Without this, a second account signing in on the same device inherits the
 * first account's library metadata, pending scrobbles and player queue.
 */
interface AccountDataRepository {

    /**
     * Wipes the locally stored library, pending scrobbles, player queue and
     * sync stamps, and notifies in-memory holders to drop their state.
     *
     * Audio and image caches are NOT purged: their keys are account-scoped, so
     * the next account cannot read the previous account's cached media and the
     * signed-in user keeps their downloaded music.
     */
    suspend fun purgeAccountData()
}
