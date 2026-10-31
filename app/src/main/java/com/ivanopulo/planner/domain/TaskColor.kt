package com.ivanopulo.planner.domain

// The id is persisted, so existing values must never be renumbered.
enum class TaskColor(val id: Int) {
    YELLOW(1),
    GREEN(2),
    BLUE(3),
    PINK(4),
    ORANGE(5);

    companion object {
        fun fromId(id: Int?): TaskColor? = entries.firstOrNull { it.id == id }
    }
}
