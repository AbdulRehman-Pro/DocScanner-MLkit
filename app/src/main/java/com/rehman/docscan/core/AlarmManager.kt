package com.rehman.docscan.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UpdateCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(InAppUpdateUtils.TAG, "Alarm triggered")

        val updateUtils = InAppUpdateUtils(
            context = context
        )

        updateUtils.checkUpdateFlow {
            if (InAppUpdateUtils.UPDATE_AVAILABLE) {
                NotificationUtils.showPlayStoreNotification(context = context, openApp = true)
            }
        }

        AlarmScheduler.scheduleUpdateAlarm(context)
    }
}
