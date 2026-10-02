package com.example.coursey.features.auth.domain.usecase

import com.example.coursey.core.usecase.UseCase
import com.example.coursey.core.util.Result
import com.example.coursey.features.auth.domain.entity.User
import com.example.coursey.features.auth.domain.repository.AuthRepository

class GetLoggedInUserUseCase(
    private val repository: AuthRepository,
) : UseCase<Unit, User?> {

    override suspend fun invoke(params: Unit): Result<User?> = repository.getLoggedInUser()
}
