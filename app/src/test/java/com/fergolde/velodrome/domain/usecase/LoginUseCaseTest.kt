package com.fergolde.velodrome.domain.usecase

import com.fergolde.velodrome.domain.model.AuthResult
import com.fergolde.velodrome.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LoginUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val useCase = LoginUseCase(repository)

    @Test
    fun `blank username returns failure`() = runTest {
        val result = useCase("", "pass", "http://server.com")
        assertTrue(result.isFailure)
        assertEquals("Username cannot be empty", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.login(any(), any(), any()) }
    }

    @Test
    fun `blank password returns failure`() = runTest {
        val result = useCase("user", "", "http://server.com")
        assertTrue(result.isFailure)
        assertEquals("Password cannot be empty", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.login(any(), any(), any()) }
    }

    @Test
    fun `blank serverUrl returns failure`() = runTest {
        val result = useCase("user", "pass", "")
        assertTrue(result.isFailure)
        assertEquals("Server URL cannot be empty", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.login(any(), any(), any()) }
    }

    @Test
    fun `adds https prefix when missing`() = runTest {
        coEvery { repository.login("user", "pass", "https://server.com/") } returns
                Result.success(AuthResult(success = true))

        val result = useCase("user", "pass", "server.com")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `preserves http prefix`() = runTest {
        coEvery { repository.login("user", "pass", "http://server.com/") } returns
                Result.success(AuthResult(success = true))

        val result = useCase("user", "pass", "http://server.com")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `preserves https prefix`() = runTest {
        coEvery { repository.login("user", "pass", "https://server.com/") } returns
                Result.success(AuthResult(success = true))

        val result = useCase("user", "pass", "https://server.com")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `adds trailing slash`() = runTest {
        coEvery { repository.login("user", "pass", "https://server.com/") } returns
                Result.success(AuthResult(success = true))

        val result = useCase("user", "pass", "https://server.com")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `trims whitespace`() = runTest {
        coEvery { repository.login("user", "pass", "https://server.com/") } returns
                Result.success(AuthResult(success = true))

        val result = useCase("user", "pass", "  https://server.com  ")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `successful login delegates to repository`() = runTest {
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
        coEvery { repository.login("user", "pass", "https://server.com/") } returns
                Result.failure(Exception("Network error"))

        val result = useCase("user", "pass", "https://server.com/")
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
