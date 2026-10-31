package com.ivanopulo.planner.domain

import java.time.LocalDate

data class Task(
    val id: String,
    val date: LocalDate?,
    val title: String,
    val done: Boolean,
)
