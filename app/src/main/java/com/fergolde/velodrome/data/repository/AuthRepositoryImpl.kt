package com.fergolde.velodrome.data.repository

import android.content.Context
import com.fergolde.velodrome.R
import com.fergolde.velodrome.data.remote.NavidromeApi
import dagger.hilt.android.qualifiers.ApplicationContext
import com.fergolde.velodrome.domain.model.AuthResult
import com.fergolde.velodrome.domain.repository.AuthRepository
import com.fergolde.velodrome.util.CredentialsManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: NavidromeApi,
    private val credentialsManager: CredentialsManager
) : AuthRepository {

    override suspend fun login(username: String, password: String, serverUrl: String): Result<AuthResult> {
        return runCatching {
            try {
                // Save credentials securely (username + password, NO token)
                credentialsManager.saveCredentials(username, password, serverUrl)

                // Try ping - auth interceptor will add u, t, s params automatically
                val response = api.ping()

                if (response.response.status == "ok") {
                    AuthResult(success = true)
                } else {
                    credentialsManager.clearCredentials()
                    val errorMsg = response.response.error?.message ?: "Invalid credentials"
                    AuthResult(success = false, error = errorMsg)
                }
            } catch (e: Exception) {
                credentialsManager.clearCredentials()
                val userMessage = when (e) {
                    is java.net.UnknownHostException,
                    is java.net.ConnectException -> context.getString(R.string.error_no_connection)
                    is java.net.SocketTimeoutException -> context.getString(R.string.error_server_timeout)
                    is java.io.IOException,
                    is IllegalArgumentException -> context.getString(R.string.error_invalid_url)
                    else -> context.getString(R.string.error_network_generic)
                }
                AuthResult(success = false, error = userMessage)
            }
        }
    }
}