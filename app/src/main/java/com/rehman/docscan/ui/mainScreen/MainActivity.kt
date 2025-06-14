package com.rehman.docscan.ui.mainScreen

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rehman.docscan.R
import com.rehman.docscan.core.InAppUpdateUtils
import com.rehman.docscan.core.Utils.showCustomSnackBar
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(getColor(R.color.color_primary_variant)),
            navigationBarStyle = SystemBarStyle.dark(getColor(R.color.color_primary_variant))
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the In-App Update utils
        updateUtils = InAppUpdateUtils(this, updateLauncher)

        initBottomNav()
        handleBackPress()


    }

    private fun initBottomNav() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)

        // Use the item ID you assigned in menu
        val settingsBadge = binding.bottomNavigationView.getOrCreateBadge(R.id.settingFragment)
        if (InAppUpdateUtils.UPDATE_AVAILABLE){
            settingsBadge.isVisible = InAppUpdateUtils.UPDATE_AVAILABLE
            settingsBadge.backgroundColor = getColor(R.color.color_secondary)
            settingsBadge.badgeTextColor = getColor(R.color.color_secondary)
            settingsBadge.number = 0
        }else{
            binding.bottomNavigationView.removeBadge(R.id.settingFragment)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> {
                    binding.titleAppBar.text = getString(R.string.app_name)
                    settingsBadge.isVisible = true
                }
                R.id.exploreFragment -> {
                    binding.titleAppBar.text = getString(R.string.explore)
                    settingsBadge.isVisible = true
                }
                R.id.settingFragment -> {
                    binding.titleAppBar.text = getString(R.string.setting)
                    settingsBadge.isVisible = false
                }
            }
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

                        showSnackBar("Press BACK again to exit")

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

    override fun onResume() {
        super.onResume()

        // Check for updates when the fragment resumed
        updateUtils.checkUpdateFlow()
    }

    override fun onStop() {
        super.onStop()

        // Unregister the listener when the fragment is paused
        updateUtils.unregister()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun showSnackBar(
        message: String,
        backgroundColor: Int,
        textColor: Int,
        duration: Long
    ) {
        this.showCustomSnackBar(message, binding.bottomNavigationView, backgroundColor, textColor,  duration)
    }
}