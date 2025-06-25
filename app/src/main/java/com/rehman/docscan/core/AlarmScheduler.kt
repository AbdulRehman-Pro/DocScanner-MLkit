package com.rehman.docscan.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri

object AlarmScheduler {

    private const val ALARM_INTERVAL_MS = 15 * 60 * 1000L // 15 min
//    private const val ALARM_INTERVAL_MS = 20 * 1000L // 20 sec for testing

    fun scheduleUpdateAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, UpdateCheckReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + ALARM_INTERVAL_MS
        // Check permission on Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(InAppUpdateUtils.TAG, "Exact alarm not permitted. Prompting user...")

                // Optional: direct user to enable it in settings
                val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${context.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)

                return // Don't schedule until permission is granted
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d(InAppUpdateUtils.TAG, "Alarm scheduled in 15 minutes")
        } catch (e: SecurityException) {
            Log.e(InAppUpdateUtils.TAG, "Failed to schedule exact alarm: ${e.localizedMessage}")
        }
    }
}
