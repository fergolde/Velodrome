package com.fergolde.velodrome.util

import com.fergolde.velodrome.data.remote.NavidromeApi
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor that adds Subsonic authentication parameters to requests
 * aimed at the configured server.
 *
 * Per Subsonic API requirements:
 * - u: username
 * - t: token (md5(password + salt))
 * - s: salt
 * - v: API version (1.16.1)
 * - c: client name (Velodrome)
 *
 * The token/salt pair is cached by [CredentialsManager] and reused until the
 * session window expires; it is never written to disk.
 *
 * Credentials are attached ONLY when the request host matches the configured
 * server. This client is shared by Retrofit, Coil and ExoPlayer, so a URL
 * that originates outside the configured server (a `coverArt` value returned
 * by the server, or a media URI pushed by an external MediaController) would
 * otherwise carry the account token to a third party.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val credentialsManager: CredentialsManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (!isRequestForConfiguredServer(originalRequest.url.host)) {
            return chain.proceed(originalRequest)
        }

        val authParams = credentialsManager.getValidAuthParams()
            ?: return chain.proceed(originalRequest)

        val (username, token, salt) = authParams

        val newUrl = originalRequest.url.newBuilder()
            .setQueryParameter("u", username)
            .setQueryParameter("t", token)
            .setQueryParameter("s", salt)
            .setQueryParameter("v", NavidromeApi.API_VERSION)
            .setQueryParameter("c", NavidromeApi.CLIENT_NAME)
            .setQueryParameter("f", "json")
            .build()

        val newRequest = originalRequest.newBuilder().url(newUrl).build()
        val response = chain.proceed(newRequest)

        // Si el servidor nos rechaza el token (401/403), invalidamos la caché
        if (response.code == 401 || response.code == 403) {
            credentialsManager.invalidateAuth()
        }

        return response
    }

    /**
     * True when [requestHost] is the host the user configured. Hostless URLs
     * never qualify, and an unset server URL means no request can match.
     */
    private fun isRequestForConfiguredServer(requestHost: String): Boolean {
        val serverUrl = credentialsManager.getServerUrl() ?: return false
        val serverHost = serverUrl.trim()
            .substringAfter("://", "")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore(':')
            .lowercase()
        return serverHost.isNotEmpty() && serverHost == requestHost.lowercase()
    }
}
