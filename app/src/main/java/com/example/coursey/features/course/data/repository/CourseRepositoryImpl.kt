package com.example.coursey.features.course.data.repository

import com.example.coursey.core.error.CacheException
import com.example.coursey.core.error.Failure
import com.example.coursey.core.error.NetworkException
import com.example.coursey.core.error.NotFoundException
import com.example.coursey.core.error.ServerException
import com.example.coursey.core.network.NetworkInfo
import com.example.coursey.core.util.Result
import com.example.coursey.features.course.data.datasource.CourseLocalDataSource
import com.example.coursey.features.course.data.datasource.CourseRemoteDataSource
import com.example.coursey.features.course.domain.entity.Course
import com.example.coursey.features.course.domain.entity.CourseDetails
import com.example.coursey.features.course.domain.entity.Lesson
import com.example.coursey.features.course.domain.repository.CourseRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class CourseRepositoryImpl(
    private val remote: CourseRemoteDataSource,
    private val local: CourseLocalDataSource,
    private val networkInfo: NetworkInfo,
    private val requestTimeoutMillis: Long = 15_000L,
) : CourseRepository {

    override fun observeCourses(): Flow<List<Course>> =
        local.observeCourses().map { courses -> courses.map { it.toEntity() } }

    override suspend fun refreshCourses(): Result<List<Course>> = remoteCall {
        val courses = withTimeout(requestTimeoutMillis) { remote.getCourses() }
        local.saveCourses(courses)
        local.getCourses().map { it.toEntity() }
    }

    override fun observeCourseDetails(courseId: Int): Flow<CourseDetails?> =
        combine(local.observeCourse(courseId), local.observeLessons(courseId)) { course, lessons ->
            course?.let { CourseDetails(it.toEntity(), lessons.map { lesson -> lesson.toEntity() }) }
        }

    override suspend fun refreshCourseDetails(courseId: Int): Result<CourseDetails> = remoteCall {
        local.getCourse(courseId) ?: throw NotFoundException()
        val lessons = withTimeout(requestTimeoutMillis) { remote.getLessons(courseId) }
        local.saveLessons(courseId, lessons)
        val course = local.getCourse(courseId) ?: throw NotFoundException()
        CourseDetails(course.toEntity(), local.getLessons(courseId).map { it.toEntity() })
    }

    override suspend fun getLessons(courseId: Int): Result<List<Lesson>> = localCall {
        local.getLessons(courseId).map { it.toEntity() }
    }

    override suspend fun completeLesson(courseId: Int, lessonId: Int, progress: Int): Result<Unit> = localCall {
        local.markLessonCompleted(courseId, lessonId, progress)
    }

    override suspend fun updateCourseProgress(courseId: Int, progress: Int): Result<Unit> = localCall {
        local.updateCourseProgress(courseId, progress)
    }

    private suspend fun <T> remoteCall(block: suspend () -> T): Result<T> {
        if (!networkInfo.isConnected()) return Result.Error(noInternetFailure())
        return try {
            Result.Success(block())
        } catch (e: TimeoutCancellationException) {
            Result.Error(Failure.Timeout("The request timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: NetworkException) {
            Result.Error(noInternetFailure())
        } catch (e: IOException) {
            Result.Error(noInternetFailure())
        } catch (e: NotFoundException) {
            Result.Error(Failure.NotFound("This course is no longer available."))
        } catch (e: ServerException) {
            Result.Error(Failure.Server("Something went wrong on our side. Please try again."))
        } catch (e: CacheException) {
            Result.Error(Failure.Cache("Unable to save data on this device."))
        } catch (e: Exception) {
            Result.Error(Failure.Unknown("Something went wrong. Please try again."))
        }
    }

    private suspend fun <T> localCall(block: suspend () -> T): Result<T> = try {
        Result.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: NotFoundException) {
        Result.Error(Failure.NotFound("Item not found."))
    } catch (e: CacheException) {
        Result.Error(Failure.Cache("Unable to save your progress. Please try again."))
    } catch (e: Exception) {
        Result.Error(Failure.Unknown("Something went wrong. Please try again."))
    }

    private fun noInternetFailure() =
        Failure.Network("No internet connection. Please connect and try again.")
}
