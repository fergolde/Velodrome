package com.fergolde.velodrome.util

import com.fergolde.velodrome.di.createHttpLoggingInterceptor
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the production logging interceptor against leaking the Subsonic token.
 *
 * The chain is wired as `logging -> auth -> network`, and the response the
 * logger prints carries `response.request.url` — the request as it reached the
 * wire, WITH credentials. This test therefore asserts against the real factory
 * (`createHttpLoggingInterceptor`) rather than a hand-built interceptor, so it
 * fails if the redaction is ever dropped from production code.
 */
class HttpLoggingSanitizationTest {

    private val credentialsManager: CredentialsManager = mockk()

    private fun stubCredentials() {
        every { credentialsManager.getValidAuthParams() } returns Triple("user", "token123", "salt123")
        every { credentialsManager.getServerUrl() } returns "https://server.com/"
    }

    /**
     * Runs the real production interceptor over logging -> auth -> network and
     * returns the captured log plus the request that actually reached the wire.
     */
    private fun captureLog(): Pair<List<String>, Request> {
        stubCredentials()
        val authInterceptor = AuthInterceptor(credentialsManager)

        val loggedMessages = mutableListOf<String>()
        val loggingInterceptor = createHttpLoggingInterceptor(
            debug = true,
            logger = { message -> loggedMessages.add(message) }
        )

        val originalRequest = Request.Builder()
            .url("https://server.com/rest/ping.view".toHttpUrl())
            .build()

        // `response.request` must be the AUTHENTICATED request — that is what
        // OkHttp really hands back — and the body must be non-null because the
        // logger dereferences `response.body!!` before the response line.
        val authenticatedRequest = slot<Request>()
        val networkChain = mockk<Interceptor.Chain>()
        every { networkChain.request() } returns originalRequest
        every { networkChain.proceed(capture(authenticatedRequest)) } answers {
            Response.Builder()
                .request(authenticatedRequest.captured)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val authChain = mockk<Interceptor.Chain>(relaxed = true)
        every { authChain.request() } returns originalRequest
        every { authChain.proceed(any()) } answers { authInterceptor.intercept(networkChain) }

        loggingInterceptor.intercept(authChain)

        // Precondition, without which every leak assertion below is vacuous.
        val authenticatedUrl = authenticatedRequest.captured.url.toString()
        assertTrue(
            "Test setup broken: token never reached the network — $authenticatedUrl",
            authenticatedUrl.contains("token123")
        )
        return loggedMessages to authenticatedRequest.captured
    }

    @Test
    fun `credentials are redacted from every logged line`() {
        val (loggedMessages, _) = captureLog()

        val fullLog = loggedMessages.joinToString("\n")
        assertFalse("Log leaked username", fullLog.contains("u=user"))
        assertFalse("Log leaked token", fullLog.contains("t=token123"))
        assertFalse("Log leaked salt", fullLog.contains("s=salt123"))
        assertFalse("Log leaked token as a bare value", fullLog.contains("token123"))
        assertFalse("Log leaked salt as a bare value", fullLog.contains("salt123"))
    }

    @Test
    fun `redaction keeps the log useful`() {
        val (loggedMessages, _) = captureLog()

        val fullLog = loggedMessages.joinToString("\n")
        assertTrue("Response line missing — redaction silenced the logger", fullLog.contains("<-- 200"))
        assertTrue("Log must still name the endpoint", fullLog.contains("ping.view"))
    }

    @Test
    fun `release builds log nothing at all`() {
        val releaseLogger = createHttpLoggingInterceptor(debug = false)
        assertTrue(
            "Release must not log HTTP traffic",
            releaseLogger.level == okhttp3.logging.HttpLoggingInterceptor.Level.NONE
        )
    }
}
