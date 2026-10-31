package com.ivanopulo.planner.ui.week

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.ivanopulo.planner.R
import com.ivanopulo.planner.domain.Task
import com.ivanopulo.planner.ui.components.BlockHeader
import java.time.LocalDate

@Composable
fun DayBlock(
    date: LocalDate,
    isToday: Boolean,
    tasks: List<Task>,
    lineCount: Int,
    editing: EditTarget?,
    actions: TaskActions,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val locale = currentLocale()
    val accent = if (isToday) colors.primary else colors.onBackground
    TaskBlock(date, tasks, lineCount, editing, actions, modifier) {
        BlockHeader(accent) {
            Text(
                text = date.dayLabel(locale),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = date.dayOfWeek.shortLabel(locale),
                style = MaterialTheme.typography.titleLarge,
                color = if (isToday) colors.primary.copy(alpha = 0.6f) else colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SomedayBlock(
    tasks: List<Task>,
    lineCount: Int,
    editing: EditTarget?,
    actions: TaskActions,
    modifier: Modifier = Modifier,
) {
    TaskBlock(date = null, tasks, lineCount, editing, actions, modifier) {
        BlockHeader {
            Text(
                text = stringResource(R.string.someday),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun TaskBlock(
    date: LocalDate?,
    tasks: List<Task>,
    lineCount: Int,
    editing: EditTarget?,
    actions: TaskActions,
    modifier: Modifier,
    header: @Composable () -> Unit,
) {
    Column(modifier) {
        header()
        tasks.forEach { task ->
            key(task.id) {
                if (editing == EditTarget.Existing(task.id)) {
                    EditorLine(
                        initialText = task.title,
                        done = task.done,
                        onSubmit = { false },
                        onClose = { actions.finishEdit(task.id, it) },
                    )
                } else {
                    TaskLine(
                        task = task,
                        onToggle = { actions.toggleDone(task) },
                        onClick = { actions.startEdit(task.id) },
                        onLongClick = { actions.openMenu(task) },
                    )
                }
            }
        }
        val addingHere = editing == EditTarget.New(date)
        if (addingHere) {
            key("new") {
                EditorLine(
                    initialText = "",
                    done = false,
                    onSubmit = { text ->
                        actions.submitNew(date, text)
                        text.isNotBlank()
                    },
                    onClose = { actions.finishNew(date, it) },
                )
            }
        }
        val emptyLines = lineCount - tasks.size - if (addingHere) 1 else 0
        repeat(emptyLines.coerceAtLeast(0)) {
            EmptyLine(onClick = { actions.startNew(date) })
        }
    }
}
