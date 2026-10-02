package com.example.coursey.features.course.presentation.dashboard

import com.example.coursey.features.course.domain.entity.Course

sealed interface CourseDashboardUiState {
    data object Loading : CourseDashboardUiState

    data class Success(
        val courses: List<Course>,
        val isRefreshing: Boolean,
        val notice: String?,
    ) : CourseDashboardUiState

    data object Empty : CourseDashboardUiState

    data class Error(val message: String) : CourseDashboardUiState
}
