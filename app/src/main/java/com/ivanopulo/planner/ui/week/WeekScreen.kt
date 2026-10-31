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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivanopulo.planner.domain.Task
import com.ivanopulo.planner.domain.Weeks
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun WeekScreen(viewModel: WeekViewModel = viewModel(factory = WeekViewModel.Factory)) {
    val anchor = viewModel.anchorMonday
    val pagerState = rememberPagerState(initialPage = Weeks.ANCHOR_PAGE) { Weeks.PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val today = rememberToday()
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    BackHandler(enabled = editing != null) { focusManager.clearFocus() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
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
                themeMode = themeMode,
                onPrevious = { scope.launch { pagerState.scrollBy(-1) } },
                onNext = { scope.launch { pagerState.scrollBy(1) } },
                onToday = { scope.launch { pagerState.animateScrollToPage(todayPage) } },
                onThemeClick = viewModel::cycleTheme,
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
                        editing = editing,
                        actions = viewModel,
                    )
                }
            }
        }
    }
}

private suspend fun PagerState.scrollBy(pages: Int) {
    animateScrollToPage((currentPage + pages).coerceIn(0, pageCount - 1))
}

@Composable
private fun WeekPage(
    monday: LocalDate,
    today: LocalDate,
    columns: Int,
    tasksByDay: Map<LocalDate, List<Task>>,
    editing: EditTarget?,
    actions: TaskActions,
) {
    val focusManager = LocalFocusManager.current
    val minLines = if (columns > 1) 5 else 3
    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
    ) {
        Weeks.daysOf(monday).chunked(columns).forEach { row ->
            // Days sharing a row get the same number of lines so the ruling stays aligned.
            val lines = maxOf(minLines, row.maxOf { (tasksByDay[it]?.size ?: 0) + 1 })
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { day ->
                    DayBlock(
                        date = day,
                        isToday = day == today,
                        tasks = tasksByDay[day].orEmpty(),
                        lineCount = lines,
                        editing = editing,
                        actions = actions,
                        modifier = Modifier.weight(1f),
                    )
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
