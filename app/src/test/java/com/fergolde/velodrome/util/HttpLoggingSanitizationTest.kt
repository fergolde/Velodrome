package com.fergolde.velodrome.util

import com.fergolde.velodrome.data.remote.NavidromeApi
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class HttpLoggingSanitizationTest {

    @Test
    fun `logging interceptor placed before auth does not log credentials`() {
        val credentialsManager: CredentialsManager = mockk()
        every { credentialsManager.getValidAuthParams() } returns Triple("user", "token123", "salt123")

        val authInterceptor = AuthInterceptor(credentialsManager)

        val loggedMessages = mutableListOf<String>()
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            loggedMessages.add(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val originalRequest = Request.Builder()
            .url("https://server.com/rest/ping.view".toHttpUrl())
            .build()

        // Build chain: logging -> auth -> network
        val networkResponse = Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

        val networkChain = mockk<Interceptor.Chain>()
        every { networkChain.request() } returns originalRequest
        every { networkChain.proceed(any()) } returns networkResponse

        val authChain = mockk<Interceptor.Chain>(relaxed = true)
        val authSlot = slot<Request>()
        every { authChain.request() } returns originalRequest
        every { authChain.proceed(capture(authSlot)) } answers {
            authInterceptor.intercept(networkChain)
        }

        loggingInterceptor.intercept(authChain)

        // Verify the request that reached the network has auth params.
        val networkSlot = slot<Request>()
        verify { networkChain.proceed(capture(networkSlot)) }
        assertNotNull("Auth param 'u' missing", networkSlot.captured.url.queryParameter("u"))

        val fullLog = loggedMessages.joinToString("\n")
        assertFalse("Log leaked username", fullLog.contains("u=user"))
        assertFalse("Log leaked token", fullLog.contains("t=token123"))
        assertFalse("Log leaked salt", fullLog.contains("s=salt123"))
    }
}
