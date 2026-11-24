package com.example.watt_vision_mobile.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.example.watt_vision_mobile.data.repository.AuthRepository
import com.example.watt_vision_mobile.data.repository.SessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository.getInstance(application)
    internal val session = authRepository.session

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    private var operation: Job? = null
    private var protectedGeneration: Long? = null
    private var protectedModels: ViewModelStore? = null

    init {
        viewModelScope.launch {
            session.collect { state ->
                if (state !is SessionState.Authenticated) clearProtectedModels()
                when (state) {
                    is SessionState.Authenticated -> _uiState.value = LoginUiState()
                    is SessionState.SignedOut -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = state.failure?.message)
                    }
                    else -> Unit
                }
            }
        }
        if (session.value == SessionState.Initializing) retryValidation()
    }

    fun onEmailChanged(newEmail: String) {
        _uiState.update { it.copy(email = newEmail, errorMessage = null) }
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(password = newPassword, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onRememberMeChanged(checked: Boolean) {
        _uiState.update { it.copy(rememberMe = checked) }
    }

    fun login() {
        if (operation?.isActive == true || session.value !is SessionState.SignedOut) return
        val input = uiState.value
        if (input.email.isBlank() || input.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Preencha e-mail e senha.") }
            return
        }

        _uiState.update {
            it.copy(password = "", isPasswordVisible = false, isLoading = true, errorMessage = null)
        }
        operation = viewModelScope.launch {
            try {
                authRepository.login(input.email, input.password, input.rememberMe)
            } finally {
                val failure = (session.value as? SessionState.SignedOut)?.failure
                _uiState.update { it.copy(isLoading = false, errorMessage = failure?.message ?: it.errorMessage) }
            }
        }
    }

    internal fun protectedStore(generation: Long): ViewModelStore {
        if (protectedGeneration != generation) {
            clearProtectedModels()
            protectedGeneration = generation
            protectedModels = ViewModelStore()
        }
        return requireNotNull(protectedModels)
    }

    private fun clearProtectedModels() {
        protectedModels?.clear()
        protectedModels = null
        protectedGeneration = null
    }

    override fun onCleared() {
        clearProtectedModels()
        super.onCleared()
    }

    internal fun retryValidation() {
        if (operation?.isActive == true) return
        operation = viewModelScope.launch { authRepository.restore() }
    }

    internal fun logout() {
        operation?.cancel()
        _uiState.value = LoginUiState()
        operation = viewModelScope.launch { authRepository.logout() }
    }
}
