package com.fergolde.velodrome.presentation.player

import com.fergolde.velodrome.domain.model.Track
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val playerManager: PlayerManager = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PlayerViewModel

    private val emptyPlaylist = MutableStateFlow<List<Track>>(emptyList())
    private val isPlayingFlow = MutableStateFlow(false)
    private val currentPositionFlow = MutableStateFlow(0L)
    private val currentTrackFlow = MutableStateFlow<Track?>(null)
    private val currentIndexFlow = MutableStateFlow(0)
    private val isShuffleFlow = MutableStateFlow(false)
    private val isRepeatFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { playerManager.playlist } returns emptyPlaylist
        every { playerManager.isPlaying } returns isPlayingFlow
        every { playerManager.currentPosition } returns currentPositionFlow
        every { playerManager.currentTrack } returns currentTrackFlow
        every { playerManager.currentIndex } returns currentIndexFlow
        every { playerManager.isShuffleEnabled } returns isShuffleFlow
        every { playerManager.isRepeatEnabled } returns isRepeatFlow
        viewModel = PlayerViewModel(playerManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isDefault() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.playlist.isEmpty())
        assertFalse(state.isPlaying)
        assertEquals(0, state.currentPosition)
        assertNull(state.currentTrack)
        assertEquals(0, state.currentIndex)
        assertFalse(state.isShuffleEnabled)
        assertFalse(state.isRepeatEnabled)
    }

    @Test
    fun onPlayPauseClick_delegates() {
        viewModel.onPlayPauseClick()
        verify { playerManager.togglePlayPause() }
    }

    @Test
    fun onPreviousClick_delegates() {
        viewModel.onPreviousClick()
        verify { playerManager.previous() }
    }

    @Test
    fun onNextClick_delegates() {
        viewModel.onNextClick()
        verify { playerManager.next() }
    }

    @Test
    fun onTrackSelected_delegates() {
        viewModel.onTrackSelected(3)
        verify { playerManager.setCurrentIndex(3) }
    }

    @Test
    fun onRemoveTrack_delegates() {
        viewModel.onRemoveTrack(2)
        verify { playerManager.removeFromPlaylist(2) }
    }

    @Test
    fun onSeek_convertsSecondsToMillis() {
        viewModel.onSeek(60)
        verify { playerManager.seekTo(60000L) }
    }

    @Test
    fun onSeek_zeroSeconds() {
        viewModel.onSeek(0)
        verify { playerManager.seekTo(0L) }
    }

    @Test
    fun toggleShuffle_delegates() {
        viewModel.toggleShuffle()
        verify { playerManager.toggleShuffle() }
    }

    @Test
    fun toggleRepeat_delegates() {
        viewModel.toggleRepeat()
        verify { playerManager.toggleRepeat() }
    }
}
