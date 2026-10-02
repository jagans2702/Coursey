package com.example.coursey.features.auth.domain.repository

import com.example.coursey.core.util.Result
import com.example.coursey.features.auth.domain.entity.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun getLoggedInUser(): Result<User?>
    suspend fun logout(): Result<Unit>
}
