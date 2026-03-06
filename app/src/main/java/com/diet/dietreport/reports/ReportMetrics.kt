package com.diet.dietreport.reports

import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.SlotStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayStat(val dateLabel: String, val success: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else success * 100 / total
}

data class HourStat(val hour: Int, val success: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else success * 100 / total
}

data class BucketStat(val label: String, val success: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else success * 100 / total
}

data class ReportData(
    val overallSuccess: Int,
    val overallTotal: Int,
    val byDay: List<DayStat>,
    val byHour: List<HourStat>,
    val buckets: List<BucketStat>,  // Morning, Afternoon, Evening
) {
    val overallPercent: Int get() = if (overallTotal == 0) 0 else overallSuccess * 100 / overallTotal
}

enum class ReportPeriod { WEEKLY, MONTHLY }

private val dayFormat = SimpleDateFormat("MMM d", Locale.getDefault())

fun computeMetrics(slots: List<ReminderSlot>): ReportData {
    if (slots.isEmpty()) return ReportData(
        0, 0, emptyList(), emptyList(),
        listOf(BucketStat("Morning", 0, 0), BucketStat("Afternoon", 0, 0), BucketStat("Evening", 0, 0)),
    )

    val cal = Calendar.getInstance()

    val byDay = slots
        .groupBy { slot ->
            cal.timeInMillis = slot.scheduledAt
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }
        .entries.sortedBy { it.key }
        .map { (_, daySlots) ->
            DayStat(
                dateLabel = dayFormat.format(Date(daySlots.first().scheduledAt)),
                success = daySlots.count { it.status == SlotStatus.SUCCESS },
                total = daySlots.size,
            )
        }

    val byHour = slots
        .groupBy { slot ->
            cal.timeInMillis = slot.scheduledAt
            cal.get(Calendar.HOUR_OF_DAY)
        }
        .entries.sortedBy { it.key }
        .map { (hour, hourSlots) ->
            HourStat(
                hour = hour,
                success = hourSlots.count { it.status == SlotStatus.SUCCESS },
                total = hourSlots.size,
            )
        }

    fun bucketSlots(startH: Int, endH: Int) = slots.filter { slot ->
        cal.timeInMillis = slot.scheduledAt
        cal.get(Calendar.HOUR_OF_DAY) in startH until endH
    }

    val morning = bucketSlots(6, 12)
    val afternoon = bucketSlots(12, 18)
    val evening = bucketSlots(18, 24)

    return ReportData(
        overallSuccess = slots.count { it.status == SlotStatus.SUCCESS },
        overallTotal = slots.size,
        byDay = byDay,
        byHour = byHour,
        buckets = listOf(
            BucketStat("Morning", morning.count { it.status == SlotStatus.SUCCESS }, morning.size),
            BucketStat("Afternoon", afternoon.count { it.status == SlotStatus.SUCCESS }, afternoon.size),
            BucketStat("Evening", evening.count { it.status == SlotStatus.SUCCESS }, evening.size),
        ),
    )
}

fun buildShareText(period: ReportPeriod, data: ReportData): String = buildString {
    appendLine("DietReport – ${if (period == ReportPeriod.WEEKLY) "Weekly" else "Monthly"} Report")
    appendLine("Overall: ${data.overallPercent}% (${data.overallSuccess}/${data.overallTotal})")
    if (data.byDay.isNotEmpty()) {
        appendLine()
        appendLine("By Day:")
        data.byDay.forEach { appendLine("  ${it.dateLabel}: ${it.percent}%") }
    }
    appendLine()
    val (m, a, e) = data.buckets
    append("Morning: ${m.percent}% | Afternoon: ${a.percent}% | Evening: ${e.percent}%")
}
