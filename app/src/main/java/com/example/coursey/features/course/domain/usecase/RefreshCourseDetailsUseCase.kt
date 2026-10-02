package com.example.coursey.features.course.domain.usecase

import com.example.coursey.core.usecase.UseCase
import com.example.coursey.core.util.Result
import com.example.coursey.features.course.domain.entity.CourseDetails
import com.example.coursey.features.course.domain.repository.CourseRepository

class RefreshCourseDetailsUseCase(
    private val repository: CourseRepository,
) : UseCase<Int, CourseDetails> {

    override suspend fun invoke(params: Int): Result<CourseDetails> {
        val result = repository.refreshCourseDetails(params)
        if (result !is Result.Success) return result

        val details = result.data
        if (details.lessons.isEmpty() || details.course.progress == details.progress) return result

        repository.updateCourseProgress(params, details.progress)
        return Result.Success(details.copy(course = details.course.copy(progress = details.progress)))
    }
}
