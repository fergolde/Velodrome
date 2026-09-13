package com.fergolde.velodrome.util

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ServerDataResetNotifierTest {

    @Test
    fun `epoch starts at zero and increments per reset`() = runTest {
        val notifier = ServerDataResetNotifier()

        assertEquals(0L, notifier.epoch.value)

        notifier.notifyReset()
        assertEquals(1L, notifier.epoch.value)

        notifier.notifyReset()
        assertEquals(2L, notifier.epoch.value)
    }
}
