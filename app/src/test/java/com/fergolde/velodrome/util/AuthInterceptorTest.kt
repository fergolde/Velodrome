package com.fergolde.velodrome.util

import com.fergolde.velodrome.data.remote.NavidromeApi
import io.mockk.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private val credentialsManager: CredentialsManager = mockk()
    private val interceptor = AuthInterceptor(credentialsManager)

    private fun chainWithUrl(url: String): Interceptor.Chain {
        val request = Request.Builder().url(url.toHttpUrl()).build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        return chain
    }

    private fun mockChainProceed(chain: Interceptor.Chain, code: Int = 200): Response {
        val response = mockk<Response>()
        every { response.code } returns code
        every { chain.proceed(any()) } returns response
        return response
    }

    @Test
    fun addsAuthParamsToRequest() {
        every { credentialsManager.getValidAuthParams() } returns Triple("user", "token123", "salt123")

        val chain = chainWithUrl("https://server.com/rest/ping.view")
        val response = mockChainProceed(chain)

        val result = interceptor.intercept(chain)

        val capturedRequest = slot<Request>()
        verify { chain.proceed(capture(capturedRequest)) }

        val url = capturedRequest.captured.url
        assertEquals("user", url.queryParameter("u"))
        assertEquals("token123", url.queryParameter("t"))
        assertEquals("salt123", url.queryParameter("s"))
        assertEquals(NavidromeApi.API_VERSION, url.queryParameter("v"))
        assertEquals(NavidromeApi.CLIENT_NAME, url.queryParameter("c"))
        assertEquals("json", url.queryParameter("f"))
    }

    @Test
    fun noAuthParams_skipsAuth() {
        every { credentialsManager.getValidAuthParams() } returns null

        val chain = chainWithUrl("https://server.com/rest/ping.view")
        val response = mockChainProceed(chain)

        val result = interceptor.intercept(chain)

        val capturedRequest = slot<Request>()
        verify { chain.proceed(capture(capturedRequest)) }

        val url = capturedRequest.captured.url
        assertNull(url.queryParameter("u"))
        assertNull(url.queryParameter("t"))
        assertNull(url.queryParameter("s"))
    }

    @Test
    fun response401_invalidatesAuth() {
        every { credentialsManager.getValidAuthParams() } returns Triple("user", "token", "salt")
        every { credentialsManager.invalidateAuth() } just runs

        val chain = chainWithUrl("https://server.com/rest/ping.view")
        val response = mockChainProceed(chain, 401)

        interceptor.intercept(chain)

        verify { credentialsManager.invalidateAuth() }
    }

    @Test
    fun response403_invalidatesAuth() {
        every { credentialsManager.getValidAuthParams() } returns Triple("user", "token", "salt")
        every { credentialsManager.invalidateAuth() } just runs

        val chain = chainWithUrl("https://server.com/rest/ping.view")
        val response = mockChainProceed(chain, 403)

        interceptor.intercept(chain)

        verify { credentialsManager.invalidateAuth() }
    }

    @Test
    fun response200_doesNotInvalidateAuth() {
        every { credentialsManager.getValidAuthParams() } returns Triple("user", "token", "salt")
        every { credentialsManager.invalidateAuth() } just runs

        val chain = chainWithUrl("https://server.com/rest/ping.view")
        val response = mockChainProceed(chain, 200)

        interceptor.intercept(chain)

        verify(exactly = 0) { credentialsManager.invalidateAuth() }
    }

    @Test
    fun preservesExistingQueryParams() {
        every { credentialsManager.getValidAuthParams() } returns Triple("user", "token", "salt")

        val chain = chainWithUrl("https://server.com/rest/ping.view?id=123&type=album")
        val response = mockChainProceed(chain)

        interceptor.intercept(chain)

        val capturedRequest = slot<Request>()
        verify { chain.proceed(capture(capturedRequest)) }

        val url = capturedRequest.captured.url
        assertEquals("123", url.queryParameter("id"))
        assertEquals("album", url.queryParameter("type"))
        assertEquals("user", url.queryParameter("u"))
    }

    @Test
    fun response500_doesNotInvalidateAuth() {
        every { credentialsManager.getValidAuthParams() } returns Triple("user", "token", "salt")
        every { credentialsManager.invalidateAuth() } just runs

        val chain = chainWithUrl("https://server.com/rest/ping.view")
        val response = mockChainProceed(chain, 500)

        interceptor.intercept(chain)

        verify(exactly = 0) { credentialsManager.invalidateAuth() }
    }
}
