package com.example.velodrome.util

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Interceptor de Coil 3 que añade autenticación a las requests de coverart.
 * 
 * Navidrome requiere autenticación para las imágenes de portada. Este interceptor
 * transforma el coverArtId en una URL autenticada antes de que Coil la procese.
 */
class NavidromeImageInterceptor @Inject constructor(
    private val credentialsManager: CredentialsManager
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request

        // Solo procesamos si es un String (coverArtId) que NO es una URL completa
        val data = request.data
        if (data !is String || data.startsWith("http")) {
            return chain.proceed()
        }

        // Es un coverArtId, generamos la URL con auth
        val coverArtId = data
        val size = 400 // Tamaño por defecto

        val authenticatedUrl = runBlocking {
            credentialsManager.getCoverArtUrl(coverArtId, size)
        } ?: return chain.proceed()

        // Crear nuevo request con la URL autenticada usando withRequest
        val newRequest = request.newBuilder()
            .data(authenticatedUrl)
            .build()

        return chain.withRequest(newRequest).proceed()
    }
}