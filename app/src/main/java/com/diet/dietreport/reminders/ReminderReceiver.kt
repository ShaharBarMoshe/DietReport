package com.diet.dietreport.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_SLOT_ID = "slot_id"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
        private const val TAG = "DR/Reminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val slotId = intent.getLongExtra(EXTRA_SLOT_ID, -1L)
        val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, System.currentTimeMillis())

        if (slotId == -1L) {
            Log.w(TAG, "ReminderReceiver fired with no slot_id")
            return
        }

        Log.d(TAG, "Alarm fired for slot $slotId at $scheduledAt")
        NotificationHelper.showReminderNotification(context, slotId, scheduledAt)
    }
}
