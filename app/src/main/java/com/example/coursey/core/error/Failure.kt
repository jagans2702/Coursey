package com.example.coursey.core.error

sealed class Failure(open val message: String) {
    data class Server(override val message: String) : Failure(message)
    data class Network(override val message: String) : Failure(message)
    data class Timeout(override val message: String) : Failure(message)
    data class InvalidCredentials(override val message: String) : Failure(message)
    data class Cache(override val message: String) : Failure(message)
    data class NotFound(override val message: String) : Failure(message)
    data class Unknown(override val message: String) : Failure(message)
    data class Validation(val field: Field, override val message: String) : Failure(message) {
        enum class Field { EMAIL, PASSWORD }
    }
}
