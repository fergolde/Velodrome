package com.fergolde.velodrome.data.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.fergolde.velodrome.data.local.dao.ScrobbleDao
import com.fergolde.velodrome.data.local.entity.ScrobbleEntity
import com.fergolde.velodrome.domain.repository.ScrobbleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ScrobbleWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val scrobbleDao: ScrobbleDao = mockk(relaxed = true)
    private val scrobbleRepository: ScrobbleRepository = mockk()

    private fun createWorker() = ScrobbleWorker(context, params, scrobbleDao, scrobbleRepository)

    @Test
    fun `deletes chunk only when server accepts`() = runTest {
        val pending = listOf(
            ScrobbleEntity(id = 1, trackId = "t1", timestamp = 1000L, isSubmitted = false),
            ScrobbleEntity(id = 2, trackId = "t2", timestamp = 2000L, isSubmitted = false)
        )
        coEvery { scrobbleDao.getPendingScrobbles() } returns pending
        coEvery { scrobbleRepository.scrobbleBatch(any(), any(), any()) } returns Result.success(Unit)

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { scrobbleDao.deleteScrobbles(listOf(1L, 2L)) }
    }

    @Test
    fun `does not delete when server rejects`() = runTest {
        val pending = listOf(
            ScrobbleEntity(id = 1, trackId = "t1", timestamp = 1000L, isSubmitted = false)
        )
        coEvery { scrobbleDao.getPendingScrobbles() } returns pending
        coEvery { scrobbleRepository.scrobbleBatch(any(), any(), any()) } returns Result.failure(RuntimeException("Subsonic rejected"))

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify(exactly = 0) { scrobbleDao.deleteScrobbles(any()) }
    }

    @Test
    fun `mixed batch deletes only accepted chunks`() = runTest {
        // BATCH_SIZE is 50; use 51 items to force two chunks.
        val firstChunk = (1..50).map { index ->
            ScrobbleEntity(id = index.toLong(), trackId = "t$index", timestamp = index * 1000L, isSubmitted = false)
        }
        val secondChunk = listOf(
            ScrobbleEntity(id = 51L, trackId = "t51", timestamp = 51000L, isSubmitted = false)
        )
        coEvery { scrobbleDao.getPendingScrobbles() } returns firstChunk + secondChunk
        coEvery { scrobbleRepository.scrobbleBatch(ids = firstChunk.map { it.trackId }, times = firstChunk.map { it.timestamp }, submission = true) } returns Result.success(Unit)
        coEvery { scrobbleRepository.scrobbleBatch(ids = secondChunk.map { it.trackId }, times = secondChunk.map { it.timestamp }, submission = true) } returns Result.failure(RuntimeException("Subsonic rejected"))

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify { scrobbleDao.deleteScrobbles(firstChunk.map { it.id }) }
        coVerify(exactly = 0) { scrobbleDao.deleteScrobbles(secondChunk.map { it.id }) }
    }
}
