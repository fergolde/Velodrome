package com.fergolde.velodrome.presentation.screen.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.imageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val PREFETCH_WINDOW = 4

fun prefetchIndices(firstVisible: Int, lastVisible: Int, itemCount: Int): IntRange {
    if (itemCount == 0 || firstVisible > lastVisible) return IntRange.EMPTY
    val first = (lastVisible + 1).coerceAtMost(itemCount)
    val last = (lastVisible + PREFETCH_WINDOW).coerceAtMost(itemCount - 1)
    return if (first <= last) first..last else IntRange.EMPTY
}

@Composable
fun ArtworkPrefetcher(
    state: LazyListState,
    itemCount: Int,
    artworkIdAt: (Int) -> String?,
    size: Dp,
) {
    ArtworkPrefetchEffect(
        itemCount = itemCount,
        artworkIdAt = artworkIdAt,
        size = size,
        visibleRange = { state.layoutInfo.visibleItemsInfo.firstOrNull()?.index?.let { first ->
            state.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { last -> first..last }
        } },
    )
}

@Composable
fun ArtworkPrefetcher(
    state: LazyGridState,
    itemCount: Int,
    artworkIdAt: (Int) -> String?,
    size: Dp,
) {
    ArtworkPrefetchEffect(
        itemCount = itemCount,
        artworkIdAt = artworkIdAt,
        size = size,
        visibleRange = { state.layoutInfo.visibleItemsInfo.firstOrNull()?.index?.let { first ->
            state.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { last -> first..last }
        } },
    )
}

@Composable
private fun ArtworkPrefetchEffect(
    itemCount: Int,
    artworkIdAt: (Int) -> String?,
    size: Dp,
    visibleRange: () -> IntRange?,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val imageLoader = context.imageLoader

    LaunchedEffect(imageLoader, itemCount, size, density) {
        val jobs = mutableMapOf<String, Job>()
        val canonicalSize = canonicalArtworkSize(size)
        snapshotFlow { visibleRange() }
            .distinctUntilChanged()
            .collect { range ->
                val indices = range?.let { prefetchIndices(it.first, it.last, itemCount) }
                    ?: IntRange.EMPTY
                val requests = indices.mapNotNull { index ->
                    artworkIdAt(index)?.takeUnless(String::isBlank)?.let { id ->
                        id to artworkImageRequest(context, id, size, density)
                    }
                }
                val requestedKeys = requests.mapTo(hashSetOf()) { (id, request) ->
                    "$id:${canonicalSize.name}"
                }

                jobs.keys.toList()
                    .filterNot(requestedKeys::contains)
                    .forEach { key -> jobs.remove(key)?.cancel() }

                requests.forEach { (id, request) ->
                    val key = "$id:${canonicalSize.name}"
                    if (key !in jobs) {
                        jobs[key] = launch {
                            try {
                                imageLoader.execute(request)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                // Visible cells retry through AsyncImage if prefetch fails.
                            }
                        }
                    }
                }
            }
    }
}
