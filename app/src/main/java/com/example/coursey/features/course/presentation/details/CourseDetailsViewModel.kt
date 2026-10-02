package com.example.coursey.features.course.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coursey.core.error.Failure
import com.example.coursey.core.ui.UserMessage
import com.example.coursey.core.util.Result
import com.example.coursey.features.course.domain.entity.CourseDetails
import com.example.coursey.features.course.domain.usecase.CompleteLessonUseCase
import com.example.coursey.features.course.domain.usecase.ObserveCourseDetailsUseCase
import com.example.coursey.features.course.domain.usecase.RefreshCourseDetailsUseCase
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

class CourseDetailsViewModel(
    private val courseId: Int,
    private val observeCourseDetailsUseCase: ObserveCourseDetailsUseCase,
    private val refreshCourseDetailsUseCase: RefreshCourseDetailsUseCase,
    private val completeLessonUseCase: CompleteLessonUseCase,
) : ViewModel() {

    private data class Model(
        val details: CourseDetails? = null,
        val isRefreshing: Boolean = false,
        val refreshFailure: Failure? = null,
        val hasLoadedOnce: Boolean = false,
        val hasReadCache: Boolean = false,
        val completingLessonIds: Set<Int> = emptySet(),
        val message: UserMessage? = null,
    )

    private val model = MutableStateFlow(Model())

    val uiState: StateFlow<CourseDetailsUiState> = model
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CourseDetailsUiState.Loading)

    private var refreshJob: Job? = null
    private var nextMessageId = 0L

    init {
        observeDetails()
        refresh()
    }

    fun onRetryClick() = refresh()

    fun onLessonCompleteClick(lessonId: Int) {
        val current = model.value
        val visible = current.details?.withOptimisticCompleted(current.completingLessonIds)
        val lesson = visible?.lessons?.firstOrNull { it.id == lessonId } ?: return
        if (lesson.completed || lessonId in current.completingLessonIds) return

        // Optimistic state lives only in completingLessonIds; details stays untouched so that
        // any database snapshot arriving mid-write cannot clobber this row.
        model.update { it.copy(completingLessonIds = it.completingLessonIds + lessonId) }

        viewModelScope.launch {
            when (val result = completeLessonUseCase(CompleteLessonUseCase.Params(courseId, lessonId))) {
                // Keep the optimistic id on success. It is retired by acceptSource once the
                // database actually reports the lesson as completed, so an out-of-order
                // emission can never flash the row back to "Mark complete".
                is Result.Success -> Unit
                is Result.Error -> model.update { state ->
                    state.copy(
                        completingLessonIds = state.completingLessonIds - lessonId,
                        message = UserMessage(nextMessageId++, result.failure.message),
                    )
                }
            }
        }
    }

    fun onMessageShown(messageId: Long) {
        model.update { if (it.message?.id == messageId) it.copy(message = null) else it }
    }

    private fun observeDetails() {
        observeCourseDetailsUseCase(courseId)
            .onEach { details -> model.update { it.acceptSource(details).copy(hasReadCache = true) } }
            .catch {
                model.update {
                    it.copy(hasReadCache = true, refreshFailure = Failure.Cache("Unable to read saved course."))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refresh() {
        if (refreshJob?.isActive == true) return
        model.update { it.copy(isRefreshing = true) }

        refreshJob = viewModelScope.launch {
            val result = refreshCourseDetailsUseCase(courseId)
            model.update { state ->
                when (result) {
                    is Result.Success -> state.acceptSource(result.data).copy(
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

    private fun toUiState(model: Model): CourseDetailsUiState {
        val details = model.details?.withOptimisticCompleted(model.completingLessonIds)
        val failure = model.refreshFailure
        val stillLoading = !model.hasReadCache || !model.hasLoadedOnce || model.isRefreshing

        if (details == null) {
            return if (stillLoading) {
                CourseDetailsUiState.Loading
            } else {
                CourseDetailsUiState.Error(failure?.message ?: "Course not found.")
            }
        }

        return when {
            details.lessons.isEmpty() && stillLoading -> CourseDetailsUiState.Loading
            details.lessons.isEmpty() && failure != null -> CourseDetailsUiState.Error(lessonsUnavailable(failure))
            else -> CourseDetailsUiState.Content(
                title = details.course.title,
                progress = details.progress,
                completedCount = details.completedCount,
                lessons = details.lessons,
                completingLessonIds = model.completingLessonIds,
                notice = failure?.let(::offlineNotice),
                message = model.message,
            )
        }
    }

    private fun lessonsUnavailable(failure: Failure): String = when (failure) {
        is Failure.Network -> "Lessons aren't saved on this device yet. Connect to the internet and try again."
        else -> failure.message
    }

    private fun offlineNotice(failure: Failure): String = when (failure) {
        is Failure.Network -> "You're offline. Progress is saved on this device."
        else -> "Couldn't refresh. Showing saved lessons."
    }

    /**
     * Stores a fresh database/refresh snapshot as the single source of truth and retires the
     * optimistic ids that snapshot already agrees with. Ids it does not confirm yet stay in
     * [completingLessonIds], so the row keeps rendering as completed until the write really lands.
     */
    private fun Model.acceptSource(snapshot: CourseDetails?): Model {
        val confirmed = completingLessonIds.filter { lessonId ->
            snapshot?.lessons?.firstOrNull { it.id == lessonId }?.completed == true
        }
        return copy(
            details = snapshot,
            completingLessonIds = completingLessonIds - confirmed.toSet(),
        )
    }

    private fun CourseDetails.withOptimisticCompleted(ids: Set<Int>): CourseDetails =
        if (ids.isEmpty()) {
            this
        } else {
            copy(lessons = lessons.map { if (it.id in ids) it.copy(completed = true) else it })
        }

    companion object {
        const val ARG_COURSE_ID = "courseId"

        fun factory(
            observeCourseDetailsUseCase: ObserveCourseDetailsUseCase,
            refreshCourseDetailsUseCase: RefreshCourseDetailsUseCase,
            completeLessonUseCase: CompleteLessonUseCase,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val courseId = createSavedStateHandle().get<Int>(ARG_COURSE_ID) ?: -1
                CourseDetailsViewModel(
                    courseId = courseId,
                    observeCourseDetailsUseCase = observeCourseDetailsUseCase,
                    refreshCourseDetailsUseCase = refreshCourseDetailsUseCase,
                    completeLessonUseCase = completeLessonUseCase,
                )
            }
        }
    }
}
