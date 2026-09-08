package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.R
import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.data.remote.dto.ErrorDto
import com.fergolde.velodrome.data.remote.dto.SubsonicResponse
import com.fergolde.velodrome.data.remote.dto.SubsonicResponseDto
import com.fergolde.velodrome.util.CredentialsManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AuthRepositoryImplTest {

    private val api: NavidromeApi = mockk()
    private val credentialsManager: CredentialsManager = mockk()
    private var lastResId: Int? = null
    private val context: android.content.Context = mockk {
        every { getString(any()) } answers {
            lastResId = firstArg()
            "resolved"
        }
    }
    private val repository = AuthRepositoryImpl(context, api, credentialsManager)

    @Test
    fun `login success uses temp credentials then persists and returns success`() = runTest {
        val response = SubsonicResponse(SubsonicResponseDto(status = "ok"))
        coEvery { api.ping() } returns response
        every { credentialsManager.setTemporaryCredentials(any(), any(), any()) } just runs
        every { credentialsManager.saveCredentials(any(), any(), any()) } just runs

        val result = repository.login("user", "pass", "https://server.com/")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.success)
        verify { credentialsManager.setTemporaryCredentials("user", "pass", "https://server.com/") }
        verify { credentialsManager.saveCredentials("user", "pass", "https://server.com/") }
    }

    @Test
    fun `login failed does not persist credentials`() = runTest {
        val response = SubsonicResponse(
            SubsonicResponseDto(status = "fail", error = ErrorDto(code = 401, message = "Invalid credentials"))
        )
        coEvery { api.ping() } returns response
        every { credentialsManager.setTemporaryCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearTemporaryCredentials() } just runs

        repository.login("user", "pass", "https://server.com/")

        verify(exactly = 0) { credentialsManager.saveCredentials(any(), any(), any()) }
        verify { credentialsManager.clearTemporaryCredentials() }
    }

    @Test
    fun `login api error returns failure with error message`() = runTest {
        val response = SubsonicResponse(
            SubsonicResponseDto(status = "fail", error = ErrorDto(code = 401, message = "Invalid credentials"))
        )
        coEvery { api.ping() } returns response
        every { credentialsManager.setTemporaryCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearTemporaryCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("Invalid credentials", result.getOrNull()!!.error)
        verify { credentialsManager.clearTemporaryCredentials() }
    }

    @Test
    fun `login unknown host maps to spanish message`() = runTest {
        lastResId = null
        coEvery { api.ping() } throws UnknownHostException()
        every { credentialsManager.setTemporaryCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearTemporaryCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("resolved", result.getOrNull()!!.error)
        assertEquals(R.string.error_no_connection, lastResId)
    }

    @Test
    fun `login connect exception maps to spanish message`() = runTest {
        lastResId = null
        coEvery { api.ping() } throws ConnectException()
        every { credentialsManager.setTemporaryCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearTemporaryCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("resolved", result.getOrNull()!!.error)
        assertEquals(R.string.error_no_connection, lastResId)
    }

    @Test
    fun `login socket timeout maps to spanish message`() = runTest {
        lastResId = null
        coEvery { api.ping() } throws SocketTimeoutException()
        every { credentialsManager.setTemporaryCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearTemporaryCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("resolved", result.getOrNull()!!.error)
        assertEquals(R.string.error_server_timeout, lastResId)
    }

    @Test
    fun `login ioexception maps to spanish message`() = runTest {
        lastResId = null
        coEvery { api.ping() } throws IOException("Stream error")
        every { credentialsManager.setTemporaryCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearTemporaryCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("resolved", result.getOrNull()!!.error)
        assertEquals(R.string.error_invalid_url, lastResId)
    }

    @Test
    fun `login generic exception maps to default spanish message`() = runTest {
        lastResId = null
        coEvery { api.ping() } throws RuntimeException("Something went wrong")
        every { credentialsManager.setTemporaryCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearTemporaryCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("resolved", result.getOrNull()!!.error)
        assertEquals(R.string.error_network_generic, lastResId)
    }

}
