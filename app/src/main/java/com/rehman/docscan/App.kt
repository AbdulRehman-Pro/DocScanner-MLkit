package com.rehman.docscan

import android.app.Application
import com.rehman.docscan.core.AlarmScheduler
import com.rehman.docscan.core.Prefs

class App : Application() {


    override fun onCreate() {
        super.onCreate()

        // Initialize Prefs
        Prefs.init(this)

        // Schedule the update alarm
        AlarmScheduler.scheduleUpdateAlarm(applicationContext)

    }


}