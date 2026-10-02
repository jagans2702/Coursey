package com.example.coursey.core.usecase

import com.example.coursey.core.util.Result

interface UseCase<in Params, out Type> {
    suspend operator fun invoke(params: Params): Result<Type>
}
