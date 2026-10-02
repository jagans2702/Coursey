package com.example.coursey.features.course.data.model

import com.example.coursey.features.course.domain.entity.Lesson
import org.json.JSONArray
import org.json.JSONObject

data class LessonModel(
    val id: Int,
    val courseId: Int,
    val title: String,
    val completed: Boolean,
) {
    fun toEntity(): Lesson = Lesson(
        id = id,
        courseId = courseId,
        title = title,
        completed = completed,
    )

    companion object {
        fun fromJson(json: JSONObject, courseId: Int): LessonModel = LessonModel(
            id = json.getInt("id"),
            courseId = courseId,
            title = json.getString("title"),
            completed = json.optBoolean("completed", false),
        )

        fun listFromJson(json: String, courseId: Int): List<LessonModel> {
            val array = JSONArray(json)
            return List(array.length()) { index -> fromJson(array.getJSONObject(index), courseId) }
        }
    }
}
