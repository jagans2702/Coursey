package com.example.coursey.features.auth.domain.usecase

import com.example.coursey.core.error.Failure
import com.example.coursey.core.util.Result
import com.example.coursey.features.auth.domain.entity.User
import com.example.coursey.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {

    private val repository = FakeAuthRepository()
    private val loginUseCase = LoginUseCase(repository)

    @Test
    fun `valid email and password logs in`() = runBlocking {
        val result = loginUseCase(LoginUseCase.Params("student@example.com", "password123"))

        assertTrue(result is Result.Success)
        assertTrue(repository.loginCalled)
    }

    @Test
    fun `invalid email does not log in`() = runBlocking {
        val result = loginUseCase(LoginUseCase.Params("not-an-email", "password123"))

        assertTrue(result is Result.Error && result.failure is Failure.Validation)
        assertFalse(repository.loginCalled)
    }

    @Test
    fun `short password does not log in`() = runBlocking {
        val result = loginUseCase(LoginUseCase.Params("student@example.com", "123"))

        assertTrue(result is Result.Error && result.failure is Failure.Validation)
        assertFalse(repository.loginCalled)
    }

    private class FakeAuthRepository : AuthRepository {

        var loginCalled = false

        override suspend fun login(email: String, password: String): Result<User> {
            loginCalled = true
            return Result.Success(User(id = 1, name = "Demo Student", email = email))
        }

        override suspend fun getLoggedInUser(): Result<User?> = Result.Success(null)

        override suspend fun logout(): Result<Unit> = Result.Success(Unit)
    }
}
