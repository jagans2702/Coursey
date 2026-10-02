package com.example.coursey.features.auth.data.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.coursey.core.error.CacheException
import com.example.coursey.features.auth.data.model.UserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException

interface AuthLocalDataSource {
    suspend fun saveUser(user: UserModel)
    suspend fun getUser(): UserModel?
    suspend fun clear()
}

class AuthLocalDataSourceImpl(
    private val preferences: SharedPreferences,
) : AuthLocalDataSource {

    override suspend fun saveUser(user: UserModel) = withContext(Dispatchers.IO) {
        val saved = preferences.edit().putString(KEY_USER, user.toJson()).commit()
        if (!saved) throw CacheException("Unable to save session")
    }

    override suspend fun getUser(): UserModel? = withContext(Dispatchers.IO) {
        val json = preferences.getString(KEY_USER, null) ?: return@withContext null
        try {
            UserModel.fromJson(json)
        } catch (e: JSONException) {
            preferences.edit { remove(KEY_USER) }
            null
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.edit { remove(KEY_USER) }
    }

    private companion object {
        const val KEY_USER = "logged_in_user"
    }
}
