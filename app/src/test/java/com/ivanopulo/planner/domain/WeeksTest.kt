package com.ivanopulo.planner.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeeksTest {
    private val anchor = LocalDate.of(2026, 10, 26)

    @Test
    fun mondayOfReturnsSameDayForMonday() {
        assertEquals(anchor, Weeks.mondayOf(anchor))
    }

    @Test
    fun mondayOfSundayIsPreviousMonday() {
        assertEquals(anchor, Weeks.mondayOf(LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun pageAndMondayAreInverse() {
        val date = LocalDate.of(2027, 3, 17)
        val page = Weeks.pageFor(date, anchor)
        assertEquals(Weeks.mondayOf(date), Weeks.mondayForPage(page, anchor))
    }

    @Test
    fun anchorWeekIsAnchorPage() {
        assertEquals(Weeks.ANCHOR_PAGE, Weeks.pageFor(LocalDate.of(2026, 10, 31), anchor))
        assertEquals(Weeks.ANCHOR_PAGE - 1, Weeks.pageFor(LocalDate.of(2026, 10, 25), anchor))
    }

    @Test
    fun weekSpanningMonthsBelongsToMonthOfThursday() {
        assertEquals(10, Weeks.representativeDay(anchor).monthValue)
        assertEquals(12, Weeks.representativeDay(LocalDate.of(2026, 11, 30)).monthValue)
    }
}
