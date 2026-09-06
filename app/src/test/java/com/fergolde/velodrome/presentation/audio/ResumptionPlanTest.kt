package com.fergolde.velodrome.presentation.audio

import com.fergolde.velodrome.data.local.queue.QueueSnapshot
import com.fergolde.velodrome.data.local.queue.TrackDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure resumption math: no Media3 involved, safe on JVM.
 */
class ResumptionPlanTest {

    private fun track(id: String) = TrackDto(
        id = id, title = "t-$id", artistName = "a", albumName = "b",
        albumId = "al", durationSec = 200, trackNumber = 1, coverArtId = null
    )

    @Test
    fun `null snapshot means nothing to resume`() {
        assertNull(computeResumptionPlan(null, playerHasItems = false))
    }

    @Test
    fun `empty snapshot means nothing to resume`() {
        assertNull(computeResumptionPlan(QueueSnapshot(), playerHasItems = false))
    }

    @Test
    fun `player with items wins over snapshot`() {
        val snapshot = QueueSnapshot(tracks = listOf(track("1")), currentIndex = 0, positionMs = 10)
        assertNull(computeResumptionPlan(snapshot, playerHasItems = true))
    }

    @Test
    fun `valid snapshot maps tracks index and position`() {
        val snapshot = QueueSnapshot(
            tracks = listOf(track("1"), track("2"), track("3")),
            currentIndex = 1,
            positionMs = 42_000
        )
        val plan = computeResumptionPlan(snapshot, playerHasItems = false)!!
        assertEquals(listOf("1", "2", "3"), plan.tracks.map { it.id })
        assertEquals(1, plan.startIndex)
        assertEquals(42_000, plan.startPositionMs)
    }

    @Test
    fun `out of range index and negative position get clamped`() {
        val snapshot = QueueSnapshot(
            tracks = listOf(track("1"), track("2")),
            currentIndex = 99,
            positionMs = -5
        )
        val plan = computeResumptionPlan(snapshot, playerHasItems = false)!!
        assertEquals(1, plan.startIndex)
        assertEquals(0, plan.startPositionMs)
    }
}
