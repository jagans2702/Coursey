package com.example.coursey.features.course.domain.entity

data class Lesson(
    val id: Int,
    val courseId: Int,
    val title: String,
    val completed: Boolean,
)
