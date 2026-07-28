package com.rehman.docscan

import android.app.Application
import com.rehman.docscan.core.AdsManager
import com.rehman.docscan.core.AlarmScheduler
import com.rehman.docscan.core.BillingManager
import com.rehman.docscan.core.Prefs

class App : Application() {


    override fun onCreate() {
        super.onCreate()

        // Initialize Prefs
        Prefs.init(this)

        // Sync premium state first, then initialize ads only for free users.
        BillingManager.initialize(this) { isPremium ->
            if (!isPremium) {
                AdsManager.initializeAdMob(this)
            }
        }

        // Schedule the update alarm
        AlarmScheduler.scheduleUpdateAlarm(applicationContext)

    }


}
