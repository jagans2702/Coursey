package com.example.coursey.features.course.data.datasource

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import com.example.coursey.core.database.AppDatabase
import com.example.coursey.core.database.CourseTable
import com.example.coursey.core.database.LessonTable
import com.example.coursey.core.error.CacheException
import com.example.coursey.core.error.NotFoundException
import com.example.coursey.core.session.UserDataCleaner
import com.example.coursey.features.course.data.model.CourseModel
import com.example.coursey.features.course.data.model.LessonModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

interface CourseLocalDataSource : UserDataCleaner {
    fun observeCourses(): Flow<List<CourseModel>>
    fun observeCourse(courseId: Int): Flow<CourseModel?>
    fun observeLessons(courseId: Int): Flow<List<LessonModel>>

    suspend fun getCourses(): List<CourseModel>
    suspend fun getCourse(courseId: Int): CourseModel?
    suspend fun getLessons(courseId: Int): List<LessonModel>

    suspend fun saveCourses(courses: List<CourseModel>)
    suspend fun saveLessons(courseId: Int, lessons: List<LessonModel>)
    suspend fun markLessonCompleted(courseId: Int, lessonId: Int, progress: Int)
    suspend fun updateCourseProgress(courseId: Int, progress: Int)
}

class CourseLocalDataSourceImpl(
    private val database: AppDatabase,
) : CourseLocalDataSource {

    private val changes = MutableStateFlow(0L)

    override fun observeCourses(): Flow<List<CourseModel>> =
        changes.map { read { queryCourses() } }.flowOn(Dispatchers.IO)

    override fun observeCourse(courseId: Int): Flow<CourseModel?> =
        changes.map { read { queryCourse(courseId) } }.flowOn(Dispatchers.IO)

    override fun observeLessons(courseId: Int): Flow<List<LessonModel>> =
        changes.map { read { queryLessons(courseId) } }.flowOn(Dispatchers.IO)

    override suspend fun getCourses(): List<CourseModel> = io { read { queryCourses() } }

    override suspend fun getCourse(courseId: Int): CourseModel? = io { read { queryCourse(courseId) } }

    override suspend fun getLessons(courseId: Int): List<LessonModel> = io { read { queryLessons(courseId) } }

    override suspend fun saveCourses(courses: List<CourseModel>) = write { db ->
        courses.forEachIndexed { index, course ->
            val existingProgress = db.singleInt(
                "SELECT ${CourseTable.PROGRESS} FROM ${CourseTable.NAME} WHERE ${CourseTable.ID} = ?",
                course.id,
            )
            val values = ContentValues().apply {
                put(CourseTable.TITLE, course.title)
                put(CourseTable.INSTRUCTOR, course.instructor)
                put(CourseTable.LESSONS_COUNT, course.lessons)
                put(CourseTable.POSITION, index)
                put(CourseTable.PROGRESS, maxOf(existingProgress ?: 0, course.progress))
            }
            if (existingProgress == null) {
                values.put(CourseTable.ID, course.id)
                db.insertOrThrow(CourseTable.NAME, null, values)
            } else {
                db.update(CourseTable.NAME, values, "${CourseTable.ID} = ?", arrayOf(course.id.toString()))
            }
        }
        db.deleteMissing(CourseTable.NAME, CourseTable.ID, courses.map { it.id })
    }

    override suspend fun saveLessons(courseId: Int, lessons: List<LessonModel>): Unit = write { db ->
        if (db.singleInt("SELECT 1 FROM ${CourseTable.NAME} WHERE ${CourseTable.ID} = ?", courseId) == null) {
            throw NotFoundException("Course $courseId not found")
        }
        lessons.forEachIndexed { index, lesson ->
            val existingCompleted = db.singleInt(
                "SELECT ${LessonTable.COMPLETED} FROM ${LessonTable.NAME} " +
                    "WHERE ${LessonTable.COURSE_ID} = ? AND ${LessonTable.ID} = ?",
                courseId,
                lesson.id,
            )
            val values = ContentValues().apply {
                put(LessonTable.TITLE, lesson.title)
                put(LessonTable.POSITION, index)
                put(LessonTable.COMPLETED, if (existingCompleted == 1 || lesson.completed) 1 else 0)
            }
            if (existingCompleted == null) {
                values.put(LessonTable.COURSE_ID, courseId)
                values.put(LessonTable.ID, lesson.id)
                db.insertOrThrow(LessonTable.NAME, null, values)
            } else {
                db.update(
                    LessonTable.NAME,
                    values,
                    "${LessonTable.COURSE_ID} = ? AND ${LessonTable.ID} = ?",
                    arrayOf(courseId.toString(), lesson.id.toString()),
                )
            }
        }
        val keepIds = lessons.map { it.id }
        val where = if (keepIds.isEmpty()) {
            "${LessonTable.COURSE_ID} = ?"
        } else {
            "${LessonTable.COURSE_ID} = ? AND ${LessonTable.ID} NOT IN (${keepIds.joinToString(",")})"
        }
        db.delete(LessonTable.NAME, where, arrayOf(courseId.toString()))
    }

    override suspend fun markLessonCompleted(courseId: Int, lessonId: Int, progress: Int) = write { db ->
        val lessonValues = ContentValues().apply { put(LessonTable.COMPLETED, 1) }
        val updated = db.update(
            LessonTable.NAME,
            lessonValues,
            "${LessonTable.COURSE_ID} = ? AND ${LessonTable.ID} = ?",
            arrayOf(courseId.toString(), lessonId.toString()),
        )
        if (updated == 0) throw NotFoundException("Lesson $lessonId not found")
        db.updateProgress(courseId, progress)
    }

    override suspend fun updateCourseProgress(courseId: Int, progress: Int) = write { db ->
        db.updateProgress(courseId, progress)
    }

    override suspend fun clearUserData() = write { db ->
        db.delete(LessonTable.NAME, null, null)
        db.delete(CourseTable.NAME, null, null)
        Unit
    }

    private fun queryCourses(): List<CourseModel> =
        database.readableDatabase.query(
            CourseTable.NAME, null, null, null, null, null, "${CourseTable.POSITION} ASC",
        ).use { cursor -> cursor.mapAll { it.toCourseModel() } }

    private fun queryCourse(courseId: Int): CourseModel? =
        database.readableDatabase.query(
            CourseTable.NAME, null, "${CourseTable.ID} = ?", arrayOf(courseId.toString()), null, null, null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toCourseModel() else null }

    private fun queryLessons(courseId: Int): List<LessonModel> =
        database.readableDatabase.query(
            LessonTable.NAME,
            null,
            "${LessonTable.COURSE_ID} = ?",
            arrayOf(courseId.toString()),
            null,
            null,
            "${LessonTable.POSITION} ASC",
        ).use { cursor -> cursor.mapAll { it.toLessonModel() } }

    private fun SQLiteDatabase.updateProgress(courseId: Int, progress: Int) {
        val values = ContentValues().apply { put(CourseTable.PROGRESS, progress.coerceIn(0, 100)) }
        val updated = update(CourseTable.NAME, values, "${CourseTable.ID} = ?", arrayOf(courseId.toString()))
        if (updated == 0) throw NotFoundException("Course $courseId not found")
    }

    private fun SQLiteDatabase.singleInt(sql: String, vararg args: Int): Int? =
        rawQuery(sql, args.map { it.toString() }.toTypedArray()).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else null
        }

    private fun SQLiteDatabase.deleteMissing(table: String, idColumn: String, keepIds: List<Int>) {
        if (keepIds.isEmpty()) {
            delete(table, null, null)
        } else {
            delete(table, "$idColumn NOT IN (${keepIds.joinToString(",")})", null)
        }
    }

    private fun Cursor.toCourseModel() = CourseModel(
        id = getInt(getColumnIndexOrThrow(CourseTable.ID)),
        title = getString(getColumnIndexOrThrow(CourseTable.TITLE)),
        instructor = getString(getColumnIndexOrThrow(CourseTable.INSTRUCTOR)),
        progress = getInt(getColumnIndexOrThrow(CourseTable.PROGRESS)),
        lessons = getInt(getColumnIndexOrThrow(CourseTable.LESSONS_COUNT)),
    )

    private fun Cursor.toLessonModel() = LessonModel(
        id = getInt(getColumnIndexOrThrow(LessonTable.ID)),
        courseId = getInt(getColumnIndexOrThrow(LessonTable.COURSE_ID)),
        title = getString(getColumnIndexOrThrow(LessonTable.TITLE)),
        completed = getInt(getColumnIndexOrThrow(LessonTable.COMPLETED)) == 1,
    )

    private inline fun <T> Cursor.mapAll(transform: (Cursor) -> T): List<T> {
        val items = ArrayList<T>(count)
        while (moveToNext()) items.add(transform(this))
        return items
    }

    private inline fun <T> read(block: () -> T): T = try {
        block()
    } catch (e: SQLException) {
        throw CacheException(e.message ?: "Database read failed")
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    private suspend fun <T> write(block: (SQLiteDatabase) -> T): T {
        val result = withContext(Dispatchers.IO) {
            try {
                val db = database.writableDatabase
                db.beginTransaction()
                try {
                    val value = block(db)
                    db.setTransactionSuccessful()
                    value
                } finally {
                    db.endTransaction()
                }
            } catch (e: SQLException) {
                throw CacheException(e.message ?: "Database write failed")
            }
        }
        changes.update { it + 1 }
        return result
    }
}
