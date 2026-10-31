package com.ivanopulo.planner.ui.week

import com.ivanopulo.planner.domain.Task
import java.time.LocalDate

interface TaskActions {
    fun startNew(date: LocalDate)
    fun startEdit(taskId: String)
    fun submitNew(date: LocalDate, title: String)
    fun finishNew(date: LocalDate, title: String)
    fun finishEdit(taskId: String, title: String)
    fun toggleDone(task: Task)
}
