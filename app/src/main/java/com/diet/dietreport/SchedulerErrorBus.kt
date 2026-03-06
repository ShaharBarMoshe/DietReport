package com.diet.dietreport

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton channel for broadcasting scheduler errors to any observer (e.g. HomeViewModel).
 * ReminderScheduler posts here on failure; HomeViewModel collects and surfaces via uiState.
 */
object SchedulerErrorBus {
    private val _error = MutableStateFlow<AppError.SchedulerError?>(null)
    val error: StateFlow<AppError.SchedulerError?> = _error.asStateFlow()

    fun post(error: AppError.SchedulerError) { _error.value = error }
    fun clear() { _error.value = null }
}
