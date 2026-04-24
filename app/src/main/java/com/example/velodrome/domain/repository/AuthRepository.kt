package com.example.velodrome.domain.repository

import com.example.velodrome.domain.model.AuthResult

/**
 * Repository interface for authentication operations.
 */
interface AuthRepository {
    suspend fun login(username: String, password: String, serverUrl: String): Result<AuthResult>
    fun isLoggedIn(): Boolean
    fun logout()
    fun getServerUrl(): String
    fun setServerUrl(url: String)
}