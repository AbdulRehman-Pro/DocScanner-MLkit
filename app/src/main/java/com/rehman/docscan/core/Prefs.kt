package com.rehman.docscan.core

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

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
    fun setScanMode(scanModeId: Int) {
        sharedPrefs.edit() { putInt("scanModeId", scanModeId) }
    }

    fun getScanMode(): Int = sharedPrefs.getInt("scanModeId", -1)

    fun removeScanMode() {
        sharedPrefs.edit() { remove("scanModeId") }
    }


    // Image Limit Preferences
    fun setImageLimit(scanModeId: Int) {
       sharedPrefs.edit() { putInt("imageLimitId", scanModeId) }
    }

    fun getImageLimit(): Int = sharedPrefs.getInt("imageLimitId", -1)


    fun removeImageLimit(context: Context) {
        sharedPrefs.edit() { remove("imageLimitId") }
    }


    // Import From Gallery Preferences
    fun setImportFromGallery(importFromGallery: Boolean) {
        sharedPrefs.edit() { putBoolean("importFromGallery", importFromGallery) }
    }

    fun getImportFromGallery(): Boolean = sharedPrefs.getBoolean("importFromGallery", false)


    fun removeImportFromGallery(context: Context) {
        sharedPrefs.edit() { remove("importFromGallery") }
    }

}