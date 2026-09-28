package com.fergolde.velodrome.presentation.screen.settings

import com.fergolde.velodrome.domain.repository.AccountDataRepository
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.util.CacheManager
import com.fergolde.velodrome.util.CredentialsManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Guards the sign-out flow.
 *
 * logout() is the one place where a failure must not leave the user stranded:
 * it is the only way to drop the credentials, and it now runs an account purge
 * (Room + DataStore writes) before clearing them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val cacheManager: CacheManager = mockk(relaxed = true)
    private val credentialsManager: CredentialsManager = mockk(relaxed = true)
    private val accountDataRepository: AccountDataRepository = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { settingsRepository.imageCacheSizeMb } returns flowOf(200)
        every { settingsRepository.musicCacheSizeGb } returns flowOf(2)
        every { settingsRepository.accentColor } returns flowOf("#B6A0FF")
        every { settingsRepository.scrobbleEnabled } returns flowOf(false)
        every { settingsRepository.eqEnabled } returns flowOf(false)
        every { settingsRepository.bassBoostEnabled } returns flowOf(false)
        viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            cacheManager = cacheManager,
            credentialsManager = credentialsManager,
            accountDataRepository = accountDataRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `logout purges the account and then signs out`() = runTest(testDispatcher) {
        subscribe()
        viewModel.logout()
        advanceUntilIdle()

        coVerify { accountDataRepository.purgeAccountData() }
        verify { credentialsManager.clearCredentials() }
        assertTrue(viewModel.uiState.value.shouldLogout)
    }

    @Test
    fun `logout still signs out when the purge fails`() = runTest(testDispatcher) {
        // The purge writes to Room, the queue DataStore and four settings keys.
        // Any of those can throw. If that escaped, the coroutine would die
        // before clearing the credentials and the user would be stuck on the
        // settings screen with no way to sign out.
        coEvery { accountDataRepository.purgeAccountData() } throws IOException("datastore corrupted")
        subscribe()

        viewModel.logout()
        advanceUntilIdle()

        verify { credentialsManager.clearCredentials() }
        assertTrue(
            "purge failure must not block the sign-out",
            viewModel.uiState.value.shouldLogout
        )
    }

    @Test
    fun `logout does not wipe the audio cache`() = runTest(testDispatcher) {
        // Cache keys are account-scoped, so the next account cannot read the
        // previous account's media. Wiping it would throw away the signed-in
        // user's downloads for no security gain.
        subscribe()
        viewModel.logout()
        advanceUntilIdle()

        coVerify(exactly = 0) { cacheManager.clearAllCaches() }
        coVerify(exactly = 0) { cacheManager.clearMusicCache() }
        coVerify(exactly = 0) { cacheManager.clearImageCache() }
    }

    @Test
    fun `logoutHandled clears the navigation signal`() = runTest(testDispatcher) {
        subscribe()
        viewModel.logout()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.shouldLogout)

        viewModel.logoutHandled()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.shouldLogout)
    }

    /**
     * `uiState` is a `stateIn(WhileSubscribed)` flow, so `.value` is the
     * initial state until something collects it. Subscribe eagerly for the
     * duration of the test.
     */
    private fun TestScope.subscribe(): Job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
}
