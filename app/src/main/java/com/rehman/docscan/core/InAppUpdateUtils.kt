package com.rehman.docscan.core

import android.Manifest
import android.animation.LayoutTransition
import android.app.Activity
import android.app.Dialog
import android.content.IntentSender
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.LifecycleObserver
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.rehman.docscan.R
import com.rehman.docscan.core.PermissionUtils.arePermissionGranted
import com.rehman.docscan.core.PermissionUtils.requestPermission
import com.rehman.docscan.core.Utils.openAppSettings
import com.rehman.docscan.core.Utils.showPermissionDialog
import com.rehman.docscan.databinding.DialogProgressBinding

class InAppUpdateUtils(
    private val activity: Activity,
    private val resultLauncher: ActivityResultLauncher<IntentSenderRequest>, // Use registerForActivityResult
    private val flexibleThresholdDays: Int = 2,
    private val immediateThresholdDays: Int = 7,
    private val maxRetry: Int = 3
) : LifecycleObserver {

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
    private var listener: InstallStateUpdatedListener? = null
    private var retries = 0

    private var dialog: Dialog? = null

    fun checkUpdateFlow(onStatusChecked: (() -> Unit)? = null) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        decideUpdate(info)
                        UPDATE_AVAILABLE = true
                    }

                    info.installStatus() == InstallStatus.DOWNLOADED -> {
//                        showInstallReadyDialog()
                        appUpdateManager.completeUpdate()
                    }

                    info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        resumeImmediateUpdate(info)
                    }

                    else -> Log.i(
                        TAG,
                        "No update action needed. Status=${info.updateAvailability()}"
                    )
                }
                onStatusChecked?.invoke()
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed fetching update info: ${it.localizedMessage}")
                retryOrAbort()
                onStatusChecked?.invoke()
            }
    }

    private fun decideUpdate(info: AppUpdateInfo) {
        val days = info.clientVersionStalenessDays() ?: 0
        val priority = info.updatePriority()
        val allowImmediate = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        val allowFlexible = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

        when {
            priority >= HIGH_PRIORITY || (days >= immediateThresholdDays && allowImmediate) -> {
                startUpdate(info, AppUpdateType.IMMEDIATE)
            }

            days >= flexibleThresholdDays && allowFlexible -> {
                startUpdate(info, AppUpdateType.FLEXIBLE)
            }

            else -> {
                Log.i(
                    TAG,
                    "Update available but conditions not met (days=$days, priority=$priority)"
                )
            }
        }
    }

    private fun startUpdate(info: AppUpdateInfo, type: Int) {
        val options = AppUpdateOptions.newBuilder(type)
            .setAllowAssetPackDeletion(false)
            .build()

        if (type == AppUpdateType.FLEXIBLE) registerListener()

        try {
            // If an in-app update is already running, resume the update.
            appUpdateManager.startUpdateFlowForResult(
                info,
                resultLauncher,
                options
            )

        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Failed to start update flow: ${e.localizedMessage}")
        }
    }

    fun onActivityResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                retries = 0
                Prefs.setUpdateNotification(false)
                Log.i(TAG, "Update completed successfully")
            }

            Activity.RESULT_CANCELED -> {
                Log.i(TAG, "User canceled the update")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!activity.arePermissionGranted()) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        ) {
                            activity.showPermissionDialog(
                                title = "Enable Notifications",
                                description = "We use notifications to alert you about app updates. Please allow this permission.",
                                positiveButton = "Allow",
                                positiveButtonClickListener = {
                                    activity.requestPermission()
                                }
                            )

                        } else {
                            // Possibly "Don't ask again"
                            activity.showPermissionDialog(
                                title = "Enable Notifications from Settings",
                                description = "To get notified about app updates, enable notification permission from settings.",
                                positiveButton = "Open Settings",
                                positiveButtonClickListener = {
                                    activity.openAppSettings()
                                }
                            )
                        }

                    } else {
                        NotificationUtils.showPlayStoreNotification(activity)
                    }
                } else {
                    NotificationUtils.showPlayStoreNotification(activity)
                }
            }


            else -> {
                Log.w(TAG, "Unknown result code: $resultCode")
                retryOrAbort()
            }
        }
    }


    private fun resumeImmediateUpdate(info: AppUpdateInfo) {
        startUpdate(info, AppUpdateType.IMMEDIATE)
    }

    private fun registerListener() {
        if (listener != null) return

        listener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
//                    promptInfo(state)
                }
                InstallStatus.DOWNLOADED -> {
//                    showInstallReadyDialog()
                    appUpdateManager.completeUpdate()
                }
                InstallStatus.FAILED -> {
                    Log.e(TAG, "Update failed (code=${state.installErrorCode()})")
                    retryOrAbort()
                }

                InstallStatus.INSTALLED, InstallStatus.CANCELED -> {
                    unregister()
                    UPDATE_AVAILABLE = false
                }

                else -> {}
            }
        }

        appUpdateManager.registerListener(listener!!)
    }


    private fun promptInfo(state: InstallState) {
        val percent = if (state.totalBytesToDownload() > 0)
            (state.bytesDownloaded() * 100 / state.totalBytesToDownload()).toInt()
        else 0

        if (dialog == null) {
            dialog = Dialog(activity).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                val binding = DialogProgressBinding.inflate(activity.layoutInflater)
                setContentView(binding.root)
                setCancelable(false)

                val transition = LayoutTransition()
                transition.enableTransitionType(LayoutTransition.CHANGING)
                binding.root.layoutTransition = transition

                window?.apply {
                    setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                    setDimAmount(0.6F)
                    setGravity(Gravity.CENTER)
                }

                binding.apply {
                    dialogTitle.text = context.getString(R.string.downloading_title)
                    dialogDescription.text = context.getString(R.string.downloading_description)
                    dialogProgress.isIndeterminate = false
                    dialogProgress.post {
                        dialogProgress.setProgressCompat(percent, true)
                    }
                    dialogButton.text = context.getString(R.string.downloading_button)
                    dialogButton.setOnClickListener { dismiss() }
                }

                show()
            }
        } else {
            // Update existing dialog progress
            val binding = DialogProgressBinding.bind(dialog!!.findViewById(android.R.id.content))
            binding.dialogProgress.setProgressCompat(percent, true)
        }
    }


    private fun showInstallReadyDialog() {
        dialog?.dismiss()
        dialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            val binding = DialogProgressBinding.inflate(activity.layoutInflater)
            setContentView(binding.root)
            setCancelable(false)

            val transition = LayoutTransition()
            transition.enableTransitionType(LayoutTransition.CHANGING)
            binding.root.layoutTransition = transition

            binding.apply {
                dialogTitle.text = context.getString(R.string.downloaded_title)
                dialogDescription.text = context.getString(R.string.downloaded_description)
                dialogProgress.postDelayed({
                    dialogProgress.isIndeterminate = true
                }, 500)
                dialogButton.text = context.getString(R.string.downloaded_button)
                dialogButton.setOnClickListener {
                    appUpdateManager.completeUpdate()
                    dismiss()
                }
            }

            window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setDimAmount(0.6F)
                setGravity(Gravity.CENTER)
            }

            show()
        }
    }


    private fun retryOrAbort() {
        if (retries++ < maxRetry) {
            Log.w(TAG, "Retrying update check (${retries}/$maxRetry)")
            checkUpdateFlow()
        } else {
            Log.e(TAG, "Max retries reached. Giving up.")
        }
    }

    fun unregister() {
        listener?.let { appUpdateManager.unregisterListener(it) }
        listener = null
    }

    companion object {
        private const val TAG = "InAppUpdateUtils"
        private const val HIGH_PRIORITY = 4
        var UPDATE_AVAILABLE = false
    }
}

