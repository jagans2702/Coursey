package com.example.coursey.features.course.domain.entity

import com.example.coursey.features.course.domain.logic.ProgressCalculator

data class CourseDetails(
    val course: Course,
    val lessons: List<Lesson>,
) {
    val progress: Int
        get() = if (lessons.isEmpty()) course.progress else ProgressCalculator.calculate(lessons)

    val completedCount: Int
        get() = lessons.count { it.completed }
}
