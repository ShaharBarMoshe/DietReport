package com.diet.dietreport.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SnoozeReceiver : BroadcastReceiver() {

    private companion object {
        private const val SNOOZE_MS = 10L * 60 * 1000  // 10 minutes
        private const val TAG = "DR/Reminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val slotId = intent.getLongExtra(ReminderReceiver.EXTRA_SLOT_ID, -1L)
        val scheduledAt = intent.getLongExtra(ReminderReceiver.EXTRA_SCHEDULED_AT, 0L)
        if (slotId == -1L) return

        val newTime = System.currentTimeMillis() + SNOOZE_MS
        Log.d(TAG, "Snoozing slot $slotId to $newTime")

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val newIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_SLOT_ID, slotId)
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            slotId.toInt(),
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, newTime, pi)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, newTime, pi)
        }
    }
}
