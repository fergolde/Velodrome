package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.domain.model.AuthResult
import com.fergolde.velodrome.domain.repository.AuthRepository
import com.fergolde.velodrome.util.CredentialsManager
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

                // Try ping - auth interceptor will add u, t, s params automatically
                val response = api.ping()

                if (response.response.status == "ok") {
                    AuthResult(success = true, token = password)
                } else {
                    credentialsManager.clearCredentials()
                    val errorMsg = response.response.error?.message ?: "Invalid credentials"
                    AuthResult(success = false, error = errorMsg)
                }
            } catch (e: Exception) {
                credentialsManager.clearCredentials()
                val userMessage = when (e) {
                    is java.net.UnknownHostException,
                    is java.net.ConnectException -> "No se puede conectar al servidor. Comprueba la URL y tu conexión."
                    is java.net.SocketTimeoutException -> "El servidor tarda mucho en responder. Comprueba tu conexión."
                    is java.io.IOException,
                    is IllegalArgumentException -> "La URL proporcionada no tiene un formato válido."
                    else -> "Error de red: Usuario o contraseña incorrectos, o servidor inaccesible."
                }
                AuthResult(success = false, error = userMessage)
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