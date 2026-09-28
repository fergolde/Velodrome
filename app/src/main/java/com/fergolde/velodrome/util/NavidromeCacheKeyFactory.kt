package com.fergolde.velodrome.util

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheKeyFactory

/**
 * Builds Media3 SimpleCache keys for streamed tracks.
 *
 * The key is scoped to the account AND the server. Without that scope, a second
 * account logging in on the same device would read the first account's cached
 * audio straight off disk, and two different servers issuing colliding track
 * IDs would overwrite each other's spans.
 */
@UnstableApi
class NavidromeCacheKeyFactory(
    private val accountScope: () -> String
) : CacheKeyFactory {
    override fun buildCacheKey(dataSpec: DataSpec): String {
        val uri = dataSpec.uri
        val trackId = uri.getQueryParameter("id")
        return if (!trackId.isNullOrBlank()) {
            trackCacheKey(accountScope(), trackId)
        } else {
            uri.buildUpon().clearQuery().build().toString()
        }
    }

    companion object {
        /**
         * Single source of truth for the audio cache key. [CacheManager] must
         * build its lookup keys through here too: it used to hardcode the same
         * string, so scoping one side without the other would make offline
         * detection silently wrong.
         */
        fun trackCacheKey(accountScope: String, trackId: String): String =
            "navidrome_${accountScope}_track_$trackId"
    }
}