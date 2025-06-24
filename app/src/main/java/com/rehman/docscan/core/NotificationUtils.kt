package com.rehman.docscan.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.rehman.docscan.R
import com.rehman.docscan.ui.splashScreen.SplashActivity

object NotificationUtils {

    fun showPlayStoreNotification(context: Context, openApp: Boolean = false) {
        val channelId = "playstore_notify_channel"
        val notificationId = 101

        val intent = if (openApp){
            Intent(Intent.ACTION_VIEW, "docscan://setting".toUri()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }else{
            Intent(Intent.ACTION_VIEW).apply {
                data = "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationChannelDescription = if (openApp) {
            "Stay up to date! Tap to open the app and get the latest version directly."
        } else {
            "A new update is available! Tap to open the Play Store and install it."
        }

        // Create Notification Channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Update Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = notificationChannelDescription
                setShowBadge(true)
                enableLights(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notificationTitle = if (openApp) {
            "Update Available Inside the App"
        } else {
            "Update Available on Play Store"
        }

        val notificationContentText = if (openApp) {
            "Open the app to download the latest version seamlessly."
        } else {
            "Click to update from the Play Store now."
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Make sure this icon exists in drawable
            .setContentTitle(notificationTitle)
            .setContentText(notificationContentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContentText)) // expands content
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }

    }

}
