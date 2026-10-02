package com.example.coursey.features.course.domain.entity

data class Course(
    val id: Int,
    val title: String,
    val instructor: String,
    val progress: Int,
    val lessonsCount: Int,
)
