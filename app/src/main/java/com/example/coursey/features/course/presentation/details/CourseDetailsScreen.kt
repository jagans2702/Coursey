package com.example.coursey.features.course.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.coursey.features.course.domain.entity.Lesson
import com.example.coursey.ui.theme.MyApplicationTheme

@Composable
fun CourseDetailsRoute(
    viewModel: CourseDetailsViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CourseDetailsScreen(
        state = state,
        onBackClick = onBackClick,
        onRetryClick = viewModel::onRetryClick,
        onLessonCompleteClick = viewModel::onLessonCompleteClick,
        onMessageShown = viewModel::onMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailsScreen(
    state: CourseDetailsUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onLessonCompleteClick: (Int) -> Unit,
    onMessageShown: (Long) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = (state as? CourseDetailsUiState.Content)?.message

    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
            message = message.text,
            withDismissAction = true,
            duration = SnackbarDuration.Short,
        )
        onMessageShown(message.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (state as? CourseDetailsUiState.Content)?.title ?: "Course Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) { Text("Back") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (state) {
            CourseDetailsUiState.Loading -> LoadingContent(modifier)
            is CourseDetailsUiState.Error -> ErrorContent(state.message, onRetryClick, modifier)
            is CourseDetailsUiState.Content -> DetailsContent(state, onLessonCompleteClick, modifier)
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Loading lessons...", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Failed to load course.", style = MaterialTheme.typography.titleMedium)
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
private fun DetailsContent(
    state: CourseDetailsUiState.Content,
    onLessonCompleteClick: (Int) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
    ) {
        item(key = "header") {
            ProgressHeader(
                title = state.title,
                progress = state.progress,
                completedCount = state.completedCount,
                totalCount = state.lessons.size,
            )
        }
        if (state.notice != null) {
            item(key = "notice") {
                Spacer(Modifier.height(12.dp))
                NoticeBanner(state.notice)
            }
        }
        item(key = "lessons_title") {
            Spacer(Modifier.height(24.dp))
            Text("Lessons", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        if (state.lessons.isEmpty()) {
            item(key = "no_lessons") {
                Text(
                    text = "No lessons available yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(items = state.lessons, key = { it.id }) { lesson ->
            LessonRow(
                lesson = lesson,
                isSaving = lesson.id in state.completingLessonIds,
                onCompleteClick = { onLessonCompleteClick(lesson.id) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ProgressHeader(
    title: String,
    progress: Int,
    completedCount: Int,
    totalCount: Int,
) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Progress",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("$progress%", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "$completedCount of $totalCount lessons completed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
private fun LessonRow(
    lesson: Lesson,
    isSaving: Boolean,
    onCompleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (lesson.completed) "✓" else "○",
            style = MaterialTheme.typography.titleMedium,
            color = if (lesson.completed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(lesson.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (lesson.completed) "Completed" else "Pending",
                style = MaterialTheme.typography.bodySmall,
                color = if (lesson.completed) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (!lesson.completed) {
            OutlinedButton(onClick = onCompleteClick, enabled = !isSaving) {
                Text("Mark complete")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CourseDetailsPreview() {
    MyApplicationTheme {
        CourseDetailsScreen(
            state = CourseDetailsUiState.Content(
                title = "Python Programming",
                progress = 50,
                completedCount = 2,
                lessons = listOf(
                    Lesson(1, 1, "Introduction", true),
                    Lesson(2, 1, "Variables & Data Types", true),
                    Lesson(3, 1, "Functions", false),
                    Lesson(4, 1, "OOP", false),
                ),
                completingLessonIds = emptySet(),
                notice = null,
                message = null,
            ),
            onBackClick = {},
            onRetryClick = {},
            onLessonCompleteClick = {},
            onMessageShown = {},
        )
    }
}
