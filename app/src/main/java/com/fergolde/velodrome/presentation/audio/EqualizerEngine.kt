package com.fergolde.velodrome.presentation.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log

/** Genre keyword buckets -> preferred system preset name. */
private val ROCK = setOf("rock", "metal", "punk", "grunge", "alternative", "hard rock", "indie")
private val POP = setOf("pop", "top 40", "charts")
private val JAZZ = setOf("jazz", "blues", "bossa", "swing", "soul", "funk")
private val CLASSICAL = setOf("classical", "clásica", "opera", "ópera", "symphony", "sinfónica", "chamber", "piano")
private val DANCE = setOf("dance", "electronic", "electrónica", "techno", "house", "edm", "hip hop", "hip-hop", "rap", "reggaeton", "reggaetón", "trap")
private val FOLK = setOf("folk", "country", "acoustic", "acústico", "singer-songwriter", "world")

/**
 * Maps a song genre to the preferred system EQ preset name.
 * Unknown/blank genres fall back to "Normal".
 * Pure function so the mapping is unit-testable without audio hardware.
 */
internal fun presetNameForGenre(genre: String?): String {
    val g = genre?.trim()?.lowercase() ?: return "Normal"
    return when {
        g.isEmpty() -> "Normal"
        ROCK.any { g.contains(it) } -> "Rock"
        DANCE.any { g.contains(it) } -> "Dance"
        JAZZ.any { g.contains(it) } -> "Jazz"
        CLASSICAL.any { g.contains(it) } -> "Classical"
        POP.any { g.contains(it) } -> "Pop"
        FOLK.any { g.contains(it) } -> "Folk"
        else -> "Normal"
    }
}

/**
 * Wraps Android's session audio effects around ExoPlayer's audio session.
 * Everything is best-effort: devices without EQ support degrade silently
 * (isAvailable == false, all calls become no-ops).
 */
class EqualizerEngine(audioSessionId: Int) {

    private val equalizer: Equalizer? = runCatching { Equalizer(0, audioSessionId) }
        .onFailure { Log.w(TAG, "Device has no equalizer support", it) }
        .getOrNull()

    private val bassBoost: BassBoost? = runCatching { BassBoost(0, audioSessionId) }.getOrNull()

    val isAvailable: Boolean = equalizer != null

    fun setEnabled(enabled: Boolean) {
        val eq = equalizer ?: return
        runCatching { eq.enabled = enabled }
    }

    /** Applies the system preset matching [genre]; keeps current curve if none matches. */
    fun applyGenrePreset(genre: String?) {
        val eq = equalizer ?: return
        runCatching {
            val names = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
            val wanted = presetNameForGenre(genre)
            val idx = names.indexOfFirst { it.equals(wanted, ignoreCase = true) }
                // Fallback: any vendor "Normal" preset when the bucket is missing
                .takeIf { it >= 0 }
                ?: names.indexOfFirst { it.equals("Normal", ignoreCase = true) }
                    .takeIf { it >= 0 }
                ?: return
            eq.usePreset(idx.toShort())
        }.onFailure { Log.w(TAG, "applyGenrePreset failed", it) }
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        val bb = bassBoost ?: return
        runCatching {
            bb.enabled = enabled
            bb.setStrength(if (enabled) BASS_STRENGTH else 0.toShort())
        }.onFailure { Log.w(TAG, "setBassBoostEnabled failed", it) }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
    }

    private companion object {
        const val TAG = "EqualizerEngine"
        const val BASS_STRENGTH: Short = 500 // moderate, out of 1000
    }
}
