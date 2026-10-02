package com.example.coursey.features.course.presentation.details

import com.example.coursey.core.ui.UserMessage
import com.example.coursey.features.course.domain.entity.Lesson

sealed interface CourseDetailsUiState {
    data object Loading : CourseDetailsUiState

    data class Error(val message: String) : CourseDetailsUiState

    data class Content(
        val title: String,
        val progress: Int,
        val completedCount: Int,
        val lessons: List<Lesson>,
        val completingLessonIds: Set<Int>,
        val notice: String?,
        val message: UserMessage?,
    ) : CourseDetailsUiState
}
