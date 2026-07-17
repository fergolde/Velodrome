package com.fergolde.velodrome.util

import com.fergolde.velodrome.data.remote.NavidromeApi
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor that automatically adds authentication parameters to ALL requests.
 * 
 * Per Subsonic API requirements:
 * - u: username
 * - t: token (md5(password + salt))
 * - s: salt (random per request)
 * - v: API version (1.16.1)
 * - c: client name (Velodrome)
 * 
 * This generates NEW salt and token for EACH request (per requirements).
 * NO token persistence - always regenerated.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val credentialsManager: CredentialsManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Obtenemos los parámetros (reutiliza el token si es válido)
        val authParams = credentialsManager.getValidAuthParams()
            ?: return chain.proceed(originalRequest)

        val (username, token, salt) = authParams

        val newUrl = originalRequest.url.newBuilder()
            .addQueryParameter("u", username)
            .addQueryParameter("t", token)
            .addQueryParameter("s", salt)
            .addQueryParameter("v", NavidromeApi.API_VERSION)
            .addQueryParameter("c", NavidromeApi.CLIENT_NAME)
            .addQueryParameter("f", "json")
            .build()

        val newRequest = originalRequest.newBuilder().url(newUrl).build()
        val response = chain.proceed(newRequest)

        // Si el servidor nos rechaza el token (401/403), invalidamos la caché
        if (response.code == 401 || response.code == 403) {
            credentialsManager.invalidateAuth()
        }

        return response
    }
}