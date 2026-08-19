package com.fergolde.velodrome.data.repository

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
    private val repository = AuthRepositoryImpl(api, credentialsManager)

    @Test
    fun `login success saves credentials and returns success`() = runTest {
        val response = SubsonicResponse(SubsonicResponseDto(status = "ok", version = "1.16.1"))
        coEvery { api.ping() } returns response
        every { credentialsManager.saveCredentials(any(), any(), any()) } just runs

        val result = repository.login("user", "pass", "https://server.com/")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.success)
        verify { credentialsManager.saveCredentials("user", "pass", "https://server.com/") }
    }

    @Test
    fun `login api error returns failure with error message`() = runTest {
        val response = SubsonicResponse(
            SubsonicResponseDto(status = "fail", version = "1.16.1", error = ErrorDto(code = 401, message = "Invalid credentials"))
        )
        coEvery { api.ping() } returns response
        every { credentialsManager.saveCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("Invalid credentials", result.getOrNull()!!.error)
        verify { credentialsManager.clearCredentials() }
    }

    @Test
    fun `login unknown host maps to spanish message`() = runTest {
        coEvery { api.ping() } throws UnknownHostException()
        every { credentialsManager.saveCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("No se puede conectar al servidor. Comprueba la URL y tu conexión.", result.getOrNull()!!.error)
    }

    @Test
    fun `login connect exception maps to spanish message`() = runTest {
        coEvery { api.ping() } throws ConnectException()
        every { credentialsManager.saveCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("No se puede conectar al servidor. Comprueba la URL y tu conexión.", result.getOrNull()!!.error)
    }

    @Test
    fun `login socket timeout maps to spanish message`() = runTest {
        coEvery { api.ping() } throws SocketTimeoutException()
        every { credentialsManager.saveCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("El servidor tarda mucho en responder. Comprueba tu conexión.", result.getOrNull()!!.error)
    }

    @Test
    fun `login ioexception maps to spanish message`() = runTest {
        coEvery { api.ping() } throws IOException("Stream error")
        every { credentialsManager.saveCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("La URL proporcionada no tiene un formato válido.", result.getOrNull()!!.error)
    }

    @Test
    fun `login generic exception maps to default spanish message`() = runTest {
        coEvery { api.ping() } throws RuntimeException("Something went wrong")
        every { credentialsManager.saveCredentials(any(), any(), any()) } just runs
        every { credentialsManager.clearCredentials() } just runs

        val result = repository.login("user", "pass", "https://server.com/")
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!.success)
        assertEquals("Error de red: Usuario o contraseña incorrectos, o servidor inaccesible.", result.getOrNull()!!.error)
    }

}
