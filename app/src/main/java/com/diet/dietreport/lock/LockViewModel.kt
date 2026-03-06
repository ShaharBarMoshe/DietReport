package com.diet.dietreport.lock

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diet.dietreport.AppError
import com.diet.dietreport.data.db.MealLogDao
import com.diet.dietreport.data.db.ReminderSlotDao
import com.diet.dietreport.data.db.SlotStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DR/Lock"

data class LockUiState(
    val remainingMs: Long,
    val done: Boolean = false,
    val error: AppError? = null,
)

class LockViewModel(
    val slotId: Long,
    private val slotDao: ReminderSlotDao,
    @Suppress("unused") private val mealLogDao: MealLogDao,
    val durationMs: Long = 600_000L,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState(remainingMs = durationMs))
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    @Volatile
    private var markedFailed = false

    init {
        startTimer()
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            val endMs = System.currentTimeMillis() + durationMs
            while (true) {
                val remaining = endMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    _uiState.update { it.copy(remainingMs = 0L, done = true) }
                    Log.d(TAG, "Lock timer expired for slot $slotId — slot status unchanged")
                    break
                }
                _uiState.update { it.copy(remainingMs = remaining) }
                delay(1_000L)
            }
        }
    }

    fun onUnlockEarly() = markFailed("unlock_early")

    fun onAppBackgrounded() = markFailed("backgrounded")

    private fun markFailed(reason: String) {
        if (markedFailed) return
        markedFailed = true
        timerJob?.cancel()
        viewModelScope.launch {
            try {
                slotDao.updateStatus(slotId, SlotStatus.FAILED)
                Log.d(TAG, "Slot $slotId marked failed: reason=$reason")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update slot $slotId status to failed", e)
                _uiState.update {
                    it.copy(error = AppError.DatabaseError("Failed to update slot status: ${e.message}", e))
                }
            }
            _uiState.update { it.copy(done = true) }
        }
    }
}
