package com.example.velodrome.data.repository

import android.util.Log
import com.example.velodrome.data.remote.NavidromeApi
import com.example.velodrome.domain.model.AuthResult
import com.example.velodrome.domain.repository.AuthRepository
import com.example.velodrome.util.CredentialsManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: NavidromeApi,
    private val credentialsManager: CredentialsManager
) : AuthRepository {

    override suspend fun login(username: String, password: String, serverUrl: String): Result<AuthResult> {
        return runCatching {
            try {
                // Save credentials securely (username + password, NO token)
                credentialsManager.saveCredentials(username, password, serverUrl)
                Log.d("AuthRepository", "Credentials saved for user: $username, server: $serverUrl")

                // Try ping - auth interceptor will add u, t, s params automatically
                Log.d("AuthRepository", "Calling api.ping()...")
                val response = api.ping()
                Log.d("AuthRepository", "Ping response: $response")
                Log.d("AuthRepository", "Ping response status: ${response.response.status}")

                if (response.response.status == "ok") {
                    Log.d("AuthRepository", "Login successful!")
                    AuthResult(success = true, token = password)
                } else {
                    credentialsManager.clearCredentials()
                    val errorMsg = response.response.error?.message ?: "Invalid credentials"
                    Log.d("AuthRepository", "Error: $errorMsg")
                    AuthResult(success = false, error = errorMsg)
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Login exception", e)
                credentialsManager.clearCredentials()
                AuthResult(success = false, error = e.message ?: "Unknown error")
            }
        }
    }

    override fun isLoggedIn(): Boolean {
        return credentialsManager.hasCredentials()
    }

    override fun logout() {
        credentialsManager.clearCredentials()
    }

    override fun getServerUrl(): String {
        return credentialsManager.getServerUrl() ?: "https://your-navidrome-server.com/"
    }

    override fun setServerUrl(url: String) {
        val currentUser = credentialsManager.getUsername()
        val currentPass = credentialsManager.getPassword()
        if (currentUser != null && currentPass != null) {
            credentialsManager.saveCredentials(currentUser, currentPass, url)
        }
    }
}