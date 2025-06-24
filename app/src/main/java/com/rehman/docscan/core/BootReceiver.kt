package com.rehman.docscan.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in listOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED
            )
        ) {
            AlarmScheduler.scheduleUpdateAlarm(context)
        }
    }
}

