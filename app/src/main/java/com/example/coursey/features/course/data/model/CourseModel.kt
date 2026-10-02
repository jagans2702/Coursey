package com.example.coursey.features.course.data.model

import com.example.coursey.features.course.domain.entity.Course
import org.json.JSONArray
import org.json.JSONObject

data class CourseModel(
    val id: Int,
    val title: String,
    val instructor: String,
    val progress: Int,
    val lessons: Int,
) {
    fun toEntity(): Course = Course(
        id = id,
        title = title,
        instructor = instructor,
        progress = progress.coerceIn(0, 100),
        lessonsCount = lessons.coerceAtLeast(0),
    )

    companion object {
        fun fromJson(json: JSONObject): CourseModel = CourseModel(
            id = json.getInt("id"),
            title = json.getString("title"),
            instructor = json.getString("instructor"),
            progress = json.getInt("progress"),
            lessons = json.getInt("lessons"),
        )

        fun listFromJson(json: String): List<CourseModel> {
            val array = JSONArray(json)
            return List(array.length()) { index -> fromJson(array.getJSONObject(index)) }
        }
    }
}
