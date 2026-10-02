package com.example.coursey.features.course.data.datasource

import com.example.coursey.core.error.NotFoundException
import com.example.coursey.core.error.ServerException
import com.example.coursey.core.mock.AssetReader
import com.example.coursey.features.course.data.model.CourseModel
import com.example.coursey.features.course.data.model.LessonModel
import kotlinx.coroutines.delay
import org.json.JSONException
import java.io.FileNotFoundException
import java.io.IOException

interface CourseRemoteDataSource {
    suspend fun getCourses(): List<CourseModel>
    suspend fun getLessons(courseId: Int): List<LessonModel>
}

class CourseMockRemoteDataSource(
    private val assetReader: AssetReader,
    private val coursesPath: String,
    private val lessonsPath: (Int) -> String,
    private val networkDelayMillis: Long = 1_200L,
) : CourseRemoteDataSource {

    override suspend fun getCourses(): List<CourseModel> {
        delay(networkDelayMillis)
        val json = try {
            read(coursesPath)
        } catch (e: FileNotFoundException) {
            throw ServerException("Courses endpoint not found")
        }
        return parse { CourseModel.listFromJson(json) }
    }

    override suspend fun getLessons(courseId: Int): List<LessonModel> {
        delay(networkDelayMillis)
        val json = try {
            read(lessonsPath(courseId))
        } catch (e: FileNotFoundException) {
            throw NotFoundException("No lessons found for course $courseId")
        }
        return parse { LessonModel.listFromJson(json, courseId) }
    }

    private suspend fun read(path: String): String = try {
        assetReader.readText(path)
    } catch (e: FileNotFoundException) {
        throw e
    } catch (e: IOException) {
        throw ServerException("Unable to read $path")
    }

    private inline fun <T> parse(block: () -> T): T = try {
        block()
    } catch (e: JSONException) {
        throw ServerException("Malformed response")
    }
}
