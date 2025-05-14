package com.rehman.docscan.ui.mainScreen

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rehman.docscan.R
import com.rehman.docscan.core.Utils.showCustomSnackBar
import com.rehman.docscan.core.Utils.showSnackBar
import com.rehman.docscan.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var doubleBackToExitPressedOnce = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(getColor(R.color.color_primary_variant)),
            navigationBarStyle = SystemBarStyle.dark(getColor(R.color.color_primary_variant))
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBottomNav()
        handleBackPress()


    }

    private fun initBottomNav() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)


        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> binding.titleAppBar.text = getString(R.string.app_name)
                R.id.exploreFragment -> binding.titleAppBar.text = getString(R.string.explore)
                R.id.settingFragment -> binding.titleAppBar.text = getString(R.string.setting)
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

//                        binding.root.showSnackBar("Press BACK again to exit", binding.bottomNavigationView)
                        showCustomSnackBar("Press BACK again to exit", binding.bottomNavigationView)

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

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}