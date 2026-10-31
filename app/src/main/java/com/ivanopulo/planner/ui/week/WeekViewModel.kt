package com.ivanopulo.planner.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ivanopulo.planner.PlannerApp
import com.ivanopulo.planner.data.TaskRepository
import com.ivanopulo.planner.domain.Task
import com.ivanopulo.planner.domain.TaskColor
import com.ivanopulo.planner.domain.Weeks
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeekViewModel(private val tasks: TaskRepository) : ViewModel(), TaskActions, TaskMenuActions {

    // Pager pages are counted from this week; it stays fixed for the process lifetime.
    val anchorMonday: LocalDate = Weeks.mondayOf(LocalDate.now())

    private val _editing = MutableStateFlow<EditTarget?>(null)
    val editing: StateFlow<EditTarget?> = _editing.asStateFlow()

    private val _menuTask = MutableStateFlow<Task?>(null)
    val menuTask: StateFlow<Task?> = _menuTask.asStateFlow()

    private val _datePickerTask = MutableStateFlow<Task?>(null)
    val datePickerTask: StateFlow<Task?> = _datePickerTask.asStateFlow()

    private val _deletions = Channel<Task>(Channel.BUFFERED)
    val deletions: Flow<Task> = _deletions.receiveAsFlow()

    val someday: StateFlow<List<Task>> = tasks.observeSomeday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun week(monday: LocalDate): Flow<Map<LocalDate, List<Task>>> = tasks.observeWeek(monday)

    override fun startNew(date: LocalDate?) {
        _editing.value = EditTarget.New(date)
    }

    override fun startEdit(taskId: String) {
        _editing.value = EditTarget.Existing(taskId)
    }

    override fun submitNew(date: LocalDate?, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { tasks.add(date, trimmed) }
    }

    override fun finishNew(date: LocalDate?, title: String) {
        submitNew(date, title)
        _editing.compareAndSet(EditTarget.New(date), null)
    }

    override fun finishEdit(taskId: String, title: String) {
        val trimmed = title.trim()
        viewModelScope.launch {
            if (trimmed.isEmpty()) tasks.delete(taskId) else tasks.rename(taskId, trimmed)
        }
        _editing.compareAndSet(EditTarget.Existing(taskId), null)
    }

    override fun toggleDone(task: Task) {
        viewModelScope.launch { tasks.setDone(task.id, !task.done) }
    }

    override fun openMenu(task: Task) {
        _menuTask.value = task
    }

    fun dismissMenu() {
        _menuTask.value = null
    }

    override fun setColor(task: Task, color: TaskColor?) {
        viewModelScope.launch { tasks.setColor(task.id, color) }
    }

    override fun moveToNextDay(task: Task) {
        task.date?.let { moveTo(task, it.plusDays(1)) }
    }

    override fun moveTo(task: Task, date: LocalDate?) {
        viewModelScope.launch { tasks.move(task.id, date) }
    }

    override fun pickDate(task: Task) {
        _datePickerTask.value = task
    }

    fun dismissDatePicker() {
        _datePickerTask.value = null
    }

    override fun delete(task: Task) {
        viewModelScope.launch { tasks.delete(task.id) }
        _deletions.trySend(task)
    }

    fun restore(task: Task) {
        viewModelScope.launch { tasks.restore(task.id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                WeekViewModel((this[APPLICATION_KEY] as PlannerApp).container.taskRepository)
            }
        }
    }
}
