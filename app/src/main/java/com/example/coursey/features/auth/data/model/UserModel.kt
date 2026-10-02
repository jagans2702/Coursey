package com.example.coursey.features.auth.data.model

import com.example.coursey.features.auth.domain.entity.User
import org.json.JSONObject

data class UserModel(
    val id: Int,
    val name: String,
    val email: String,
) {
    fun toEntity(): User = User(id = id, name = name, email = email)

    fun toJson(): String = JSONObject()
        .put(KEY_ID, id)
        .put(KEY_NAME, name)
        .put(KEY_EMAIL, email)
        .toString()

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"

        fun fromJson(json: JSONObject): UserModel = UserModel(
            id = json.getInt(KEY_ID),
            name = json.getString(KEY_NAME),
            email = json.getString(KEY_EMAIL),
        )

        fun fromJson(json: String): UserModel = fromJson(JSONObject(json))
    }
}
