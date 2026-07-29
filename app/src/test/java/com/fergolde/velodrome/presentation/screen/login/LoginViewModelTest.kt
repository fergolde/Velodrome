package com.fergolde.velodrome.presentation.screen.login

import com.fergolde.velodrome.domain.model.AuthResult
import com.fergolde.velodrome.domain.usecase.LoginUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class LoginViewModelTest {

    private val loginUseCase: LoginUseCase = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(loginUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isDefault() {
        val state = viewModel.uiState.value
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertEquals("", state.serverUrl)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isLoggedIn)
    }

    @Test
    fun onUsernameChange_updatesState() {
        viewModel.onUsernameChange("testuser")
        assertEquals("testuser", viewModel.uiState.value.username)
    }

    @Test
    fun onPasswordChange_updatesState() {
        viewModel.onPasswordChange("testpass")
        assertEquals("testpass", viewModel.uiState.value.password)
    }

    @Test
    fun onServerUrlChange_updatesState() {
        viewModel.onServerUrlChange("https://server.com")
        assertEquals("https://server.com", viewModel.uiState.value.serverUrl)
    }

    @Test
    fun onUsernameChange_clearsError() {
        viewModel.onUsernameChange("user")
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun login_blankUsername_showsError() {
        viewModel.onPasswordChange("pass")
        viewModel.onServerUrlChange("https://server.com")
        viewModel.login()

        assertEquals("Please enter username and password", viewModel.uiState.value.error)
        coVerify(exactly = 0) { loginUseCase(any(), any(), any()) }
    }

    @Test
    fun login_blankPassword_showsError() {
        viewModel.onUsernameChange("user")
        viewModel.onServerUrlChange("https://server.com")
        viewModel.login()

        assertEquals("Please enter username and password", viewModel.uiState.value.error)
        coVerify(exactly = 0) { loginUseCase(any(), any(), any()) }
    }

    @Test
    fun login_blankServerUrl_showsError() {
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.login()

        assertEquals("Please enter server URL", viewModel.uiState.value.error)
        coVerify(exactly = 0) { loginUseCase(any(), any(), any()) }
    }

    @Test
    fun login_success_setsIsLoggedIn() = runTest {
        coEvery { loginUseCase("user", "pass", "https://server.com") } returns
                Result.success(AuthResult(success = true))

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.onServerUrlChange("https://server.com")
        viewModel.login()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun login_failure_showsError() = runTest {
        coEvery { loginUseCase("user", "pass", "https://server.com") } returns
                Result.success(AuthResult(success = false, error = "Invalid credentials"))

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.onServerUrlChange("https://server.com")
        viewModel.login()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoggedIn)
        assertEquals("Invalid credentials", viewModel.uiState.value.error)
    }

    @Test
    fun login_networkException_showsError() = runTest {
        coEvery { loginUseCase("user", "pass", "https://server.com") } returns
                Result.failure(Exception("Connection refused"))

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.onServerUrlChange("https://server.com")
        viewModel.login()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoggedIn)
        assertEquals("Connection refused", viewModel.uiState.value.error)
    }

    @Test
    fun login_failureWithNullError_showsDefaultMessage() = runTest {
        coEvery { loginUseCase("user", "pass", "https://server.com") } returns
                Result.success(AuthResult(success = false, error = null))

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.onServerUrlChange("https://server.com")
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Login failed", viewModel.uiState.value.error)
    }

    @Test
    fun login_networkExceptionWithNullMessage_showsDefault() = runTest {
        coEvery { loginUseCase("user", "pass", "https://server.com") } returns
                Result.failure(Exception())

        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.onServerUrlChange("https://server.com")
        viewModel.login()
        advanceUntilIdle()

        assertEquals("Unknown error", viewModel.uiState.value.error)
    }
}
