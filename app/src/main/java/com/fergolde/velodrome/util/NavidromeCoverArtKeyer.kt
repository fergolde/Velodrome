package com.fergolde.velodrome.util

import coil3.key.Keyer
import coil3.request.Options
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Stable cache key for Navidrome cover-art URLs.
 *
 * [CredentialsManager.getCoverArtUrl] embeds a rotating token/salt (1h TTL +
 * regenerated on every cold start). Without a custom keyer those rotating query
 * params leak into Coil's memory/disk cache keys, busting the whole image cache
 * every hour. Stripping them yields one stable key per (host, coverArt id, size),
 * mirroring what NavidromeCacheKeyFactory does for the audio SimpleCache.
 */
class NavidromeCoverArtKeyer : Keyer<String> {

    override fun key(data: String, options: Options): String? {
        // Null = not our model type, fall back to Coil's default keying.
        if (!data.contains(COVER_ART_PATH)) return null
        return runCatching { normalize(data) }.getOrNull()
    }

    companion object {
        private const val COVER_ART_PATH = "getCoverArt"
        private val ROTATING_PARAMS = setOf("u", "t", "s")

        /**
         * Removes the rotating auth params (u/t/s) while keeping the rest of the
         * URL (host, id, size, v, c) intact and in original order, so the result
         * is deterministic across token rotations.
         */
        fun normalize(url: String): String {
            val parsed = url.toHttpUrlOrNull() ?: return url
            val hasRotatingParams = parsed.queryParameterNames.any { it in ROTATING_PARAMS }
            if (!hasRotatingParams) return url
            return parsed.newBuilder().apply {
                ROTATING_PARAMS.forEach { removeAllQueryParameters(it) }
            }.build().toString()
        }
    }
}
