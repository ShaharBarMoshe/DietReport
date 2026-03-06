package com.diet.dietreport.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diet.dietreport.AppError
import com.diet.dietreport.data.db.ReminderSlot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

private const val TAG = "DR/Report"

data class ReportUiState(
    val period: ReportPeriod = ReportPeriod.WEEKLY,
    val data: ReportData? = null,
    val error: AppError? = null,
)

fun interface ReportRepository {
    suspend fun slotsForRange(startMs: Long, endMs: Long): List<ReminderSlot>
}

class ReportViewModel(private val repo: ReportRepository) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.WEEKLY)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReportUiState> = _period
        .flatMapLatest { period ->
            flow {
                try {
                    val (startMs, endMs) = rangeFor(period)
                    val slots = repo.slotsForRange(startMs, endMs)
                    val data = computeMetrics(slots)
                    Log.d(TAG, "Report generated: ${data.overallPercent}% overall for $period (${slots.size} slots)")
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

    fun setPeriod(period: ReportPeriod) { _period.value = period }

    fun clearError() { _period.value = _period.value } // re-trigger refresh
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
