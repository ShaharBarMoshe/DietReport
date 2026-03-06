package com.diet.dietreport.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diet.dietreport.AppError
import com.diet.dietreport.SchedulerErrorBus
import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.ReminderSlotDao
import com.diet.dietreport.data.db.SlotStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val slots: List<ReminderSlot> = emptyList(),
    val error: AppError? = null,
    val nowMs: Long = System.currentTimeMillis(),
)

class HomeViewModel(private val slotDao: ReminderSlotDao) : ViewModel() {

    companion object {
        const val LOG_WINDOW_BEFORE_MS = 5 * 60_000L   // 5 min before scheduled time
        const val LOG_WINDOW_AFTER_MS  = 30 * 60_000L  // 30 min after scheduled time
        const val HIDE_AFTER_MS        = 60 * 60_000L  // hide from home screen 1 h after scheduled
    }

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000L)
        }
    }

    val uiState: StateFlow<HomeUiState>

    init {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60_000L

        uiState = combine(
            slotDao.slotsForRangeFlow(dayStart, dayEnd),
            SchedulerErrorBus.error,
            ticker,
        ) { slots, schedulerError, now ->
            val visible = slots.filter { slot ->
                slot.status == SlotStatus.SUCCESS || now <= slot.scheduledAt + HIDE_AFTER_MS
            }
            HomeUiState(slots = visible, error = schedulerError, nowMs = now)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

        // Auto-mark PENDING slots past the log window as FAILED
        viewModelScope.launch {
            while (true) {
                slotDao.markExpiredAsFailed(System.currentTimeMillis() - LOG_WINDOW_AFTER_MS)
                delay(60_000L)
            }
        }
    }

    fun clearError() = SchedulerErrorBus.clear()
}
