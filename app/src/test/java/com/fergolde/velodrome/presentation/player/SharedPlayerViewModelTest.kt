package com.fergolde.velodrome.presentation.player

import com.fergolde.velodrome.domain.model.Track
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedPlayerViewModelTest {

    private val playerManager: PlayerManager = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SharedPlayerViewModel

    private val currentTrackFlow = MutableStateFlow<Track?>(null)
    private val isPlayingFlow = MutableStateFlow(false)
    private val currentPositionFlow = MutableStateFlow(0L)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { playerManager.currentTrack } returns currentTrackFlow
        every { playerManager.isPlaying } returns isPlayingFlow
        every { playerManager.currentPosition } returns currentPositionFlow
        viewModel = SharedPlayerViewModel(playerManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isDefault() {
        assertNull(viewModel.currentTrack.value)
        assertFalse(viewModel.isPlaying.value)
        assertEquals(0L, viewModel.currentPosition.value)
    }

    @Test
    fun togglePlayPause_delegates() {
        viewModel.togglePlayPause()
        verify { playerManager.togglePlayPause() }
    }

    @Test
    fun next_delegates() {
        viewModel.next()
        verify { playerManager.next() }
    }

    @Test
    fun previous_delegates() {
        viewModel.previous()
        verify { playerManager.previous() }
    }

    @Test
    fun currentTrack_updatesFromManager() {
        val track = Track(id = "t1", albumId = "a1", title = "Song", durationSec = 180, sizeBytes = 5000000, bitrate = 320, trackNumber = 1)
        currentTrackFlow.value = track
        assertEquals(track, viewModel.currentTrack.value)
    }

    @Test
    fun isPlaying_updatesFromManager() {
        isPlayingFlow.value = true
        assertTrue(viewModel.isPlaying.value)
    }
}
