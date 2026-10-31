package com.ivanopulo.planner.ui.week

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivanopulo.planner.domain.Task
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
    Column(modifier) {
        DayHeader(date, isToday)
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

@Composable
private fun DayHeader(date: LocalDate, isToday: Boolean) {
    val colors = MaterialTheme.colorScheme
    val locale = currentLocale()
    val accent = if (isToday) colors.primary else colors.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .drawBehind {
                val stroke = 2.dp.toPx()
                val y = size.height - stroke / 2
                drawLine(accent, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            }
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
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
