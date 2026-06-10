package com.diet.dietreport.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.diet.dietreport.AppError
import com.diet.dietreport.SchedulerErrorBus
import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.ReminderSlotDao

class ReminderScheduler(
    private val context: Context,
    private val slotDao: ReminderSlotDao,
) {
    private companion object {
        private const val TAG = "DR/Reminder"
        /** Fixed request code for the daily-reschedule alarm (avoids collision with slot IDs). */
        private const val NEXT_DAY_REQUEST_CODE = Int.MAX_VALUE - 1
    }

    /**
     * Computes slots for a day, clears any future slots from [fromMs] onwards, inserts new
     * slots into the database, schedules an AlarmManager alarm for each, and sets a
     * next-day alarm so reminders automatically repeat every day.
     */
    suspend fun scheduleForDay(
        wakeMs: Long,
        delayMs: Long,
        bedtimeMs: Long,
        fromMs: Long = System.currentTimeMillis(),
    ) {
        try {
            slotDao.deleteFrom(fromMs)

            val slots = SlotComputer.computeSlots(wakeMs, delayMs, bedtimeMs)
                .filter { it.scheduledAt >= fromMs }

            Log.d(TAG, "Scheduling ${slots.size} slot(s) from $fromMs")

            for (slotTime in slots) {
                val slotId = slotDao.insert(
                    ReminderSlot(
                        scheduledAt = slotTime.scheduledAt,
                        wakeStart = slotTime.wakeStart,
                        wakeEnd = slotTime.wakeEnd,
                        bedtime = slotTime.bedtime,
                    )
                )
                scheduleAlarm(slotId, slotTime.scheduledAt)
                Log.d(TAG, "Slot $slotId scheduled at ${slotTime.scheduledAt}")
            }

            scheduleNextDayAlarm(wakeMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminders", e)
            SchedulerErrorBus.post(
                AppError.SchedulerError("Failed to schedule reminders: ${e.message}", e)
            )
        }
    }

    /**
     * Schedules an alarm for tomorrow's wake time that fires [DailyRescheduleReceiver],
     * which in turn calls [scheduleForDay] for the new day, creating a self-perpetuating chain.
     */
    private fun scheduleNextDayAlarm(todayWakeMs: Long) {
        val tomorrowWakeMs = todayWakeMs + 24L * 60 * 60 * 1000
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, DailyRescheduleReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            NEXT_DAY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tomorrowWakeMs, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tomorrowWakeMs, pi)
        }
        Log.d(TAG, "Next-day reschedule alarm set for $tomorrowWakeMs")
    }

    private fun scheduleAlarm(slotId: Long, scheduledAt: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_SLOT_ID, slotId)
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            slotId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledAt, pi)
        } else {
            // setAndAllowWhileIdle bypasses Doze without requiring SCHEDULE_EXACT_ALARM
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledAt, pi)
        }
    }
}
