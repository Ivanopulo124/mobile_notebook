package com.ivanopulo.planner.ui.week

import com.ivanopulo.planner.domain.Task
import com.ivanopulo.planner.domain.TaskColor
import java.time.LocalDate

interface TaskActions {
    fun startNew(date: LocalDate?)
    fun startEdit(taskId: String)
    fun submitNew(date: LocalDate?, title: String)
    fun finishNew(date: LocalDate?, title: String)
    fun finishEdit(taskId: String, title: String)
    fun toggleDone(task: Task)
    fun openMenu(task: Task)
}

interface TaskMenuActions {
    fun setColor(task: Task, color: TaskColor?)
    fun moveToNextDay(task: Task)
    fun moveTo(task: Task, date: LocalDate?)
    fun pickDate(task: Task)
    fun delete(task: Task)
}
