package com.example.coursey.features.course.domain.usecase

import com.example.coursey.features.course.domain.entity.CourseDetails
import com.example.coursey.features.course.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow

class ObserveCourseDetailsUseCase(
    private val repository: CourseRepository,
) {
    operator fun invoke(courseId: Int): Flow<CourseDetails?> = repository.observeCourseDetails(courseId)
}
