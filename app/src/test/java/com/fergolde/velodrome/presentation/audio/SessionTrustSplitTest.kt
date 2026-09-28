package com.fergolde.velodrome.presentation.audio

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the trust split applied in [AudioPlayerService.sessionCallback].
 *
 * Watch companions (Garmin Connect) connect as UNTRUSTED controllers: they are
 * neither the system nor hold MEDIA_CONTENT_CONTROL. Granting them the full
 * player command set would let any installed app read the queue and push
 * arbitrary MediaItems into the player, which then egress through the shared
 * OkHttp client.
 */
class SessionTrustSplitTest {

    private val allowed = untrustedTransportCommands().toSet()

    @Test
    fun `untrusted controller can still drive transport`() {
        // The reason this callback exists at all: without transport commands a
        // Garmin watch is read-only and the app goes mute.
        assertTrue("play/pause is the whole point", Player.COMMAND_PLAY_PAUSE in allowed)
        assertTrue("prepare", Player.COMMAND_PREPARE in allowed)
        assertTrue("stop", Player.COMMAND_STOP in allowed)
        assertTrue("seek to next", Player.COMMAND_SEEK_TO_NEXT in allowed)
        assertTrue("seek to previous", Player.COMMAND_SEEK_TO_PREVIOUS in allowed)
    }

    @Test
    fun `untrusted controller cannot inject media items`() {
        assertFalse(
            "An untrusted app must not be able to set an arbitrary MediaItem",
            Player.COMMAND_SET_MEDIA_ITEM in allowed
        )
        assertFalse(
            "An untrusted app must not be able to change the queue",
            Player.COMMAND_CHANGE_MEDIA_ITEMS in allowed
        )
    }

    @Test
    fun `untrusted controller cannot reshape playback mode`() {
        assertFalse(Player.COMMAND_SET_SHUFFLE_MODE in allowed)
        assertFalse(Player.COMMAND_SET_REPEAT_MODE in allowed)
        assertFalse(Player.COMMAND_SET_SPEED_AND_PITCH in allowed)
    }

    // NOTE: the assembled `Player.Commands` are not asserted here on purpose.
    // `Player.Commands.Builder` pulls in Android framework classes and throws
    // ExceptionInInitializerError in a plain JVM test, so the allowlist is
    // verified as raw command ints instead. Read access is intentional — the
    // watch must see what is playing — and the actual credential leak was in
    // the MediaItem URI, which no longer carries auth params.

    @Test
    fun `transport set contains no duplicates`() {
        val commands = untrustedTransportCommands()
        assertTrue("duplicated command in the allowlist", commands.size == commands.toSet().size)
    }
}
