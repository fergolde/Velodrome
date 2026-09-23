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

        val coverArtUrl = when {
            data.startsWith("http") && data.contains(COVER_ART_PATH) -> {
                val parsed = data.toHttpUrlOrNull() ?: return chain.proceed()
                val id = parsed.queryParameter("id") ?: return chain.proceed()
                val requestedSize = parsed.queryParameter("size")?.toIntOrNull()
                    ?: chain.request.sizeResolver.size().width.pxOrElse { DEFAULT_SIZE }
                parsed.newBuilder()
                    .setQueryParameter("id", id)
                    .setQueryParameter("size", requestedSize.toString())
                    .removeAllQueryParameters("u")
                    .removeAllQueryParameters("t")
                    .removeAllQueryParameters("s")
                    .build()
                    .toString()
            }
            !data.startsWith("http") -> {
                val requestedSize = chain.request.sizeResolver.size().width.pxOrElse { DEFAULT_SIZE }
                credentialsManager.getCoverArtBaseUrl(data, requestedSize) ?: return chain.proceed()
            }
            else -> return chain.proceed()
        }

        val canonicalUrl = NavidromeCoverArtKeyer.normalize(coverArtUrl)

        val newRequest = request.newBuilder()
            .data(coverArtUrl)
            // AuthInterceptor adds u/t/s to the shared OkHttp request. Keep those
            // credentials out of both Coil cache identities.
            .memoryCacheKey(canonicalUrl)
            .diskCacheKey(canonicalUrl)
            .build()

        return chain.withRequest(newRequest).proceed()
    }

    private companion object {
        private const val COVER_ART_PATH = "getCoverArt"
        private const val DEFAULT_SIZE = 400

    }
}
