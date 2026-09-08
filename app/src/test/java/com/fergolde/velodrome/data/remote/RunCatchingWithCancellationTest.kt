package com.fergolde.velodrome.data.remote

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunCatchingWithCancellationTest {

    @Test(expected = CancellationException::class)
    fun `rethrows CancellationException`() {
        runCatchingWithCancellation {
            throw CancellationException("cancelled")
        }
    }

    @Test
    fun `returns failure for other exceptions`() {
        val result = runCatchingWithCancellation {
            throw RuntimeException("boom")
        }
        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun `returns success for normal value`() {
        val result = runCatchingWithCancellation { 42 }
        assertEquals(42, result.getOrNull())
    }
}
