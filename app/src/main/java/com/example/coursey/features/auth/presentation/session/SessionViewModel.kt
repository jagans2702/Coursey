package com.example.coursey.features.auth.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coursey.core.util.Result
import com.example.coursey.features.auth.domain.usecase.GetLoggedInUserUseCase
import com.example.coursey.features.auth.domain.usecase.LogoutUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Checking : SessionState
    data object LoggedIn : SessionState
    data object LoggedOut : SessionState
}

class SessionViewModel(
    private val getLoggedInUserUseCase: GetLoggedInUserUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Checking)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val result = getLoggedInUserUseCase(Unit)
            _state.value = if (result is Result.Success && result.data != null) {
                SessionState.LoggedIn
            } else {
                SessionState.LoggedOut
            }
        }
    }

    private var logoutJob: Job? = null

    fun logout(onLoggedOut: () -> Unit) {
        if (logoutJob?.isActive == true) return
        logoutJob = viewModelScope.launch {
            logoutUseCase(Unit)
            onLoggedOut()
        }
    }

    companion object {
        fun factory(
            getLoggedInUserUseCase: GetLoggedInUserUseCase,
            logoutUseCase: LogoutUseCase,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SessionViewModel(getLoggedInUserUseCase, logoutUseCase) }
        }
    }
}
