package com.ivanopulo.planner.ui.week

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivanopulo.planner.R
import java.time.LocalDate

@Composable
fun WeekHeader(
    monday: LocalDate,
    showToday: Boolean,
    onToday: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val title = monthTitle(monday, currentLocale())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 30.sp),
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        // "Today" comes first so that the settings button keeps its position when it appears.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (showToday) {
                RoundIconButton(
                    icon = R.drawable.ic_today,
                    contentDescription = stringResource(R.string.cd_today),
                    container = colors.primary,
                    content = colors.onPrimary,
                    onClick = onToday,
                )
            }
            RoundIconButton(
                icon = R.drawable.ic_settings,
                contentDescription = stringResource(R.string.cd_settings),
                container = colors.surfaceVariant,
                content = colors.onBackground,
                onClick = onSettings,
            )
        }
    }
}

@Composable
private fun RoundIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier.size(22.dp),
        )
    }
}
