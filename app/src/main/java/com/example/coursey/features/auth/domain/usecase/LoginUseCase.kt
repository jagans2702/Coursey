package com.example.coursey.features.auth.domain.usecase

import com.example.coursey.core.usecase.UseCase
import com.example.coursey.core.util.Result
import com.example.coursey.features.auth.domain.entity.User
import com.example.coursey.features.auth.domain.repository.AuthRepository
import com.example.coursey.features.auth.domain.validation.CredentialsValidator

class LoginUseCase(
    private val repository: AuthRepository,
) : UseCase<LoginUseCase.Params, User> {

    data class Params(val email: String, val password: String)

    override suspend fun invoke(params: Params): Result<User> {
        CredentialsValidator.validateEmail(params.email)?.let { return Result.Error(it) }
        CredentialsValidator.validatePassword(params.password)?.let { return Result.Error(it) }
        return repository.login(params.email.trim(), params.password)
    }
}
