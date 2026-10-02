package com.example.coursey.features.auth.data.datasource

import com.example.coursey.core.error.InvalidCredentialsException
import com.example.coursey.core.error.ServerException
import com.example.coursey.features.auth.data.model.UserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

interface AuthRemoteDataSource {
    suspend fun login(email: String, password: String): UserModel
}

class AuthMockRemoteDataSource(
    private val networkDelayMillis: Long = 1_500L,
) : AuthRemoteDataSource {

    override suspend fun login(email: String, password: String): UserModel = withContext(Dispatchers.IO) {
        delay(networkDelayMillis)

        val users = try {
            JSONObject(MOCK_USERS_JSON).getJSONArray("users")
        } catch (e: JSONException) {
            throw ServerException("Malformed mock response")
        }

        for (index in 0 until users.length()) {
            val user = users.optJSONObject(index) ?: continue
            val emailMatches = user.optString(KEY_EMAIL).equals(email, ignoreCase = true)
            val passwordMatches = user.optString(KEY_PASSWORD) == password
            if (emailMatches && passwordMatches) {
                if (user.optBoolean(KEY_SIMULATE_SERVER_ERROR)) throw ServerException()
                return@withContext UserModel.fromJson(user)
            }
        }
        throw InvalidCredentialsException()
    }

    private companion object {
        const val KEY_EMAIL = "email"
        const val KEY_PASSWORD = "password"
        const val KEY_SIMULATE_SERVER_ERROR = "simulateServerError"

        const val MOCK_USERS_JSON = """
        {
          "users": [
            { "id": 1, "name": "Test User", "email": "test@gmail.com", "password": "test@123" },
          ]
        }
        """
    }
}
