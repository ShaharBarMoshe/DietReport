package com.diet.dietreport.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diet.dietreport.AppError
import com.diet.dietreport.data.db.MealLog
import com.diet.dietreport.data.db.ReminderSlot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TAG = "DR/Report"

data class ReportUiState(
    val period: ReportPeriod = ReportPeriod.WEEKLY,
    val data: ReportData? = null,
    val error: AppError? = null,
    val showClearConfirmDialog: Boolean = false,
    val clearSuccess: Boolean = false,
)

interface ReportRepository {
    suspend fun slotsForRange(startMs: Long, endMs: Long): List<ReminderSlot>
    suspend fun offScheduleLogsForRange(startMs: Long, endMs: Long): List<MealLog>
    suspend fun clearLastWeek(startMs: Long, endMs: Long)
}

private data class ExtraState(
    val showClearConfirmDialog: Boolean = false,
    val clearSuccess: Boolean = false,
)

class ReportViewModel(private val repo: ReportRepository) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.WEEKLY)
    private val _extra = MutableStateFlow(ExtraState())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReportUiState> = _period
        .flatMapLatest { period ->
            flow {
                try {
                    val (startMs, endMs) = rangeFor(period)
                    val slots = repo.slotsForRange(startMs, endMs)
                    val offScheduleLogs = repo.offScheduleLogsForRange(startMs, endMs)
                    val data = computeMetrics(slots).copy(offScheduleCount = offScheduleLogs.size)
                    Log.d(TAG, "Report generated: ${data.overallPercent}% overall for $period " +
                        "(${slots.size} slots, ${offScheduleLogs.size} off-schedule)")
                    emit(ReportUiState(period = period, data = data))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load report for $period", e)
                    emit(
                        ReportUiState(
                            period = period,
                            error = AppError.DatabaseError("Failed to load report: ${e.message}", e),
                        )
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())
        .combine(_extra) { base, extra ->
            base.copy(
                showClearConfirmDialog = extra.showClearConfirmDialog,
                clearSuccess = extra.clearSuccess,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun setPeriod(period: ReportPeriod) { _period.value = period }

    fun clearError() { _period.value = _period.value } // re-trigger refresh

    fun onClearLastWeekClick() {
        _extra.update { it.copy(showClearConfirmDialog = true) }
    }

    fun onClearDismissed() {
        _extra.update { it.copy(showClearConfirmDialog = false) }
    }

    fun onClearConfirmed() {
        _extra.update { it.copy(showClearConfirmDialog = false) }
        viewModelScope.launch {
            try {
                val (startMs, endMs) = lastSaturdayRange()
                repo.clearLastWeek(startMs, endMs)
                Log.d(TAG, "Cleared last-week slots [$startMs, $endMs)")
                _extra.update { it.copy(clearSuccess = true) }
                _period.value = _period.value   // re-trigger report refresh
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear last week", e)
                _extra.update {
                    it.copy(
                        showClearConfirmDialog = false,
                        clearSuccess = false,
                    )
                }
                _period.value = _period.value  // re-trigger to show error
            }
        }
    }

    fun onClearSuccessDismissed() {
        _extra.update { it.copy(clearSuccess = false) }
    }
}

internal fun rangeFor(period: ReportPeriod): Pair<Long, Long> {
    val endMs = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis + 1L
    val startMs = when (period) {
        ReportPeriod.WEEKLY -> endMs - 7L * 24 * 60 * 60_000
        ReportPeriod.MONTHLY -> endMs - 30L * 24 * 60 * 60_000
    }
    return Pair(startMs, endMs)
}

internal fun lastSaturdayRange(): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    val daysBack = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY + 7) % 7
    cal.add(Calendar.DAY_OF_YEAR, -daysBack)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startMs = cal.timeInMillis
    val endMs = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis + 1L
    return Pair(startMs, endMs)
}
