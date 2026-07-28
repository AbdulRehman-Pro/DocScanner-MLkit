package com.rehman.docscan.core

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.rehman.docscan.R

object Prefs {

    private var _sharedPrefs: SharedPreferences? = null


    private val sharedPrefs: SharedPreferences
        get() = _sharedPrefs ?: throw IllegalStateException(
            "Prefs is not initialized. Call Prefs.init(context) in Application class."
        )

    fun init(context: Context) {
        if (_sharedPrefs == null) {
            _sharedPrefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        }
    }

    // Scan Mode Preferences
    fun setScanMode(scanMode: String) {
        sharedPrefs.edit() { putString("scanMode", scanMode) }
    }

    fun getScanMode(context: Context): String = sharedPrefs.getString("scanMode", context.getString(R.string.basic_mode)).toString()

    fun removeScanMode() {
        sharedPrefs.edit() { remove("scanMode") }
    }


    // Image Limit Preferences
    fun setImageLimit(imageLimit: String) {
       sharedPrefs.edit() { putString("imageLimit", imageLimit) }
    }

    fun getImageLimit(context: Context): String = sharedPrefs.getString("imageLimit", context.getString(R.string.single_mode)).toString()


    fun removeImageLimit() {
        sharedPrefs.edit() { remove("imageLimit") }
    }


    // Import From Gallery Preferences
    fun setImportFromGallery(importFromGallery: Boolean) {
        sharedPrefs.edit() { putBoolean("importFromGallery", importFromGallery) }
    }

    fun getImportFromGallery(): Boolean = sharedPrefs.getBoolean("importFromGallery", false)


    fun removeImportFromGallery(context: Context) {
        sharedPrefs.edit() { remove("importFromGallery") }
    }

   // App update notification

    var wasNotificationPermissionRequested: Boolean
        get() = sharedPrefs.getBoolean("notify_permission_asked", false)
        set(value) {
            sharedPrefs.edit() { putBoolean("notify_permission_asked", value) }
        }


    fun isPremiumUnlocked() = sharedPrefs.getBoolean("is_premium_unlocked", false)

    fun setPremiumUnlocked(isUnlocked: Boolean) {
        sharedPrefs.edit() { putBoolean("is_premium_unlocked", isUnlocked) }
    }

    fun getCoinBalance() = sharedPrefs.getInt("coin_balance", 0)

    fun addCoins(amount: Int) {
        val newBalance = getCoinBalance() + amount
        sharedPrefs.edit() { putInt("coin_balance", newBalance) }
    }

    fun useCoins(cost: Int): Boolean {
        val current = getCoinBalance()
        return if (current >= cost) {
            sharedPrefs.edit() { putInt("coin_balance", current - cost) }
            true
        } else false
    }




}
