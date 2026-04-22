package com.example.velodrome.util

import android.util.Log
import com.example.velodrome.data.remote.NavidromeApi
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
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

    private val TAG = "AuthInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // Generate fresh auth params for each request
        val authParams = credentialsManager.generateAuthParams()

        if (authParams == null) {
            Log.w(TAG, "No credentials available - request without auth")
            // Allow request to proceed (might be login request)
            return chain.proceed(originalRequest)
        }

        val (username, token, salt) = authParams

        // Build new URL with auth params and force JSON response
        val newUrl = originalUrl.newBuilder()
            .addQueryParameter("u", username)
            .addQueryParameter("t", token)
            .addQueryParameter("s", salt)
            .addQueryParameter("v", NavidromeApi.API_VERSION)
            .addQueryParameter("c", NavidromeApi.CLIENT_NAME)
            .addQueryParameter("f", "json")  // Force JSON response
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        Log.d(TAG, "Added auth: u=$username, s=$salt, v=${NavidromeApi.API_VERSION}, c=${NavidromeApi.CLIENT_NAME}")

        return try {
            val response = chain.proceed(newRequest)
            
            // Check for auth errors - if 401/403, clear credentials
            if (response.code == 401 || response.code == 403) {
                Log.w(TAG, "Auth error - clearing credentials")
                credentialsManager.clearCredentials()
            }
            
            response
        } catch (e: IOException) {
            Log.e(TAG, "Request failed", e)
            throw e
        }
    }
}