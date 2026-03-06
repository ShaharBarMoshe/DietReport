package com.diet.dietreport.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diet.dietreport.AppError
import com.diet.dietreport.settings.data.Settings
import com.diet.dietreport.settings.data.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "DR/Settings"

data class SettingsUiState(
    val wakeTimeText: String = "07:00",
    val bedTimeText: String = "23:00",
    val delayText: String = "30",
    val ringtone: String = "default",
    val activityMonitorEnabled: Boolean = false,
    val wakeTimeError: String? = null,
    val bedTimeError: String? = null,
    val delayError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: AppError? = null,
    val onboardingComplete: Boolean = false,
)

sealed class SettingsNavEvent {
    data object ToHome : SettingsNavEvent()
}

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<SettingsNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<SettingsNavEvent> = _navEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        wakeTimeText = "%02d:%02d".format(settings.wakeHour, settings.wakeMinute),
                        bedTimeText = "%02d:%02d".format(settings.bedHour, settings.bedMinute),
                        delayText = settings.firstMealDelayMinutes.toString(),
                        ringtone = settings.ringtone,
                        activityMonitorEnabled = settings.activityMonitorEnabled,
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.isOnboardingComplete.collect { complete ->
                _uiState.update { it.copy(onboardingComplete = complete) }
            }
        }
    }

    fun onWakeTimeTextChange(text: String) {
        _uiState.update { it.copy(wakeTimeText = text, wakeTimeError = null) }
    }

    fun onBedTimeTextChange(text: String) {
        _uiState.update { it.copy(bedTimeText = text, bedTimeError = null) }
    }

    fun onDelayTextChange(text: String) {
        _uiState.update { it.copy(delayText = text, delayError = null) }
    }

    fun onRingtoneChange(ringtone: String) {
        _uiState.update { it.copy(ringtone = ringtone) }
    }

    fun onActivityMonitorToggle(enabled: Boolean) {
        _uiState.update { it.copy(activityMonitorEnabled = enabled) }
    }

    fun save() {
        val state = _uiState.value

        val (wakeH, wakeM) = parseTime(state.wakeTimeText) ?: run {
            _uiState.update { it.copy(wakeTimeError = "Invalid format, use HH:MM") }
            return
        }
        val (bedH, bedM) = parseTime(state.bedTimeText) ?: run {
            _uiState.update { it.copy(bedTimeError = "Invalid format, use HH:MM") }
            return
        }
        val delay = state.delayText.toIntOrNull() ?: run {
            _uiState.update { it.copy(delayError = "Enter a number 0–180") }
            return
        }

        val settings = Settings(
            wakeHour = wakeH,
            wakeMinute = wakeM,
            bedHour = bedH,
            bedMinute = bedM,
            firstMealDelayMinutes = delay,
            ringtone = state.ringtone,
            activityMonitorEnabled = state.activityMonitorEnabled,
        )

        val result = SettingsValidator.validate(settings)
        if (result.isFailure) {
            val msg = result.exceptionOrNull()?.message ?: "Invalid settings"
            when {
                msg.contains("Wake") -> _uiState.update { it.copy(wakeTimeError = msg) }
                msg.contains("Delay") || msg.contains("delay") -> _uiState.update { it.copy(delayError = msg) }
                else -> _uiState.update { it.copy(wakeTimeError = msg) }
            }
            return
        }

        val wasOnboarding = !state.onboardingComplete

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                repository.save(result.getOrThrow())
                if (wasOnboarding) {
                    repository.markOnboardingComplete()
                    _navEvent.emit(SettingsNavEvent.ToHome)
                }
                Log.d(TAG, "Settings saved: wake=${settings.wakeHour}:${settings.wakeMinute} " +
                        "bed=${settings.bedHour}:${settings.bedMinute} delay=${settings.firstMealDelayMinutes}")
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save settings", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = AppError.DatabaseError("Failed to save settings: ${e.message}", e),
                    )
                }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun parseTime(text: String): Pair<Int, Int>? {
        val parts = text.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h to m
    }
}
