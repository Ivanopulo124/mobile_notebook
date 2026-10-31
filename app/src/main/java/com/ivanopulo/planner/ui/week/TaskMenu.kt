package com.ivanopulo.planner.ui.week

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivanopulo.planner.R
import com.ivanopulo.planner.domain.Task
import com.ivanopulo.planner.domain.TaskColor
import com.ivanopulo.planner.ui.theme.HighlightText
import com.ivanopulo.planner.ui.theme.highlightColor
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskMenuSheet(
    task: Task,
    today: LocalDate,
    actions: TaskMenuActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val closeThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            action()
            onDismiss()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            )
            ColorRow(selected = task.color, onSelect = { closeThen { actions.setColor(task, it) } })
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            if (task.date != null) {
                MenuItem(R.drawable.ic_arrow_forward, R.string.menu_next_day) {
                    closeThen { actions.moveToNextDay(task) }
                }
            } else {
                MenuItem(R.drawable.ic_today, R.string.menu_schedule_today) {
                    closeThen { actions.moveTo(task, today) }
                }
            }
            MenuItem(R.drawable.ic_event, R.string.menu_move_to_date) {
                closeThen { actions.pickDate(task) }
            }
            if (task.date != null) {
                MenuItem(R.drawable.ic_inbox, R.string.menu_move_to_someday) {
                    closeThen { actions.moveTo(task, null) }
                }
            }
            MenuItem(R.drawable.ic_delete, R.string.menu_delete, MaterialTheme.colorScheme.error) {
                closeThen { actions.delete(task) }
            }
        }
    }
}

@Composable
private fun ColorRow(selected: TaskColor?, onSelect: (TaskColor?) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        Swatch(
            fill = null,
            selected = selected == null,
            label = stringResource(R.string.color_none),
            onClick = { onSelect(null) },
        )
        TaskColor.entries.forEach { color ->
            Swatch(
                fill = highlightColor(color),
                selected = selected == color,
                label = stringResource(color.label()),
                onClick = { onSelect(color) },
            )
        }
    }
}

@Composable
private fun Swatch(fill: Color?, selected: Boolean, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(if (fill != null) Modifier.background(fill) else Modifier)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.onBackground else colors.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick, role = Role.RadioButton)
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            selected && fill != null -> Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = HighlightText,
                modifier = Modifier.size(20.dp),
            )
            fill == null -> Canvas(Modifier.size(20.dp)) {
                drawLine(
                    color = colors.onSurfaceVariant,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }
    }
}

@Composable
private fun MenuItem(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint)
        Spacer(Modifier.width(20.dp))
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToDateDialog(initial: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // The Material date picker works with UTC midnight millis.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    // The default headline uses onSurfaceVariant, which is too faint in this palette.
    val pickerColors = DatePickerDefaults.colors(
        headlineContentColor = MaterialTheme.colorScheme.onBackground,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = pickerColors,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let {
                        onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                },
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    ) {
        DatePicker(state = state, colors = pickerColors)
    }
}

private fun TaskColor.label() = when (this) {
    TaskColor.YELLOW -> R.string.color_yellow
    TaskColor.GREEN -> R.string.color_green
    TaskColor.BLUE -> R.string.color_blue
    TaskColor.PINK -> R.string.color_pink
    TaskColor.ORANGE -> R.string.color_orange
}
