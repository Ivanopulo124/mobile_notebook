package com.ivanopulo.planner.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ivanopulo.planner.PlannerApp
import com.ivanopulo.planner.data.SettingsRepository
import com.ivanopulo.planner.data.TaskRepository
import com.ivanopulo.planner.data.ThemeMode
import com.ivanopulo.planner.domain.Task
import com.ivanopulo.planner.domain.Weeks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeekViewModel(
    private val tasks: TaskRepository,
    private val settings: SettingsRepository,
) : ViewModel(), TaskActions {

    // Pager pages are counted from this week; it stays fixed for the process lifetime.
    val anchorMonday: LocalDate = Weeks.mondayOf(LocalDate.now())

    private val _editing = MutableStateFlow<EditTarget?>(null)
    val editing: StateFlow<EditTarget?> = _editing.asStateFlow()

    val themeMode: StateFlow<ThemeMode> =
        settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun week(monday: LocalDate): Flow<Map<LocalDate, List<Task>>> = tasks.observeWeek(monday)

    fun cycleTheme() {
        viewModelScope.launch { settings.setThemeMode(themeMode.value.next()) }
    }

    override fun startNew(date: LocalDate) {
        _editing.value = EditTarget.New(date)
    }

    override fun startEdit(taskId: String) {
        _editing.value = EditTarget.Existing(taskId)
    }

    override fun submitNew(date: LocalDate, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { tasks.add(date, trimmed) }
    }

    override fun finishNew(date: LocalDate, title: String) {
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as PlannerApp).container
                WeekViewModel(container.taskRepository, container.settingsRepository)
            }
        }
    }
}
