package com.fergolde.velodrome.util

import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import org.junit.Test

class ConfigurableLruCacheEvictorTest {

    @Test
    fun loweringLimitEvictsOldestSpanImmediately() {
        val cache = mockk<Cache>()
        val evictor = ConfigurableLruCacheEvictor(initialMaxBytes = 100L)
        val span = CacheSpan("track", 0L, 100L, 1L, File("track.cache"))
        every { cache.removeSpan(span) } answers { evictor.onSpanRemoved(cache, span) }

        evictor.onSpanAdded(cache, span)
        evictor.setMaxBytes(cache, 50L)

        verify(exactly = 1) { cache.removeSpan(span) }
    }
}
