package com.example.coursey.features.auth.presentation.login

import com.example.coursey.core.ui.UserMessage

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val snackbarMessage: UserMessage? = null,
    val isLoggedIn: Boolean = false,
)
