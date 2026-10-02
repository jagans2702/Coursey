package com.example.coursey.features.auth.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coursey.core.error.Failure
import com.example.coursey.core.ui.UserMessage
import com.example.coursey.core.util.Result
import com.example.coursey.features.auth.domain.usecase.LoginUseCase
import com.example.coursey.features.auth.domain.validation.CredentialsValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(email = savedStateHandle[KEY_EMAIL] ?: ""),
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var loginJob: Job? = null
    private var nextMessageId = 0L

    fun onEmailChange(email: String) {
        if (_uiState.value.isLoading) return
        savedStateHandle[KEY_EMAIL] = email
        _uiState.update { it.copy(email = email, emailError = null, loginError = null) }
    }

    fun onPasswordChange(password: String) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(password = password, passwordError = null, loginError = null) }
    }

    fun onLoginClick() {
        if (loginJob?.isActive == true) return

        val current = _uiState.value
        if (current.isLoading || current.isLoggedIn) return

        val emailError = CredentialsValidator.validateEmail(current.email)?.message
        val passwordError = CredentialsValidator.validatePassword(current.password)?.message
        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(emailError = emailError, passwordError = passwordError, loginError = null)
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, loginError = null, snackbarMessage = null) }

        loginJob = viewModelScope.launch {
            val result = loginUseCase(LoginUseCase.Params(current.email, current.password))
            _uiState.update { state ->
                when (result) {
                    is Result.Success -> state.copy(isLoading = false, isLoggedIn = true, password = "")
                    is Result.Error -> state.withFailure(result.failure)
                }
            }
        }
    }

    fun onRetryClick() = onLoginClick()

    fun onSnackbarShown(messageId: Long) {
        _uiState.update { state ->
            if (state.snackbarMessage?.id == messageId) state.copy(snackbarMessage = null) else state
        }
    }

    private fun LoginUiState.withFailure(failure: Failure): LoginUiState = when (failure) {
        is Failure.Validation -> when (failure.field) {
            Failure.Validation.Field.EMAIL -> copy(isLoading = false, emailError = failure.message)
            Failure.Validation.Field.PASSWORD -> copy(isLoading = false, passwordError = failure.message)
        }
        is Failure.Network -> copy(
            isLoading = false,
            snackbarMessage = UserMessage(id = nextMessageId++, text = failure.message),
        )
        else -> copy(isLoading = false, loginError = failure.message)
    }

    companion object {
        private const val KEY_EMAIL = "login_email"

        fun factory(loginUseCase: LoginUseCase): ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(loginUseCase, createSavedStateHandle()) }
        }
    }
}
