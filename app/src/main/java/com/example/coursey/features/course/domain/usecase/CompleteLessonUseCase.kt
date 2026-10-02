package com.example.coursey.features.course.domain.usecase

import com.example.coursey.core.error.Failure
import com.example.coursey.core.usecase.UseCase
import com.example.coursey.core.util.Result
import com.example.coursey.features.course.domain.logic.ProgressCalculator
import com.example.coursey.features.course.domain.repository.CourseRepository

class CompleteLessonUseCase(
    private val repository: CourseRepository,
) : UseCase<CompleteLessonUseCase.Params, Int> {

    data class Params(val courseId: Int, val lessonId: Int)

    override suspend fun invoke(params: Params): Result<Int> {
        val lessons = when (val result = repository.getLessons(params.courseId)) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }

        val lesson = lessons.firstOrNull { it.id == params.lessonId }
            ?: return Result.Error(Failure.NotFound("Lesson not found"))

        if (lesson.completed) return Result.Success(ProgressCalculator.calculate(lessons))

        val updated = lessons.map { if (it.id == params.lessonId) it.copy(completed = true) else it }
        val progress = ProgressCalculator.calculate(updated)

        return when (val saved = repository.completeLesson(params.courseId, params.lessonId, progress)) {
            is Result.Success -> Result.Success(progress)
            is Result.Error -> saved
        }
    }
}
