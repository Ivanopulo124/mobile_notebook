package com.ivanopulo.planner.ui.week

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import com.ivanopulo.planner.domain.Weeks
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

fun LocalDate.dayLabel(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("d MMM", locale))

fun DayOfWeek.shortLabel(locale: Locale): String =
    getDisplayName(TextStyle.SHORT_STANDALONE, locale).capitalized(locale)

fun monthTitle(monday: LocalDate, locale: Locale): String {
    val day = Weeks.representativeDay(monday)
    return "${day.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).capitalized(locale)} ${day.year}"
}

private fun String.capitalized(locale: Locale) = replaceFirstChar { it.titlecase(locale) }
