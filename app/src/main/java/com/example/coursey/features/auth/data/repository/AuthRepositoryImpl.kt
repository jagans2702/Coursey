package com.example.coursey.features.auth.data.repository

import com.example.coursey.core.error.CacheException
import com.example.coursey.core.error.Failure
import com.example.coursey.core.error.InvalidCredentialsException
import com.example.coursey.core.error.NetworkException
import com.example.coursey.core.error.ServerException
import com.example.coursey.core.network.NetworkInfo
import com.example.coursey.core.session.UserDataCleaner
import com.example.coursey.core.util.Result
import com.example.coursey.features.auth.data.datasource.AuthLocalDataSource
import com.example.coursey.features.auth.data.datasource.AuthRemoteDataSource
import com.example.coursey.features.auth.domain.entity.User
import com.example.coursey.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl(
    private val remote: AuthRemoteDataSource,
    private val local: AuthLocalDataSource,
    private val networkInfo: NetworkInfo,
    private val userDataCleaners: List<UserDataCleaner> = emptyList(),
    private val requestTimeoutMillis: Long = 15_000L,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        if (!networkInfo.isConnected()) return Result.Error(noInternetFailure())

        return try {
            val user = withTimeout(requestTimeoutMillis) { remote.login(email, password) }
            local.saveUser(user)
            Result.Success(user.toEntity())
        } catch (e: TimeoutCancellationException) {
            Result.Error(Failure.Timeout("The request timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: InvalidCredentialsException) {
            Result.Error(Failure.InvalidCredentials("Invalid email or password"))
        } catch (e: NetworkException) {
            Result.Error(noInternetFailure())
        } catch (e: IOException) {
            Result.Error(noInternetFailure())
        } catch (e: ServerException) {
            Result.Error(Failure.Server("Unable to login. Please try again."))
        } catch (e: CacheException) {
            Result.Error(Failure.Cache("Unable to save your session. Please try again."))
        } catch (e: Exception) {
            Result.Error(Failure.Unknown("Something went wrong. Please try again."))
        }
    }

    override suspend fun getLoggedInUser(): Result<User?> = try {
        Result.Success(local.getUser()?.toEntity())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(Failure.Cache("Unable to read session"))
    }

    override suspend fun logout(): Result<Unit> = try {
        local.clear()
        userDataCleaners.forEach { it.clearUserData() }
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(Failure.Cache("Unable to clear session"))
    }

    private fun noInternetFailure() =
        Failure.Network("No internet connection. Please connect and try again.")
}
