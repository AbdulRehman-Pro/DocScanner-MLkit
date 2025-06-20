package com.rehman.docscan.core

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.rehman.docscan.R
import com.rehman.docscan.core.PermissionUtils.arePermissionGranted
import com.rehman.docscan.core.PermissionUtils.requestPermission

object NotificationUtils {

    fun showPlayStoreNotification(activity: Activity) {
        val channelId = "playstore_notify_channel"
        val notificationId = 101

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://play.google.com/store/apps/details?id=${activity.packageName}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            activity,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create Notification Channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Update Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification to redirect user to Play Store"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }
            val manager = activity.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(activity, channelId)
            .setSmallIcon(R.drawable.ic_update) // Make sure this icon exists in drawable
            .setContentTitle("Update Available")
            .setContentText("Tap to update the app from Play Store")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(activity).notify(notificationId, notification)
            }
        } else {
            NotificationManagerCompat.from(activity).notify(notificationId, notification)
        }

    }

}
