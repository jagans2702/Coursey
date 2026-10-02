package com.example.coursey.core.util

import com.example.coursey.core.error.Failure

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val failure: Failure) : Result<Nothing>
}
