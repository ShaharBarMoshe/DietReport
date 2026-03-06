package com.diet.dietreport.meals

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diet.dietreport.AppError
import com.diet.dietreport.data.db.LogSource
import com.diet.dietreport.data.db.MealLog
import com.diet.dietreport.data.db.MealLogDao
import com.diet.dietreport.data.db.ReminderSlotDao
import com.diet.dietreport.data.db.SlotStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "DR/Meals"

data class LogMealUiState(
    val photoPath: String? = null,
    val photoSource: String? = null,
    val isConfirmed: Boolean = false,
    val error: AppError? = null,
)

class LogMealViewModel(
    val slotId: Long,
    private val slotDao: ReminderSlotDao,
    private val mealLogDao: MealLogDao,
    private val filesDir: File,
    initialState: LogMealUiState = LogMealUiState(),
) : ViewModel() {

    companion object {
        const val SUCCESS_WINDOW_MS = 30 * 60 * 1000L
    }

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<LogMealUiState> = _uiState.asStateFlow()

    fun onPhotoCaptured(path: String, source: String) {
        _uiState.update { it.copy(photoPath = path, photoSource = source, error = null) }
    }

    fun clearPhoto() {
        _uiState.update { it.copy(photoPath = null, photoSource = null) }
    }

    fun onCameraError(e: Exception) {
        Log.e(TAG, "Camera capture failed for slot $slotId", e)
        _uiState.update { it.copy(error = AppError.CameraError("Camera capture failed: ${e.message}", e)) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun confirm() {
        val state = _uiState.value
        val photoPath = state.photoPath ?: return

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                mealLogDao.insert(
                    MealLog(
                        reminderSlotId = slotId,
                        photoPath = photoPath,
                        loggedAt = now,
                        source = state.photoSource ?: LogSource.CAMERA,
                    )
                )
                val slot = slotDao.getById(slotId)
                if (slot != null) {
                    val isOnTime = now <= slot.scheduledAt + SUCCESS_WINDOW_MS
                    val status = if (isOnTime) SlotStatus.SUCCESS else SlotStatus.FAILED
                    slotDao.updateStatus(slotId, status)
                    Log.d(TAG, "Meal logged: slotId=$slotId source=${state.photoSource} onTime=$isOnTime")
                }
                _uiState.update { it.copy(isConfirmed = true) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log meal for slot $slotId", e)
                _uiState.update {
                    it.copy(error = AppError.DatabaseError("Failed to save meal log: ${e.message}", e))
                }
            }
        }
    }
}
