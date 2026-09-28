package com.fergolde.velodrome.util

import coil3.key.Keyer
import coil3.request.Options
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Stable cache key for Navidrome cover-art URLs.
 *
 * AuthInterceptor appends rotating token/salt values to cover-art requests.
 * Stripping them yields one stable key per (account, host, coverArt id, size),
 * mirroring what [NavidromeCacheKeyFactory] does for the audio SimpleCache.
 *
 * The account scope is part of the key on purpose: without it, two accounts on
 * the same server would share artwork cache entries, and one account could be
 * served the other account's private cover art.
 */
class NavidromeCoverArtKeyer(
    private val accountScope: () -> String
) : Keyer<String> {

    override fun key(data: String, options: Options): String? {
        // Null = not our model type, fall back to Coil's default keying.
        if (!data.contains(COVER_ART_PATH)) return null
        return runCatching { normalize(data, accountScope()) }.getOrNull()
    }

    companion object {
        private const val COVER_ART_PATH = "getCoverArt"
        private val ROTATING_PARAMS = setOf("u", "t", "s")

        /**
         * Removes the rotating auth params (u/t/s) while keeping the rest of the
         * URL (host, id, size, v, c) intact and in original order, so the result
         * is deterministic across token rotations, then prefixes the account
         * scope so keys never collide across accounts.
         */
        fun normalize(url: String, accountScope: String): String {
            val parsed = url.toHttpUrlOrNull()
                ?: return "$accountScope|$url"
            val hasRotatingParams = parsed.queryParameterNames.any { it in ROTATING_PARAMS }
            val sanitized = if (!hasRotatingParams) {
                parsed
            } else {
                parsed.newBuilder().apply {
                    ROTATING_PARAMS.forEach { removeAllQueryParameters(it) }
                }.build()
            }
            return "$accountScope|$sanitized"
        }
    }
}
