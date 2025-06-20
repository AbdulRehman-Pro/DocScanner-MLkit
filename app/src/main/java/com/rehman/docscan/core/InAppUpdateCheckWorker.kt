package com.rehman.docscan.core

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val appUpdateManager = AppUpdateManagerFactory.create(context)

    init {
        Prefs.init(context)
    }

    companion object {
        private const val TAG = "UpdateCheckWorker"
        private const val HIGH_PRIORITY = 4
        private const val FLEXIBLE_THRESHOLD_DAYS = 2
        private const val IMMEDIATE_THRESHOLD_DAYS = 7
    }

    override fun doWork(): Result {
        var latch = CountDownLatch(1)
        var result = Result.success()

        NotificationUtils.showPlayStoreNotification(applicationContext as Activity)

//        appUpdateManager.appUpdateInfo
//            .addOnSuccessListener { info ->
//                val updateAvailable =
//                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
//                val priority = info.updatePriority()
//                val days = info.clientVersionStalenessDays() ?: 0
//                val allowImmediate = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
//                val allowFlexible = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
//
//                val shouldNotify = when {
//                    priority >= HIGH_PRIORITY || (days >= IMMEDIATE_THRESHOLD_DAYS && allowImmediate) -> true
//                    days >= FLEXIBLE_THRESHOLD_DAYS && allowFlexible -> true
//                    else -> false
//                }
//
//                val alreadyNotified = Prefs.getUpdateNotification()
//
//                if (updateAvailable && shouldNotify && !alreadyNotified) {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
//                        ContextCompat.checkSelfPermission(
//                            applicationContext,
//                            Manifest.permission.POST_NOTIFICATIONS
//                        )
//                        != PackageManager.PERMISSION_GRANTED
//                    ) {
//                        Log.w(TAG, "Notification permission not granted.")
//                    } else {
//                        NotificationUtils.showPlayStoreNotification(applicationContext as Activity)
//                        Prefs.setUpdateNotification(true)
//                        Log.i(TAG, "Notification shown.")
//                    }
//                } else {
//                    Log.i(TAG, "No update needed or already notified.")
//                }
//
//                latch.countDown()
//            }
//            .addOnFailureListener {
//                Log.e(TAG, "Failed to fetch update info: ${it.localizedMessage}")
//                result = Result.retry()
//                latch.countDown()
//            }

        latch.await(10, TimeUnit.SECONDS)
        return result
    }
}
