package com.example.coursey.features.course.domain.usecase

import com.example.coursey.features.course.domain.entity.Course
import com.example.coursey.features.course.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow

class ObserveCoursesUseCase(
    private val repository: CourseRepository,
) {
    operator fun invoke(): Flow<List<Course>> = repository.observeCourses()
}
