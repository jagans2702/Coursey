package com.example.coursey.core.mock

object MockAssets {
    const val COURSES = "mock/courses.json"
    const val COURSES_EMPTY = "mock/courses_empty.json"

    fun lessons(courseId: Int) = "mock/lessons/course_$courseId.json"
}
