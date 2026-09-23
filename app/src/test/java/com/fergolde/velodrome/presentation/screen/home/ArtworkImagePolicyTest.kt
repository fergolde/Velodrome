package com.fergolde.velodrome.presentation.screen.home

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkImagePolicyTest {

    @Test
    fun canonicalArtworkSizeKeepsSmallSurfacesTogether() {
        assertEquals(ArtworkSize.Thumbnail, canonicalArtworkSize(46.dp))
        assertEquals(ArtworkSize.Avatar, canonicalArtworkSize(80.dp))
        assertEquals(ArtworkSize.Card, canonicalArtworkSize(130.dp))
    }

    @Test
    fun canonicalArtworkSizePreservesGridAndDetailQuality() {
        assertEquals(ArtworkSize.Grid, canonicalArtworkSize(200.dp))
        assertEquals(ArtworkSize.Detail, canonicalArtworkSize(0.dp))
        assertEquals(ArtworkSize.Detail, canonicalArtworkSize(400.dp))
    }

    @Test
    fun prefetchIndicesSelectsOnlyBoundedItemsAfterViewport() {
        assertEquals(10..13, prefetchIndices(firstVisible = 4, lastVisible = 9, itemCount = 20))
        assertEquals(19..19, prefetchIndices(firstVisible = 16, lastVisible = 18, itemCount = 20))
    }

    @Test
    fun prefetchIndicesDoesNotReadPastEndOrCreateWorkForEmptyList() {
        assertEquals(IntRange.EMPTY, prefetchIndices(0, 3, itemCount = 4))
        assertEquals(IntRange.EMPTY, prefetchIndices(0, -1, itemCount = 10))
        assertEquals(IntRange.EMPTY, prefetchIndices(0, 3, itemCount = 0))
    }
}
