package com.rehman.docscan.ui.mainScreen

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rehman.docscan.R
import com.rehman.docscan.core.AdsManager
import com.rehman.docscan.core.BillingManager
import com.rehman.docscan.core.InAppUpdateUtils
import com.rehman.docscan.core.PermissionUtils.arePermissionGranted
import com.rehman.docscan.core.Prefs
import com.rehman.docscan.core.Toasty
import com.rehman.docscan.core.Utils.enableTransition
import com.rehman.docscan.core.Utils.openAppSettings
import com.rehman.docscan.core.Utils.showCustomSnackBar
import com.rehman.docscan.core.Utils.showPermissionDialog
import com.rehman.docscan.databinding.ActivityMainBinding
import com.rehman.docscan.interfaces.SnackBarListener

class MainActivity : AppCompatActivity(), SnackBarListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var doubleBackToExitPressedOnce = false
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var updateUtils: InAppUpdateUtils
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        updateUtils.onActivityResult(result.resultCode)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // ✅ User allowed
                Log.d("PermissionResult", "POST_NOTIFICATIONS granted")
                // You can now show a welcome notification or badge
            } else {
                // ❌ User denied
                Log.d("PermissionResult", "POST_NOTIFICATIONS denied")
                // Optionally check if it's "Don't ask again"
                val shouldShowRationale =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        false
                    }
                if (!shouldShowRationale) {
                    Log.d("PermissionResult", "Don't ask again selected")
                    // Optionally guide user to app settings

                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.enableTransition()

        BillingManager.initialize(applicationContext) { refreshPremiumUi(it) }
        refreshPremiumUi(Prefs.isPremiumUnlocked())

        // Initialize the In-App Update utils
        initializeIAUpdate()

        // Initialize the Bottom Navigation
        initBottomNav()

        // Handle back press
        handleBackPress()


    }

    private fun initializeIAUpdate() {
        updateUtils = InAppUpdateUtils(
            context = this,
            resultLauncher = updateLauncher,
            flexibleThresholdDays = 0
        )

        updateUtils.checkUpdateFlow {
            updateSettingsBadge()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!arePermissionGranted()) {

                val isFirstTimeAsking = !Prefs.wasNotificationPermissionRequested
                if (isFirstTimeAsking) {
                    showPermissionDialog(
                        title = "Enable Notifications",
                        description = "We use notifications to alert you about updates and important events.",
                        positiveButton = "Allow",
                        positiveButtonClickListener = {
                            Prefs.wasNotificationPermissionRequested = true
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    ) {
                        // Denied once (no "Don't ask again")
                        showPermissionDialog(
                            title = "Enable Notifications",
                            description = "We use notifications to alert you about updates and important events.",
                            positiveButton = "Allow",
                            positiveButtonClickListener = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        )
                    } else {
                        // "Don't ask again"
                        showPermissionDialog(
                            title = "Enable Notifications from Settings",
                            description = "Notification permission is permanently denied. Open settings to allow it manually.",
                            positiveButton = "Open Settings",
                            positiveButtonClickListener = {
                                openAppSettings()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun initBottomNav() {

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> {
                    binding.titleAppBar.text = getString(R.string.app_name)
                    updateSettingsBadge()
                }

                R.id.exploreFragment -> {
                    binding.titleAppBar.text = getString(R.string.explore)
                    updateSettingsBadge()
                }

                R.id.settingFragment -> {
                    binding.titleAppBar.text = getString(R.string.setting)
                    binding.bottomNavigationView.removeBadge(R.id.settingFragment)
                }
            }
        }


    }

    private fun updateSettingsBadge() {
        val settingsBadge = binding.bottomNavigationView.getOrCreateBadge(R.id.settingFragment)
        if (InAppUpdateUtils.UPDATE_AVAILABLE) {
            settingsBadge.isVisible = true
            settingsBadge.backgroundColor = getColor(R.color.color_secondary)
            settingsBadge.badgeTextColor = getColor(R.color.color_secondary)
            settingsBadge.number = 0
        } else {
            binding.bottomNavigationView.removeBadge(R.id.settingFragment)
        }
    }


    private fun handleBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id == R.id.homeFragment) {
                    if (doubleBackToExitPressedOnce) {
                        finish() // or you can call super.onBackPressed() if you want to use the default back behavior
                    } else {
                        doubleBackToExitPressedOnce = true

//                        showSnackBar("Press BACK again to exit")
                        Toasty.show(
                            this@MainActivity,
                            "Press BACK again to exit",
                            backgroundColor = R.color.color_secondary,
                            textColor = R.color.color_on_secondary
                        )

                        handler.postDelayed({
                            doubleBackToExitPressedOnce = false
                        }, 2000) // 2 seconds delay
                    }
                } else {
                    navController.navigateUp() // navigate to the previous fragment in the stack
                }
            }
        })
    }

    override fun onPause() {
        binding.adView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        refreshPremiumUi(Prefs.isPremiumUnlocked())
        binding.adView.resume()
    }

    override fun onStop() {
        super.onStop()
        // Unregister the listener when the fragment is paused
        updateUtils.unregister()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        binding.adView.destroy()
        super.onDestroy()
    }

    fun refreshPremiumUi(isPremium: Boolean = Prefs.isPremiumUnlocked()) {
        if (isPremium) {
            binding.adView.visibility = android.view.View.GONE
            return
        }

        AdsManager.loadBannerAd(binding.adView)
    }

    override fun showSnackBar(
        message: String,
        backgroundColor: Int,
        textColor: Int,
        duration: Long
    ) {
        this.showCustomSnackBar(
            message,
            binding.bottomLayout,
            backgroundColor,
            textColor,
            duration
        )
    }
}
