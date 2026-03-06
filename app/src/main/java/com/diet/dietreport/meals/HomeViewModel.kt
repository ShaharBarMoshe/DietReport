package com.diet.dietreport.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diet.dietreport.AppError
import com.diet.dietreport.SchedulerErrorBus
import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.ReminderSlotDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class HomeUiState(
    val slots: List<ReminderSlot> = emptyList(),
    val error: AppError? = null,
)

class HomeViewModel(slotDao: ReminderSlotDao) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = run {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60_000L
        combine(
            slotDao.slotsForRangeFlow(dayStart, dayEnd),
            SchedulerErrorBus.error,
        ) { slots, schedulerError ->
            HomeUiState(slots = slots, error = schedulerError)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
    }

    fun clearError() = SchedulerErrorBus.clear()
}
