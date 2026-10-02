package com.example.coursey.features.auth.domain.usecase

import com.example.coursey.core.usecase.UseCase
import com.example.coursey.core.util.Result
import com.example.coursey.features.auth.domain.repository.AuthRepository

class LogoutUseCase(
    private val repository: AuthRepository,
) : UseCase<Unit, Unit> {

    override suspend fun invoke(params: Unit): Result<Unit> = repository.logout()
}
