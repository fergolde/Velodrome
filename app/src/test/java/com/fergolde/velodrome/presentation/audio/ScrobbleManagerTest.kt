package com.fergolde.velodrome.presentation.audio

import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.ExistingWorkPolicy
import androidx.work.impl.WorkManagerImpl
import com.fergolde.velodrome.domain.repository.ScrobbleRepository
import com.fergolde.velodrome.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ScrobbleManagerTest {

    private val scrobbleRepository: ScrobbleRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk()
    private val context: Context = mockk()
    private val workManager: WorkManagerImpl = mockk(relaxed = true)

    private lateinit var manager: ScrobbleManager

    @Before
    fun setup() {
        mockkStatic(WorkManagerImpl::class)
        every { WorkManagerImpl.getInstance(any()) } returns workManager
        every { settingsRepository.scrobbleEnabled } returns flowOf(true)
        manager = ScrobbleManager(scrobbleRepository, settingsRepository, context)
    }

    @Test
    fun `markTrackPlayed saves pending scrobble and enqueues work`() = runTest {
        manager.markTrackPlayed("t1")

        coVerify(timeout = 2000) { scrobbleRepository.savePendingScrobble("t1", any()) }
        verify(timeout = 2000) {
            workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun `markTrackPlayed is deduplicated for the same track`() = runTest {
        manager.markTrackPlayed("t1")
        manager.markTrackPlayed("t1")

        coVerify(exactly = 1, timeout = 2000) { scrobbleRepository.savePendingScrobble("t1", any()) }
    }

    @Test
    fun `onTrackChanged resets dedup so track can be marked again`() = runTest {
        manager.markTrackPlayed("t1")
        manager.onTrackChanged()
        manager.markTrackPlayed("t1")

        coVerify(exactly = 2, timeout = 2000) { scrobbleRepository.savePendingScrobble("t1", any()) }
    }

    @Test
    fun `markTrackPlayed does nothing when scrobbling disabled`() = runTest {
        every { settingsRepository.scrobbleEnabled } returns flowOf(false)

        manager.markTrackPlayed("t1")

        coVerify(timeout = 2000, exactly = 0) { scrobbleRepository.savePendingScrobble(any(), any()) }
    }
}
