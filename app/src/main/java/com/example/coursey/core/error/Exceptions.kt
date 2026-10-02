package com.example.coursey.core.error

sealed class AppException(message: String) : Exception(message)

class ServerException(message: String = "Server error") : AppException(message)

class NetworkException(message: String = "Network error") : AppException(message)

class InvalidCredentialsException(message: String = "Invalid credentials") : AppException(message)

class CacheException(message: String = "Cache error") : AppException(message)

class NotFoundException(message: String = "Not found") : AppException(message)
