package com.fergolde.velodrome.util

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import java.util.TreeSet

/** LRU evictor whose limit can change without recreating SimpleCache. */
@UnstableApi
class ConfigurableLruCacheEvictor(initialMaxBytes: Long) : CacheEvictor {

    private val leastRecentlyUsed = TreeSet<CacheSpan>(::compare)
    private var currentSize = 0L
    private var maxBytes = initialMaxBytes.coerceAtLeast(0L)

    @Synchronized
    fun setMaxBytes(cache: Cache, maxBytes: Long) {
        this.maxBytes = maxBytes.coerceAtLeast(0L)
        evictCache(cache, 0L)
    }

    /** Current configured limit; Long.MAX_VALUE until reconciled with settings. */
    @Synchronized
    fun peekMaxBytes(): Long = maxBytes

    @Synchronized
    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() = Unit

    @Synchronized
    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        if (length != C.LENGTH_UNSET.toLong()) {
            evictCache(cache, length)
        }
    }

    @Synchronized
    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.add(span)
        currentSize += span.length
        evictCache(cache, 0L)
    }

    @Synchronized
    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.remove(span)
        currentSize -= span.length
    }

    @Synchronized
    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    private fun evictCache(cache: Cache, requiredSpace: Long) {
        while (currentSize + requiredSpace > maxBytes && leastRecentlyUsed.isNotEmpty()) {
            cache.removeSpan(leastRecentlyUsed.first())
        }
    }

    private companion object {
        fun compare(left: CacheSpan, right: CacheSpan): Int {
            val timestampDelta = left.lastTouchTimestamp - right.lastTouchTimestamp
            return when {
                timestampDelta < 0L -> -1
                timestampDelta > 0L -> 1
                else -> left.compareTo(right)
            }
        }
    }
}
