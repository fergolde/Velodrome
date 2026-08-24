package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.local.dao.ScrobbleDao
import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.data.remote.dto.ErrorDto
import com.fergolde.velodrome.data.remote.dto.SubsonicResponse
import com.fergolde.velodrome.data.remote.dto.SubsonicResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrobbleRepositoryImplTest {

    private val api: NavidromeApi = mockk()
    private val dao: ScrobbleDao = mockk(relaxed = true)
    private val repository = ScrobbleRepositoryImpl(api, dao)

    private fun ok() = SubsonicResponse(
        SubsonicResponseDto(status = "ok")
    )

    @Test
    fun `scrobbleBatch sends parallel id and time lists`() = runTest {
        coEvery { api.scrobble(any(), any(), any()) } returns ok()

        val ids = listOf("t1", "t2", "t3")
        val times = listOf(1000L, 2000L, 3000L)
        val result = repository.scrobbleBatch(ids, times)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { api.scrobble(ids, times, submission = true) }
    }

    @Test
    fun `scrobble single delegates to one-element list without times when null`() = runTest {
        coEvery { api.scrobble(any(), any(), any()) } returns ok()

        val result = repository.scrobble("t1", time = null, submission = false)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { api.scrobble(listOf("t1"), null, submission = false) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `scrobbleBatch rejects mismatched lists`() = runTest {
        repository.scrobbleBatch(listOf("t1"), times = emptyList())
    }
}
