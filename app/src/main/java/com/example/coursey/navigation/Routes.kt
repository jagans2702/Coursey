package com.example.coursey.navigation

import com.example.coursey.features.course.presentation.details.CourseDetailsViewModel

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"

    const val ARG_COURSE_ID = CourseDetailsViewModel.ARG_COURSE_ID
    const val COURSE_DETAILS = "course/{$ARG_COURSE_ID}"

    fun courseDetails(courseId: Int) = "course/$courseId"
}
