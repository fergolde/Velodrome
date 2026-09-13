package com.fergolde.velodrome.util

/**
 * Pure helpers to reason about the Navidrome version reported by the Subsonic
 * response (`serverVersion` attribute).
 */
object ServerVersion {

    /**
     * First Navidrome release whose database IDs are canonical 128-bit base62.
     * A client upgrading across this boundary can no longer resolve any cached
     * ID (albums, tracks, playlists), so local data must be re-synced.
     */
    const val CANONICAL_IDS_VERSION = "0.64.0"

    /**
     * Parses `major.minor.patch`, tolerating an optional `v` prefix and
     * pre-release/build suffixes (e.g. `0.64.0-SNAPSHOT`). Missing minor or
     * patch components default to 0. Returns null when the value is absent or
     * has no numeric major component.
     */
    fun parse(raw: String?): Triple<Int, Int, Int>? {
        val cleaned = raw?.trim()
            ?.removePrefix("v")
            ?.substringBefore('-')
            ?.substringBefore('+')
        if (cleaned.isNullOrEmpty()) return null

        val parts = cleaned.split('.')
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
        return Triple(
            major,
            parts.getOrNull(1)?.toIntOrNull() ?: 0,
            parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    }

    /**
     * True when locally cached IDs synced against [storedVersion] must be
     * discarded because [currentVersion] introduces the canonical ID format.
     *
     * - Server still older than [CANONICAL_IDS_VERSION]: nothing to migrate.
     * - Stored version unknown or unparseable: conservative reset whenever
     *   local data exists, since there is no way to tell whether that data
     *   predates the migration.
     * - Stored version known: reset only when it crosses the boundary.
     */
    fun requiresIdMigrationReset(
        storedVersion: String?,
        currentVersion: String?,
        hasLocalData: Boolean
    ): Boolean {
        val current = parse(currentVersion) ?: return false
        val threshold = parse(CANONICAL_IDS_VERSION) ?: return false
        if (compare(current, threshold) < 0) return false

        val stored = parse(storedVersion) ?: return hasLocalData
        return compare(stored, threshold) < 0
    }

    private fun compare(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Int {
        if (a.first != b.first) return a.first - b.first
        if (a.second != b.second) return a.second - b.second
        return a.third - b.third
    }
}
