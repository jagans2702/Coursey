package com.example.coursey.features.course.domain.logic

import com.example.coursey.features.course.domain.entity.Lesson

object ProgressCalculator {

    fun calculate(completedLessons: Int, totalLessons: Int): Int {
        if (totalLessons <= 0) return 0
        val completed = completedLessons.coerceIn(0, totalLessons)
        return completed * 100 / totalLessons
    }

    fun calculate(lessons: List<Lesson>): Int =
        calculate(lessons.count { it.completed }, lessons.size)
}
