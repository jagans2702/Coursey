package com.example.coursey.features.course.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coursey.features.course.domain.entity.Course
import com.example.coursey.ui.theme.MyApplicationTheme

@Composable
fun CourseDashboardRoute(
    viewModel: CourseDashboardViewModel,
    onCourseClick: (Int) -> Unit,
    onLogoutClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CourseDashboardScreen(
        state = state,
        onCourseClick = onCourseClick,
        onRetryClick = viewModel::onRetryClick,
        onRefresh = viewModel::onRefresh,
        onLogoutClick = onLogoutClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDashboardScreen(
    state: CourseDashboardUiState,
    onCourseClick: (Int) -> Unit,
    onRetryClick: () -> Unit,
    onRefresh: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Dashboard") },
                actions = {
                    TextButton(onClick = onLogoutClick) { Text("Logout") }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state is CourseDashboardUiState.Success && state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state) {
                CourseDashboardUiState.Loading -> LoadingContent()
                CourseDashboardUiState.Empty -> EmptyContent()
                is CourseDashboardUiState.Error -> ErrorContent(state.message, onRetryClick)
                is CourseDashboardUiState.Success -> CourseList(state.courses, state.notice, onCourseClick)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    CenteredColumn {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Loading courses...", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyContent() {
    CenteredColumn {
        Text("No courses available.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Pull down to refresh.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
) {
    CenteredColumn {
        Text("Failed to load courses.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetryClick) { Text("Retry") }
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun CourseList(
    courses: List<Course>,
    notice: String?,
    onCourseClick: (Int) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (notice != null) {
            item(key = "notice") { NoticeBanner(notice) }
        }
        items(items = courses, key = { it.id }) { course ->
            CourseCard(course = course, onContinueClick = { onCourseClick(course.id) })
        }
    }
}

@Composable
private fun NoticeBanner(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun CourseCard(
    course: Course,
    onContinueClick: () -> Unit,
) {
    Card(
        onClick = onContinueClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = course.instructor,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            Text("Progress: ${course.progress}%", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { course.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${course.lessonsCount} Lessons",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onContinueClick) { Text("Continue") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CourseDashboardPreview() {
    MyApplicationTheme {
        CourseDashboardScreen(
            state = CourseDashboardUiState.Success(
                courses = listOf(
                    Course(1, "Python Programming", "John Smith", 65, 20),
                    Course(2, "Generative AI", "Sarah Williams", 40, 16),
                ),
                isRefreshing = false,
                notice = "You're offline. Showing saved courses.",
            ),
            onCourseClick = {},
            onRetryClick = {},
            onRefresh = {},
            onLogoutClick = {},
        )
    }
}
