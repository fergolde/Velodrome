package com.fergolde.velodrome.domain.usecase

import android.content.Context
import android.content.pm.ApplicationInfo
import com.fergolde.velodrome.R
import com.fergolde.velodrome.domain.model.AuthResult
import com.fergolde.velodrome.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
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

        // En release no permitimos tráfico en claro: el manifest desactiva
        // usesCleartextTraffic y la NetworkSecurityConfig fuerza HTTPS.
        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebug && cleanUrl.startsWith("http://")) {
            return Result.failure(
                IllegalArgumentException(context.getString(R.string.error_cleartext_not_allowed))
            )
        }

        return repository.login(username, password, cleanUrl)
    }
}
