package com.ivanopulo.planner.ui.week

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivanopulo.planner.domain.Task

private val LineMinHeight = 48.dp
private val LineTextPadding = Modifier.padding(top = 12.dp, bottom = 12.dp, end = 8.dp)

@Composable
fun Modifier.ruledLine(): Modifier {
    val color = MaterialTheme.colorScheme.outlineVariant
    return fillMaxWidth()
        .heightIn(min = LineMinHeight)
        .drawBehind {
            val y = size.height - 0.5.dp.toPx()
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
}

@Composable
fun TaskLine(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.ruledLine().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheckbox(checked = task.done, onToggle = onToggle)
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (task.done) colors.onSurfaceVariant else colors.onBackground,
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = LineTextPadding,
        )
    }
}

@Composable
fun EmptyLine(onClick: () -> Unit) {
    Box(Modifier.ruledLine().clickable(onClick = onClick))
}

/**
 * Inline editor for a line. [onSubmit] handles the keyboard "Done" action and returns true to keep
 * the editor open with a cleared field; [onClose] fires exactly once with the final text when the
 * editor loses focus or leaves the composition.
 */
@Composable
fun EditorLine(
    initialText: String,
    done: Boolean,
    onSubmit: (String) -> Boolean,
    onClose: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var value by remember {
        mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
    }
    var hadFocus by remember { mutableStateOf(false) }
    val closeState = remember { CloseOnce() }
    val latestText by rememberUpdatedState(value.text)
    val latestOnClose by rememberUpdatedState(onClose)
    val close = { closeState.run { latestOnClose(latestText) } }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    DisposableEffect(Unit) { onDispose { close() } }

    Row(modifier = Modifier.ruledLine(), verticalAlignment = Alignment.CenterVertically) {
        TaskCheckbox(checked = done, onToggle = {}, enabled = false)
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onBackground),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (onSubmit(value.text)) value = TextFieldValue("") else focusManager.clearFocus()
            }),
            modifier = Modifier
                .weight(1f)
                .then(LineTextPadding)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.isFocused) hadFocus = true else if (hadFocus) close()
                },
        )
    }
}

private class CloseOnce {
    private var closed = false

    fun run(block: () -> Unit) {
        if (closed) return
        closed = true
        block()
    }
}

@Composable
fun TaskCheckbox(checked: Boolean, onToggle: () -> Unit, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val boxColor = if (checked) colors.onSurfaceVariant else colors.onBackground.copy(alpha = 0.55f)
    val checkColor = colors.background
    Box(
        modifier = Modifier
            .size(44.dp)
            .toggleable(
                value = checked,
                interactionSource = null,
                indication = ripple(bounded = false, radius = 20.dp),
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val corner = CornerRadius(4.dp.toPx())
            if (checked) {
                drawRoundRect(boxColor, cornerRadius = corner)
                val check = Path().apply {
                    moveTo(size.width * 0.24f, size.height * 0.52f)
                    lineTo(size.width * 0.43f, size.height * 0.70f)
                    lineTo(size.width * 0.77f, size.height * 0.32f)
                }
                drawPath(
                    check,
                    checkColor,
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            } else {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = if (enabled) boxColor else boxColor.copy(alpha = 0.3f),
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = corner,
                    style = Stroke(stroke),
                )
            }
        }
    }
}

