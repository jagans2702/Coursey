package com.example.coursey.features.course.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coursey.core.error.Failure
import com.example.coursey.core.util.Result
import com.example.coursey.features.course.domain.entity.Course
import com.example.coursey.features.course.domain.usecase.ObserveCoursesUseCase
import com.example.coursey.features.course.domain.usecase.RefreshCoursesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CourseDashboardViewModel(
    private val observeCoursesUseCase: ObserveCoursesUseCase,
    private val refreshCoursesUseCase: RefreshCoursesUseCase,
) : ViewModel() {

    private data class Model(
        val courses: List<Course> = emptyList(),
        val isRefreshing: Boolean = false,
        val refreshFailure: Failure? = null,
        val hasLoadedOnce: Boolean = false,
        val hasReadCache: Boolean = false,
    )

    private val model = MutableStateFlow(Model())

    val uiState: StateFlow<CourseDashboardUiState> = model
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CourseDashboardUiState.Loading)

    private var refreshJob: Job? = null

    init {
        observeCourses()
        refresh()
    }

    fun onRetryClick() = refresh()

    fun onRefresh() = refresh()

    private fun observeCourses() {
        observeCoursesUseCase()
            .onEach { courses -> model.update { it.copy(courses = courses, hasReadCache = true) } }
            .catch {
                model.update {
                    it.copy(hasReadCache = true, refreshFailure = Failure.Cache("Unable to read saved courses."))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refresh() {
        if (refreshJob?.isActive == true) return
        model.update { it.copy(isRefreshing = true) }

        refreshJob = viewModelScope.launch {
            val result = refreshCoursesUseCase(Unit)
            model.update { state ->
                when (result) {
                    is Result.Success -> state.copy(
                        courses = result.data,
                        hasReadCache = true,
                        isRefreshing = false,
                        refreshFailure = null,
                        hasLoadedOnce = true,
                    )
                    is Result.Error -> state.copy(
                        isRefreshing = false,
                        refreshFailure = result.failure,
                        hasLoadedOnce = true,
                    )
                }
            }
        }
    }

    private fun toUiState(model: Model): CourseDashboardUiState {
        val failure = model.refreshFailure
        return when {
            model.courses.isNotEmpty() -> CourseDashboardUiState.Success(
                courses = model.courses,
                isRefreshing = model.isRefreshing,
                notice = failure?.let(::offlineNotice),
            )
            !model.hasReadCache || !model.hasLoadedOnce || model.isRefreshing -> CourseDashboardUiState.Loading
            failure != null -> CourseDashboardUiState.Error(failure.message)
            else -> CourseDashboardUiState.Empty
        }
    }

    private fun offlineNotice(failure: Failure): String = when (failure) {
        is Failure.Network -> "You're offline. Showing saved courses."
        else -> "Couldn't refresh. Showing saved courses."
    }

    companion object {
        fun factory(
            observeCoursesUseCase: ObserveCoursesUseCase,
            refreshCoursesUseCase: RefreshCoursesUseCase,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { CourseDashboardViewModel(observeCoursesUseCase, refreshCoursesUseCase) }
        }
    }
}
