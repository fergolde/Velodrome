package com.fergolde.velodrome.domain.usecase

import android.content.Context
import android.content.pm.ApplicationInfo
import com.fergolde.velodrome.R
import com.fergolde.velodrome.domain.model.AuthResult
import com.fergolde.velodrome.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LoginUseCaseTest {

    private val repository: AuthRepository = mockk()

    private fun createContext(isDebuggable: Boolean = false): Context {
        return mockk {
            every { applicationInfo } returns ApplicationInfo().apply {
                flags = if (isDebuggable) ApplicationInfo.FLAG_DEBUGGABLE else 0
            }
            every { getString(R.string.error_cleartext_not_allowed) } returns "HTTPS required"
        }
    }

    @Test
    fun `blank username returns failure`() = runTest {
        val useCase = LoginUseCase(createContext(), repository)
        val result = useCase("", "pass", "http://server.com")
        assertTrue(result.isFailure)
        assertEquals("Username cannot be empty", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.login(any(), any(), any()) }
    }

    @Test
    fun `blank password returns failure`() = runTest {
        val useCase = LoginUseCase(createContext(), repository)
        val result = useCase("user", "", "http://server.com")
        assertTrue(result.isFailure)
        assertEquals("Password cannot be empty", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.login(any(), any(), any()) }
    }

    @Test
    fun `blank serverUrl returns failure`() = runTest {
        val useCase = LoginUseCase(createContext(), repository)
        val result = useCase("user", "pass", "")
        assertTrue(result.isFailure)
        assertEquals("Server URL cannot be empty", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.login(any(), any(), any()) }
    }

    @Test
    fun `adds https prefix when missing`() = runTest {
        val useCase = LoginUseCase(createContext(), repository)
        coEvery { repository.login("user", "pass", "https://server.com/") } returns
                Result.success(AuthResult(success = true))

        val result = useCase("user", "pass", "server.com")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `preserves http prefix in debug builds`() = runTest {
        val useCase = LoginUseCase(createContext(isDebuggable = true), repository)
        coEvery { repository.login("user", "pass", "http://server.com/") } returns
                Result.success(AuthResult(success = true))

        val result = useCase("user", "pass", "http://server.com")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `rejects http prefix in release builds`() = runTest {
        val useCase = LoginUseCase(createContext(isDebuggable = false), repository)
        val result = useCase("user", "pass", "http://server.com")
        assertTrue(result.isFailure)
        assertEquals("HTTPS required", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.login(any(), any(), any()) }
    }

    @Test
    fun `preserves https prefix`() = runTest {
        val useCase = LoginUseCase(createContext(), repository)
        coEvery { repository.login("user", "pass", "https://server.com/") } returns
                Result.success(AuthResult(success = true))

        val result = useCase("user", "pass", "https://server.com")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `adds trailing slash`() = runTest {
        val useCase = LoginUseCase(createContext(), repository)
        coEvery { repository.login("user", "pass", "https://server.com/") } returns
                Result.success(AuthResult(success = true))

        val result = useCase("user", "pass", "https://server.com")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `trims whitespace`() = runTest {
        val useCase = LoginUseCase(createContext(), repository)
        coEvery { repository.login("user", "pass", "https://server.com/") } returns
                Result.success(AuthResult(success = true))

        val result = useCase("user", "pass", "  https://server.com  ")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `successful login delegates to repository`() = runTest {
        val useCase = LoginUseCase(createContext(), repository)
        val authResult = AuthResult(success = true)
        coEvery { repository.login("user", "pass", "https://server.com/") } returns
                Result.success(authResult)

        val result = useCase("user", "pass", "https://server.com/")
        assertTrue(result.isSuccess)
        assertEquals(authResult, result.getOrNull())
        coVerify { repository.login("user", "pass", "https://server.com/") }
    }

    @Test
    fun `failed login propagates error`() = runTest {
        val useCase = LoginUseCase(createContext(), repository)
        coEvery { repository.login("user", "pass", "https://server.com/") } returns
                Result.failure(Exception("Network error"))

        val result = useCase("user", "pass", "https://server.com/")
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
