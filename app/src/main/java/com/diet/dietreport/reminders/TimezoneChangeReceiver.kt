package com.diet.dietreport.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.settings.data.SettingsRepository
import com.diet.dietreport.settings.data.settingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class TimezoneChangeReceiver : BroadcastReceiver() {

    private companion object {
        private const val TAG = "DR/Reminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIMEZONE_CHANGED) return

        Log.d(TAG, "Timezone changed — rescheduling reminders")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val settings = SettingsRepository(context.settingsDataStore).settings.first()
                val slotDao = AppDatabase.getInstance(context).reminderSlotDao()
                val scheduler = ReminderScheduler(context, slotDao)

                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                val wakeMs = todayTimeMs(settings.wakeHour, settings.wakeMinute, cal)
                val bedtimeMs = todayTimeMs(settings.bedHour, settings.bedMinute, cal)
                val delayMs = settings.firstMealDelayMinutes * 60_000L

                scheduler.scheduleForDay(wakeMs, delayMs, bedtimeMs, fromMs = now)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule after timezone change", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun todayTimeMs(hour: Int, minute: Int, cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
