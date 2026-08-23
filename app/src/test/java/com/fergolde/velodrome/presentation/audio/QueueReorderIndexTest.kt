package com.fergolde.velodrome.presentation.audio

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure queue-reorder math: no MediaController involved, safe on JVM.
 */
class QueueReorderIndexTest {

    @Test
    fun `moving the current item moves its index with it`() {
        assertEquals(5, adjustedCurrentIndex(current = 3, from = 3, to = 5))
        assertEquals(1, adjustedCurrentIndex(current = 3, from = 3, to = 1))
    }

    @Test
    fun `item moved from above across current shifts current down`() {
        // 0:[A] 1:[B] 2:[C] 3:[CUR] — B(1) -> 4 lands after CUR
        assertEquals(2, adjustedCurrentIndex(current = 3, from = 1, to = 4))
        // exact landing on current slot also displaces it
        assertEquals(2, adjustedCurrentIndex(current = 3, from = 1, to = 3))
    }

    @Test
    fun `item moved from below across current shifts current up`() {
        // 3:[CUR] 4:[E] 5:[F] — F(5) -> 1 lands before CUR
        assertEquals(4, adjustedCurrentIndex(current = 3, from = 5, to = 1))
        // exact landing on current slot displaces it too
        assertEquals(4, adjustedCurrentIndex(current = 3, from = 5, to = 3))
    }

    @Test
    fun `moves not crossing current leave it untouched`() {
        assertEquals(3, adjustedCurrentIndex(current = 3, from = 0, to = 2))
        assertEquals(3, adjustedCurrentIndex(current = 3, from = 4, to = 6))
        assertEquals(3, adjustedCurrentIndex(current = 3, from = 6, to = 4))
    }

    @Test
    fun `no-op move returns same index`() {
        assertEquals(2, adjustedCurrentIndex(current = 2, from = 2, to = 2))
    }
}
