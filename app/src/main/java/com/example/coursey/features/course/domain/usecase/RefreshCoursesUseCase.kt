package com.example.coursey.features.course.domain.usecase

import com.example.coursey.core.usecase.UseCase
import com.example.coursey.core.util.Result
import com.example.coursey.features.course.domain.entity.Course
import com.example.coursey.features.course.domain.repository.CourseRepository

class RefreshCoursesUseCase(
    private val repository: CourseRepository,
) : UseCase<Unit, List<Course>> {

    override suspend fun invoke(params: Unit): Result<List<Course>> = repository.refreshCourses()
}
