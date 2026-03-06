package com.diet.dietreport.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.diet.dietreport.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID = "diet_reminders"
    private const val NOTIF_ID_BASE = 2000

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Meal Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders to log your meals"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showReminderNotification(context: Context, slotId: Long, scheduledAt: Long) {
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(scheduledAt))

        // "Log meal" action → opens MainActivity (deep link wired in Phase 6)
        val logMealIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "log_meal")
            putExtra("slot_id", slotId)
        }
        val logMealPi = PendingIntent.getActivity(
            context,
            slotId.toInt(),
            logMealIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "Snooze" action → reschedules alarm +10 min
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_SLOT_ID, slotId)
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            (slotId + 10000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val logIcon = Icon.createWithResource(context, android.R.drawable.ic_menu_camera)
        val snoozeIcon = Icon.createWithResource(context, android.R.drawable.ic_popup_reminder)

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Time to log your meal")
            .setContentText("Reminder at $timeStr")
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentIntent(logMealPi)
            .addAction(
                Notification.Action.Builder(logIcon, "Log meal", logMealPi).build()
            )
            .addAction(
                Notification.Action.Builder(snoozeIcon, "Snooze", snoozePi).build()
            )
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID_BASE + slotId.toInt(), notification)
    }
}
