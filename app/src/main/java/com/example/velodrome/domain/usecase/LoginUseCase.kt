package com.example.velodrome.domain.usecase

import com.example.velodrome.domain.model.AuthResult
import com.example.velodrome.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
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

        return repository.login(username, password, cleanUrl)
    }
}