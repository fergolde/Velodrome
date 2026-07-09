package com.fergolde.velodrome.presentation.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fergolde.velodrome.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val serverUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun onServerUrlChange(serverUrl: String) {
        _uiState.update { it.copy(serverUrl = serverUrl, error = null) }
    }

    fun login() {
        val currentState = _uiState.value
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(error = "Please enter username and password") }
            return
        }
        if (currentState.serverUrl.isBlank()) {
            _uiState.update { it.copy(error = "Please enter server URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            loginUseCase(currentState.username, currentState.password, currentState.serverUrl)
                .onSuccess { authResult ->
                    if (authResult.success) {
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, error = authResult.error ?: "Login failed")
                        }
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(isLoading = false, error = exception.message ?: "Unknown error")
                    }
                }
        }
    }
}