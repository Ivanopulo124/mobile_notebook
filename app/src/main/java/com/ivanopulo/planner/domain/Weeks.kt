package com.ivanopulo.planner.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

object Weeks {
    const val PAGE_COUNT = 10_000
    const val ANCHOR_PAGE = PAGE_COUNT / 2

    fun mondayOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun mondayForPage(page: Int, anchorMonday: LocalDate): LocalDate =
        anchorMonday.plusWeeks((page - ANCHOR_PAGE).toLong())

    fun pageFor(date: LocalDate, anchorMonday: LocalDate): Int =
        ANCHOR_PAGE + ChronoUnit.WEEKS.between(anchorMonday, mondayOf(date)).toInt()

    fun daysOf(monday: LocalDate): List<LocalDate> = (0L..6L).map(monday::plusDays)

    fun isoWeekNumber(monday: LocalDate): Int = monday.get(WeekFields.ISO.weekOfWeekBasedYear())

    // ISO 8601: a week belongs to the month (and year) that contains its Thursday.
    fun representativeDay(monday: LocalDate): LocalDate = monday.plusDays(3)
}
