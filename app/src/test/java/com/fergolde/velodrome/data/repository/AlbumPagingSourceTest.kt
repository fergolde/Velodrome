package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.datasource.LocalMusicDataSource
import com.fergolde.velodrome.data.local.entity.AlbumEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumPagingSourceTest {

    private val localDataSource: LocalMusicDataSource = mockk()
    private val pagingSource = AlbumPagingSource(localDataSource)

    private fun albumEntity(id: String) = AlbumEntity(
        id = id,
        artistId = "art1",
        artistName = "Artist",
        title = "Album $id",
        year = 2020,
        genre = null,
        coverUrl = null
    )

    @Test
    fun `refresh returns first page with absolute offset keys`() = runTest {
        coEvery { localDataSource.getAlbumsPage(offset = 0, limit = 20) } returns (1..20).map { albumEntity("a$it") }

        val result = pagingSource.load(
            androidx.paging.PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        )

        val page = result as androidx.paging.PagingSource.LoadResult.Page
        assertEquals(20, page.data.size)
        assertNull(page.prevKey)
        assertEquals(20, page.nextKey)
    }

    @Test
    fun `append returns next absolute offset and stops at partial page`() = runTest {
        coEvery { localDataSource.getAlbumsPage(offset = 20, limit = 20) } returns (21..35).map { albumEntity("a$it") }

        val result = pagingSource.load(
            androidx.paging.PagingSource.LoadParams.Append(key = 20, loadSize = 20, placeholdersEnabled = false)
        )

        val page = result as androidx.paging.PagingSource.LoadResult.Page
        assertEquals(15, page.data.size)
        assertEquals(0, page.prevKey)
        assertNull(page.nextKey)
    }
}
