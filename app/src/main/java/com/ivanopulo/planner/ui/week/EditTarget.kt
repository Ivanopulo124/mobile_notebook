package com.ivanopulo.planner.ui.week

import java.time.LocalDate

sealed interface EditTarget {
    // A null date adds to the "someday" list.
    data class New(val date: LocalDate?) : EditTarget
    data class Existing(val taskId: String) : EditTarget
}
