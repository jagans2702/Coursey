package com.example.coursey.features.auth.domain.validation

import com.example.coursey.core.error.Failure

object CredentialsValidator {

    const val MIN_PASSWORD_LENGTH = 6

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateEmail(email: String): Failure.Validation? = when {
        email.isBlank() -> Failure.Validation(Failure.Validation.Field.EMAIL, "Email is required")
        !EMAIL_REGEX.matches(email.trim()) -> Failure.Validation(Failure.Validation.Field.EMAIL, "Invalid email")
        else -> null
    }

    fun validatePassword(password: String): Failure.Validation? = when {
        password.isEmpty() -> Failure.Validation(Failure.Validation.Field.PASSWORD, "Password is required")
        password.length < MIN_PASSWORD_LENGTH -> Failure.Validation(
            Failure.Validation.Field.PASSWORD,
            "Password must be at least $MIN_PASSWORD_LENGTH characters",
        )
        else -> null
    }
}
