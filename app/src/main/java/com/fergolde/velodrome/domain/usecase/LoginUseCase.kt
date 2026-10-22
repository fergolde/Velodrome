package com.fergolde.velodrome.domain.usecase

import com.fergolde.velodrome.domain.model.AuthResult
import com.fergolde.velodrome.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Named

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    @Named("cleartextNotAllowedMessage") private val cleartextNotAllowedMessage: String
) {
    suspend operator fun invoke(username: String, password: String, serverUrl: String): Result<AuthResult> {
        if (username.isBlank()) {
            return Result.failure(IllegalArgumentException("Username cannot be empty"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password cannot be empty"))
        }
        if (serverUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Server URL cannot be empty"))
        }

        // Saneamiento inteligente de la URL
        var cleanUrl = serverUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        if (!cleanUrl.endsWith("/")) {
            cleanUrl = "$cleanUrl/"
        }

        // HTTPS es obligatorio en todos los builds: el manifest desactiva
        // usesCleartextTraffic y la NetworkSecurityConfig fuerza HTTPS.
        if (cleanUrl.startsWith("http://")) {
            return Result.failure(IllegalArgumentException(cleartextNotAllowedMessage))
        }

        return repository.login(username, password, cleanUrl)
    }
}
