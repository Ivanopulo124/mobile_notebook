package com.ivanopulo.planner.ui.week

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivanopulo.planner.R
import com.ivanopulo.planner.domain.Task
import com.ivanopulo.planner.domain.Weeks
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun WeekScreen(
    onOpenSettings: () -> Unit,
    viewModel: WeekViewModel = viewModel(factory = WeekViewModel.Factory),
) {
    val anchor = viewModel.anchorMonday
    val pagerState = rememberPagerState(initialPage = Weeks.ANCHOR_PAGE) { Weeks.PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val today = rememberToday()
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val someday by viewModel.someday.collectAsStateWithLifecycle()
    val menuTask by viewModel.menuTask.collectAsStateWithLifecycle()
    val datePickerTask by viewModel.datePickerTask.collectAsStateWithLifecycle()

    BackHandler(enabled = editing != null) { focusManager.clearFocus() }

    val deletedMessage = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.undo)
    LaunchedEffect(Unit) {
        viewModel.deletions.collect { task ->
            snackbarHostState.currentSnackbarData?.dismiss()
            launch {
                val result = snackbarHostState.showSnackbar(
                    message = deletedMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.restore(task)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
        ) {
            val visibleMonday = Weeks.mondayForPage(pagerState.currentPage, anchor)
            val todayPage = Weeks.pageFor(today, anchor)
            WeekHeader(
                monday = visibleMonday,
                showToday = pagerState.currentPage != todayPage,
                onToday = { scope.launch { pagerState.animateScrollToPage(todayPage) } },
                onSettings = onOpenSettings,
            )
            BoxWithConstraints {
                val columns = (maxWidth / 300.dp).toInt().coerceIn(1, 4)
                HorizontalPager(state = pagerState, beyondViewportPageCount = 1) { page ->
                    val monday = Weeks.mondayForPage(page, anchor)
                    val tasks by remember(monday) { viewModel.week(monday) }
                        .collectAsStateWithLifecycle(emptyMap())
                    WeekPage(
                        monday = monday,
                        today = today,
                        columns = columns,
                        tasksByDay = tasks,
                        someday = someday,
                        editing = editing,
                        isCurrentPage = page == pagerState.currentPage,
                        actions = viewModel,
                    )
                }
            }
        }
    }

    menuTask?.let { task ->
        TaskMenuSheet(task = task, today = today, actions = viewModel, onDismiss = viewModel::dismissMenu)
    }
    datePickerTask?.let { task ->
        MoveToDateDialog(
            initial = task.date ?: today,
            onConfirm = { viewModel.moveTo(task, it) },
            onDismiss = viewModel::dismissDatePicker,
        )
    }
}

private sealed interface Cell {
    data class Day(val date: LocalDate) : Cell
    data object Someday : Cell
}

@Composable
private fun WeekPage(
    monday: LocalDate,
    today: LocalDate,
    columns: Int,
    tasksByDay: Map<LocalDate, List<Task>>,
    someday: List<Task>,
    editing: EditTarget?,
    isCurrentPage: Boolean,
    actions: TaskActions,
) {
    val focusManager = LocalFocusManager.current
    val minLines = if (columns > 1) 5 else 3
    val cells = Weeks.daysOf(monday).map(Cell::Day) + Cell.Someday
    fun tasksOf(cell: Cell) = when (cell) {
        is Cell.Day -> tasksByDay[cell.date].orEmpty()
        Cell.Someday -> someday
    }
    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
    ) {
        cells.chunked(columns).forEach { row ->
            // Cells sharing a row get the same number of lines so the ruling stays aligned.
            val lines = maxOf(minLines, row.maxOf { tasksOf(it).size + 1 })
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { cell ->
                    val modifier = Modifier.weight(1f)
                    when (cell) {
                        is Cell.Day -> DayBlock(
                            date = cell.date,
                            isToday = cell.date == today,
                            tasks = tasksOf(cell),
                            lineCount = lines,
                            editing = editing,
                            actions = actions,
                            modifier = modifier,
                        )
                        Cell.Someday -> SomedayBlock(
                            tasks = someday,
                            lineCount = lines,
                            // Every composed page shows the same list; an editor on an off-screen
                            // page would steal focus from the visible one and close it.
                            editing = editing.takeIf { isCurrentPage },
                            actions = actions,
                            modifier = modifier,
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleResumeEffect(Unit) {
        today = LocalDate.now()
        onPauseOrDispose {}
    }
    return today
}
