package com.fergolde.velodrome.util

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.size.pxOrElse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

class NavidromeImageInterceptor @Inject constructor(
    private val credentialsManager: CredentialsManager
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data
        if (data !is String) {
            return chain.proceed()
        }

        // Already authenticated URLs can pass through unchanged.
        if (data.isAuthenticatedCoverUrl()) {
            return chain.proceed()
        }

        val (coverArtId, size) = when {
            data.startsWith("http") && data.contains(COVER_ART_PATH) -> {
                val parsed = data.toHttpUrlOrNull() ?: return chain.proceed()
                val id = parsed.queryParameter("id") ?: return chain.proceed()
                val requestedSize = parsed.queryParameter("size")?.toIntOrNull()
                    ?: chain.request.sizeResolver.size().width.pxOrElse { DEFAULT_SIZE }
                id to requestedSize
            }
            !data.startsWith("http") -> {
                val requestedSize = chain.request.sizeResolver.size().width.pxOrElse { DEFAULT_SIZE }
                data to requestedSize
            }
            else -> return chain.proceed()
        }

        val authenticatedUrl = credentialsManager.getCoverArtUrl(coverArtId, size)
            ?: return chain.proceed()

        val newRequest = request.newBuilder()
            .data(authenticatedUrl)
            .build()

        return chain.withRequest(newRequest).proceed()
    }

    private companion object {
        private const val COVER_ART_PATH = "getCoverArt"
        private const val DEFAULT_SIZE = 400

        private fun String.isAuthenticatedCoverUrl(): Boolean {
            if (!startsWith("http") || !contains(COVER_ART_PATH)) return false
            val parsed = toHttpUrlOrNull() ?: return false
            return parsed.queryParameterNames.containsAll(setOf("u", "t", "s"))
        }
    }
}
