package com.fergolde.velodrome.util

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.size.pxOrElse
import javax.inject.Inject

class NavidromeImageInterceptor @Inject constructor(
    private val credentialsManager: CredentialsManager
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request

        val data = request.data
        if (data !is String || data.startsWith("http")) {
            return chain.proceed()
        }

        val coverArtId = data
        val size = chain.request.sizeResolver.size().width.pxOrElse { 400 }

        val authenticatedUrl = credentialsManager.getCoverArtUrl(coverArtId, size)
            ?: return chain.proceed()

        val newRequest = request.newBuilder()
            .data(authenticatedUrl)
            .build()

        return chain.withRequest(newRequest).proceed()
    }
}