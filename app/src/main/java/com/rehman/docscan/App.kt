package com.rehman.docscan

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rehman.docscan.core.Prefs
import com.rehman.docscan.core.UpdateCheckWorker
import java.util.concurrent.TimeUnit

class App: Application() {


    override fun onCreate() {
        super.onCreate()

        // Initialize Prefs
        Prefs.init(this)

        // Schedule the update check worker
        scheduleUpdateWorker(this)

    }

    fun scheduleUpdateWorker(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "UpdateChecker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }




}