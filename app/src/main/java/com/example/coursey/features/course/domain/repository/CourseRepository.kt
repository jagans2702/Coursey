package com.example.coursey.features.course.domain.repository

import com.example.coursey.core.util.Result
import com.example.coursey.features.course.domain.entity.Course
import com.example.coursey.features.course.domain.entity.CourseDetails
import com.example.coursey.features.course.domain.entity.Lesson
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    fun observeCourses(): Flow<List<Course>>
    suspend fun refreshCourses(): Result<List<Course>>

    fun observeCourseDetails(courseId: Int): Flow<CourseDetails?>
    suspend fun refreshCourseDetails(courseId: Int): Result<CourseDetails>

    suspend fun getLessons(courseId: Int): Result<List<Lesson>>
    suspend fun completeLesson(courseId: Int, lessonId: Int, progress: Int): Result<Unit>
    suspend fun updateCourseProgress(courseId: Int, progress: Int): Result<Unit>
}
