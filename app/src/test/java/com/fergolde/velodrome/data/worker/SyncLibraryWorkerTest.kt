package com.fergolde.velodrome.data.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.fergolde.velodrome.data.local.datasource.LocalMusicDataSource
import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.data.remote.dto.SubsonicResponse
import com.fergolde.velodrome.data.remote.dto.SubsonicResponseDto
import com.fergolde.velodrome.domain.repository.AlbumRepository
import com.fergolde.velodrome.domain.repository.ArtistRepository
import com.fergolde.velodrome.domain.repository.ServerMigrationRepository
import com.fergolde.velodrome.domain.repository.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class SyncLibraryWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val inputData: Data = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val artistRepository: ArtistRepository = mockk(relaxed = true)
    private val albumRepository: AlbumRepository = mockk(relaxed = true)
    private val localMusicDataSource: LocalMusicDataSource = mockk(relaxed = true)
    private val api: NavidromeApi = mockk()
    private val serverMigrationRepository: ServerMigrationRepository = mockk(relaxed = true)

    init {
        every { params.inputData } returns inputData
        every { inputData.getBoolean(any(), any()) } returns false
    }

    private fun createWorker() = SyncLibraryWorker(
        context = context,
        params = params,
        settingsRepository = settingsRepository,
        artistRepository = artistRepository,
        albumRepository = albumRepository,
        localMusicDataSource = localMusicDataSource,
        api = api,
        serverMigrationRepository = serverMigrationRepository
    )

    @Test
    fun `probes server version before the incremental throttle`() = runTest {
        val now = System.currentTimeMillis()
        every { settingsRepository.lastSyncTimestamp } returns flowOf(now)
        every { settingsRepository.lastSyncOffset } returns flowOf(0)
        every { settingsRepository.lastServerCheckAt } returns flowOf(now)
        coEvery { api.ping() } returns SubsonicResponse(
            SubsonicResponseDto(status = "ok", serverVersion = "0.64.0")
        )
        coEvery { serverMigrationRepository.checkAndMigrate("0.64.0") } returns false

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { api.ping() }
        coVerify { serverMigrationRepository.checkAndMigrate("0.64.0") }
    }

    @Test
    fun `runs full sync after a migration reset`() = runTest {
        every { settingsRepository.lastSyncTimestamp } returns flowOf(0L)
        every { settingsRepository.lastSyncOffset } returns flowOf(0)
        every { settingsRepository.lastServerCheckAt } returns flowOf(0L)
        coEvery { api.ping() } returns SubsonicResponse(
            SubsonicResponseDto(status = "ok", serverVersion = "0.64.0")
        )
        coEvery { serverMigrationRepository.checkAndMigrate("0.64.0") } returns true
        coEvery { artistRepository.syncArtistsFromServer() } returns Result.success(3)
        coEvery {
            albumRepository.syncAlbumsFromServer(startOffset = 0, onPageProcessed = any())
        } returns Result.success(4)

        val result = createWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { artistRepository.syncArtistsFromServer() }
        coVerify { albumRepository.syncAlbumsFromServer(startOffset = 0, onPageProcessed = any()) }
    }

    @Test
    fun `ping failure never wipes local data`() = runTest {
        every { settingsRepository.lastSyncTimestamp } returns flowOf(0L)
        every { settingsRepository.lastSyncOffset } returns flowOf(0)
        every { settingsRepository.lastServerCheckAt } returns flowOf(0L)
        coEvery { api.ping() } throws IOException("offline")
        coEvery { artistRepository.syncArtistsFromServer() } returns Result.success(0)
        coEvery {
            albumRepository.syncAlbumsFromServer(startOffset = 0, onPageProcessed = any())
        } returns Result.success(0)

        createWorker().doWork()

        coVerify { serverMigrationRepository.checkAndMigrate(null) }
    }
}
