package com.ivanopulo.planner.ui.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val blockModifier = if (isToday) modifier.todayTint(colors.primaryContainer) else modifier
    TaskBlock(date, tasks, lineCount, editing, actions, blockModifier) {
        BlockHeader(accent) {
            Text(
                text = date.dayLabel(locale),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Spacer(Modifier.weight(1f))
            if (tasks.any { it.done }) ClearDoneButton { actions.clearDone(date) }
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
            Spacer(Modifier.weight(1f))
            if (tasks.any { it.done }) ClearDoneButton { actions.clearDone(null) }
        }
    }
}

// Smaller than the header text so the header height, and the ruling of neighbouring blocks, stays the same.
@Composable
private fun RowScope.ClearDoneButton(onClick: () -> Unit) {
    Icon(
        painter = painterResource(R.drawable.ic_clear_done),
        contentDescription = stringResource(R.string.cd_clear_done),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .padding(end = 8.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(2.dp)
            .size(22.dp),
    )
}

// Drawn slightly outside the block so today's text stays aligned with the other days.
private fun Modifier.todayTint(color: Color) = drawBehind {
    val inset = 10.dp.toPx()
    val top = 12.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(-inset, top),
        size = Size(size.width + 2 * inset, size.height - top + 8.dp.toPx()),
        cornerRadius = CornerRadius(16.dp.toPx()),
    )
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
