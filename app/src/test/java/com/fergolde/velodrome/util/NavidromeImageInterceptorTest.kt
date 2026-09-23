package com.fergolde.velodrome.util

import android.content.Context
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NavidromeImageInterceptorTest {

    private val credentialsManager: CredentialsManager = mockk()
    private val interceptor = NavidromeImageInterceptor(credentialsManager)
    private val context: Context = mockk(relaxed = true)

    @Test
    fun coverId_usesUnauthenticatedUrlAndExplicitStableCacheKeys() = runTest {
        every {
            credentialsManager.getCoverArtBaseUrl("al-123", 400)
        } returns "https://music.example.com/rest/getCoverArt.view?id=al-123&size=400&v=1.16.1&c=Velodrome"

        val request = ImageRequest.Builder(context)
            .data("al-123")
            .size(400)
            .build()
        val chain = mockChain(request)

        interceptor.intercept(chain)

        val transformed = slot<ImageRequest>()
        verify { chain.withRequest(capture(transformed)) }
        assertEquals(
            "https://music.example.com/rest/getCoverArt.view?id=al-123&size=400&v=1.16.1&c=Velodrome",
            transformed.captured.data
        )
        assertEquals(transformed.captured.data, transformed.captured.memoryCacheKey)
        assertEquals(transformed.captured.data, transformed.captured.diskCacheKey)
        verify(exactly = 0) { credentialsManager.getCoverArtUrl(any(), any()) }
    }

    @Test
    fun rotatingCredentials_doNotChangeRuntimeCacheIdentity() = runTest {
        val baseUrl = "https://music.example.com/rest/getCoverArt.view?id=al-123&size=400&v=1.16.1&c=Velodrome"
        every { credentialsManager.getCoverArtBaseUrl("al-123", 400) } returns baseUrl

        val first = transformedRequestFor("al-123")
        val second = transformedRequestFor("al-123")

        assertEquals(first.memoryCacheKey, second.memoryCacheKey)
        assertEquals(first.diskCacheKey, second.diskCacheKey)
        assertEquals(baseUrl, first.data)
        assertEquals(baseUrl, second.data)
    }

    private suspend fun transformedRequestFor(coverId: String): ImageRequest {
        val request = ImageRequest.Builder(context).data(coverId).size(400).build()
        val chain = mockChain(request)
        interceptor.intercept(chain)
        val transformed = slot<ImageRequest>()
        verify { chain.withRequest(capture(transformed)) }
        return transformed.captured
    }

    private fun mockChain(request: ImageRequest): Interceptor.Chain {
        val chain = mockk<Interceptor.Chain>(relaxed = true)
        every { chain.request } returns request
        every { chain.withRequest(any()) } returns chain
        return chain
    }
}
