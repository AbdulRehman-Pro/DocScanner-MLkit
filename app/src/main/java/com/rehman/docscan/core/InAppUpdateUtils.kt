package com.rehman.docscan.core

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.*
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.rehman.docscan.R
import com.rehman.docscan.databinding.DialogModeSelectionBinding

object InAppUpdateUtils {

    private const val UPDATE_REQUEST_CODE = 1001
    private var installStateUpdatedListener: InstallStateUpdatedListener? = null
    private var appUpdateManager: AppUpdateManager? = null

    fun checkForUpdate(
        activity: Activity,
        isFlexible: Boolean = true
    ) {
        appUpdateManager = AppUpdateManagerFactory.create(activity)
        val updateType = if (isFlexible) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE

        val appUpdateInfoTask = appUpdateManager!!.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(updateType)
            ) {
                try {
                    appUpdateManager!!.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateType,
                        activity,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("InAppUpdateUtils", "Update flow failed: ${e.localizedMessage}")
                }
            } else {
                Log.i("InAppUpdateUtils", "No update available or not allowed.")
            }
        }.addOnFailureListener {
            Log.e("InAppUpdateUtils", "Check for update failed: ${it.localizedMessage}")
        }

        if (isFlexible) {
            registerFlexibleUpdateListener(activity)
        }
    }

    private fun registerFlexibleUpdateListener(activity: Activity) {
        installStateUpdatedListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                showRestartSnackbar(activity)
            }

            progressDialog(activity)
        }

        appUpdateManager?.registerListener(installStateUpdatedListener!!)
    }

    private fun showRestartSnackbar(activity: Activity) {
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            "Update downloaded. Restart to apply.",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("RESTART") {
            appUpdateManager?.completeUpdate()
        }.show()
    }

    fun resumeUpdateIfNeeded(activity: Activity) {
        appUpdateManager = AppUpdateManagerFactory.create(activity)

        appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                appUpdateManager?.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    UPDATE_REQUEST_CODE
                )
            }
        }
    }

    fun unregisterListener() {
        installStateUpdatedListener?.let {
            appUpdateManager?.unregisterListener(it)
        }
        installStateUpdatedListener = null
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == UPDATE_REQUEST_CODE && resultCode != Activity.RESULT_OK) {
            Log.e("InAppUpdateUtils", "User canceled or update failed. Result code: $resultCode")
            // Optionally retry or show a message
        }
    }



    private fun progressDialog(activity: Activity) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogModeSelectionBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(false)
        //



        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setDimAmount(0.8F)
            setGravity(Gravity.CENTER)

        }
    }
}
