package com.ivanopulo.planner.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LineMinHeight = 48.dp

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
fun BlockHeader(
    accent: Color = MaterialTheme.colorScheme.onBackground,
    content: @Composable RowScope.() -> Unit,
) {
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
        content = content,
    )
}
