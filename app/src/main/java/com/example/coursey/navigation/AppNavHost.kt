package com.example.coursey.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.coursey.core.di.AppContainer
import com.example.coursey.features.auth.presentation.login.LoginRoute
import com.example.coursey.features.auth.presentation.login.LoginViewModel
import com.example.coursey.features.auth.presentation.session.SessionState
import com.example.coursey.features.auth.presentation.session.SessionViewModel
import com.example.coursey.features.course.presentation.dashboard.CourseDashboardRoute
import com.example.coursey.features.course.presentation.dashboard.CourseDashboardViewModel
import com.example.coursey.features.course.presentation.details.CourseDetailsRoute
import com.example.coursey.features.course.presentation.details.CourseDetailsViewModel

@Composable
fun AppNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModel.factory(container.getLoggedInUserUseCase, container.logoutUseCase),
    )
    val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()

    if (sessionState == SessionState.Checking) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (sessionState == SessionState.LoggedIn) Routes.DASHBOARD else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.factory(container.loginUseCase),
            )
            LoginRoute(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.DASHBOARD) { entry ->
            val dashboardViewModel: CourseDashboardViewModel = viewModel(
                factory = CourseDashboardViewModel.factory(
                    container.observeCoursesUseCase,
                    container.refreshCoursesUseCase,
                ),
            )
            CourseDashboardRoute(
                viewModel = dashboardViewModel,
                onCourseClick = { courseId ->
                    if (entry.isResumed()) {
                        navController.navigate(Routes.courseDetails(courseId)) {
                            launchSingleTop = true
                        }
                    }
                },
                onLogoutClick = {
                    if (entry.isResumed()) {
                        sessionViewModel.logout {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.DASHBOARD) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                },
            )
        }
        composable(
            route = Routes.COURSE_DETAILS,
            arguments = listOf(navArgument(Routes.ARG_COURSE_ID) { type = NavType.IntType }),
        ) { entry ->
            val detailsViewModel: CourseDetailsViewModel = viewModel(
                factory = CourseDetailsViewModel.factory(
                    container.observeCourseDetailsUseCase,
                    container.refreshCourseDetailsUseCase,
                    container.completeLessonUseCase,
                ),
            )
            CourseDetailsRoute(
                viewModel = detailsViewModel,
                onBackClick = {
                    if (entry.isResumed()) navController.popBackStack()
                },
            )
        }
    }
}

private fun NavBackStackEntry.isResumed(): Boolean =
    lifecycle.currentState == Lifecycle.State.RESUMED
