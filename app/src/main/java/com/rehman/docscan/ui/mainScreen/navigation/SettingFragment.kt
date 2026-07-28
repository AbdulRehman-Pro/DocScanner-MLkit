package com.rehman.docscan.ui.mainScreen.navigation

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.rehman.docscan.R
import com.rehman.docscan.core.AdsManager
import com.rehman.docscan.core.BillingManager
import com.rehman.docscan.core.InAppUpdateUtils
import com.rehman.docscan.core.Prefs
import com.rehman.docscan.core.Toasty
import com.rehman.docscan.core.Utils
import com.rehman.docscan.core.Utils.applyCustomColor
import com.rehman.docscan.core.Utils.enableTransition
import com.rehman.docscan.core.Utils.getAppVersion
import com.rehman.docscan.core.Utils.openPlayStoreAppPage
import com.rehman.docscan.core.Utils.openPlayStoreDevPage
import com.rehman.docscan.core.Utils.toggleCardDetails
import com.rehman.docscan.databinding.DialogModeSelectionBinding
import com.rehman.docscan.databinding.DialogPremiumBinding
import com.rehman.docscan.databinding.FragmentSettingBinding
import com.rehman.docscan.ui.mainScreen.MainActivity

class SettingFragment : Fragment() {

    private lateinit var binding: FragmentSettingBinding

    override fun onResume() {
        super.onResume()
        getPrefDetails()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.containerLayout.enableTransition()

        binding.scanModeCard.setOnClickListener {
            getPrefDetails()
            toggleCardDetails(binding.scanModeCardDetail, binding.scanModeArrow)

        }

        binding.scanLimitCard.setOnClickListener {
            getPrefDetails()
            toggleCardDetails(binding.scanLimitDetails, binding.scanLimitArrow)
        }

        binding.limitMode.setOnClickListener {
            openDialog("limit")
        }

        binding.scanMode.setOnClickListener {
            openDialog("mode")
        }

        binding.importSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setImportFromGallery(isChecked)
        }

        binding.premiumCard.setOnClickListener {
            showPremiumDialog()
        }


        binding.versionName.text = getString(R.string.version, getAppVersion(requireContext()))

        if (InAppUpdateUtils.UPDATE_AVAILABLE) {
            val boldStart =
                requireContext().getString(R.string.play_store_update_desc).indexOf("Tap here")

            binding.playStoreDesc.applyCustomColor(
                requireContext().getString(R.string.play_store_update_desc),
                boldTextLengthStart = boldStart,
                boldTextLengthEnd = boldStart + 8
            )
        } else {
            binding.playStoreDesc.applyCustomColor(
                requireContext().getString(R.string.play_store_desc),
                boldTextLengthEnd = 14
            )
        }


        val startColor = ContextCompat.getColor(requireContext(), R.color.color_secondary)
        val endColor = ContextCompat.getColor(requireContext(), R.color.color_primary)
        binding.versionCard.animateStrokeColorLoop(startColor, endColor)

        binding.versionCard.setOnClickListener {
            if (InAppUpdateUtils.UPDATE_AVAILABLE) {
                requireContext().openPlayStoreAppPage()
            } else {
                requireContext().openPlayStoreDevPage()
            }
        }


    }

    private fun getPrefDetails() {
        when (Prefs.getScanMode(requireContext())) {
            getString(R.string.basic_mode) -> binding.scanModeText.text =
                getString(R.string.basic_mode)

            getString(R.string.basic_mode_with_filters) -> binding.scanModeText.text =
                getString(R.string.basic_mode_with_filters)

            getString(R.string.advance_mode) -> binding.scanModeText.text =
                getString(R.string.advance_mode)
        }

        when (Prefs.getImageLimit(requireContext())) {
            getString(R.string.single_mode) -> binding.limitModeText.text =
                getString(R.string.single_mode)

            getString(R.string.burst_mode) -> binding.limitModeText.text =
                getString(R.string.burst_mode)
        }

        binding.importSwitch.isChecked = Prefs.getImportFromGallery()
        renderPremiumState()

        binding.updateTag.visibility =
            if (InAppUpdateUtils.UPDATE_AVAILABLE) View.VISIBLE else View.GONE
    }

    private fun renderPremiumState() {
        if (Prefs.isPremiumUnlocked()) {
            binding.premiumTitle.text = getString(R.string.premium_active_title)
            binding.premiumDescription.text = getString(R.string.premium_active_description)
        } else {
            binding.premiumTitle.text = getString(R.string.premium_card_title)
            binding.premiumDescription.text = getString(R.string.premium_card_description)
        }
    }

    private fun MaterialCardView.animateStrokeColorLoop(
        @ColorInt colorStart: Int,
        @ColorInt colorEnd: Int,
        duration: Long = 1000L
    ): ValueAnimator {
        val animator = ValueAnimator.ofObject(ArgbEvaluator(), colorStart, colorEnd).apply {
            this.duration = duration
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE

            addUpdateListener { animation ->
                val color = animation.animatedValue as Int
                this@animateStrokeColorLoop.strokeColor = color
            }

            start()
        }
        return animator
    }


    private fun openDialog(type: String) {
        Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            val dialogBinding = DialogModeSelectionBinding.inflate(layoutInflater)
            setContentView(dialogBinding.root)

            dialogBinding.root.post {
                if (Prefs.isPremiumUnlocked() || Prefs.getCoinBalance() >= 5) {
                    dialogBinding.advanceModeRadio.alpha = 1f
                    dialogBinding.burstModeRadio.alpha = 1f
                } else {
                    val isAdvanceChecked = dialogBinding.advanceModeRadio.isChecked
                    val isBurstChecked = dialogBinding.burstModeRadio.isChecked

                    Log.d("ModeAlphaDebug", "Coin balance < 5")
                    Log.d("ModeAlphaDebug", "AdvanceMode isChecked: $isAdvanceChecked")
                    Log.d("ModeAlphaDebug", "BurstMode isChecked: $isBurstChecked")

                    dialogBinding.advanceModeRadio.alpha = if (isAdvanceChecked) 1f else 0.5f
                    dialogBinding.burstModeRadio.alpha = if (isBurstChecked) 1f else 0.5f
                }
            }




            if (type == "limit") {
                dialogBinding.limitRadioGroup.visibility = View.VISIBLE
                dialogBinding.modeRadioGroup.visibility = View.GONE
                Utils.applyCustomFontAndColor(
                    requireContext(),
                    dialogBinding.singleModeRadio,
                    "${getString(R.string.single_mode)}\nTake one focused picture at a time.",
                    11
                )
                Utils.applyCustomFontAndColor(
                    requireContext(),
                    dialogBinding.burstModeRadio,
                    "${getString(R.string.burst_mode)}\nMultiple pictures seamlessly for rapid scanning.",
                    10
                )


                when (Prefs.getImageLimit(requireContext())) {
                    getString(R.string.single_mode) -> dialogBinding.singleModeRadio.isChecked =
                        true

                    getString(R.string.burst_mode) -> dialogBinding.burstModeRadio.isChecked = true
                }

                dialogBinding.limitRadioGroup.setOnCheckedChangeListener { radioGroup, checkedId ->

                    val selectedRadioButton = radioGroup.findViewById<RadioButton>(checkedId)

                    if (checkedId == R.id.burstModeRadio) {
                        if (!Prefs.isPremiumUnlocked() && Prefs.getCoinBalance() < 5) {
                            showPremiumDialog(type)
                            dialogBinding.advanceModeRadio.isChecked = false
                            dismiss()
                            return@setOnCheckedChangeListener // Exit early, don't dismiss
                        } else {
                            val selectedText = selectedRadioButton?.text?.toString() ?: "Unknown"
                            val title = selectedText.substringBefore("\n")
                            Prefs.setImageLimit(title)
                            if (!Prefs.isPremiumUnlocked()) {
                                Prefs.useCoins(5)
                            }
                            binding.limitModeText.text = title
                            dismiss()
                        }
                    } else {
                        val selectedText = selectedRadioButton?.text?.toString() ?: "Unknown"
                        val title = selectedText.substringBefore("\n")
                        Prefs.setImageLimit(title)
                        binding.limitModeText.text = title
                        dismiss()
                    }
                }


            } else {
                dialogBinding.limitRadioGroup.visibility = View.GONE
                dialogBinding.modeRadioGroup.visibility = View.VISIBLE

                Utils.applyCustomFontAndColor(
                    requireContext(),
                    dialogBinding.basicModeRadio,
                    "${getString(R.string.basic_mode)}\nBasic editing (crop, rotate, reorder pages).",
                    10
                )
                Utils.applyCustomFontAndColor(
                    requireContext(),
                    dialogBinding.basicModeFilterRadio,
                    "${getString(R.string.basic_mode_with_filters)}\nAdds image filters (grayscale, enhancement).",
                    23
                )
                Utils.applyCustomFontAndColor(
                    requireContext(),
                    dialogBinding.advanceModeRadio,
                    "${getString(R.string.advance_mode)}\nML-enabled cleaning (erase stains, fingers) and future major features.",
                    12
                )

                when (Prefs.getScanMode(requireContext())) {
                    getString(R.string.basic_mode) -> dialogBinding.basicModeRadio.isChecked = true
                    getString(R.string.basic_mode_with_filters) -> dialogBinding.basicModeFilterRadio.isChecked =
                        true

                    getString(R.string.advance_mode) -> dialogBinding.advanceModeRadio.isChecked =
                        true
                }

                dialogBinding.modeRadioGroup.setOnCheckedChangeListener { radioGroup, checkedId ->

                    val selectedRadioButton = radioGroup.findViewById<RadioButton>(checkedId)

                    if (checkedId == R.id.advanceModeRadio) {
                        if (!Prefs.isPremiumUnlocked() && Prefs.getCoinBalance() < 5) {
                            showPremiumDialog(type)
                            dialogBinding.advanceModeRadio.isChecked = false
                            dismiss()
                            return@setOnCheckedChangeListener // Exit early, don't dismiss
                        } else {
                            val selectedText = selectedRadioButton?.text?.toString() ?: "Unknown"
                            val title = selectedText.substringBefore("\n")
                            Prefs.setScanMode(title)
                            if (!Prefs.isPremiumUnlocked()) {
                                Prefs.useCoins(5)
                            }
                            binding.scanModeText.text = title
                            dismiss()
                        }
                    } else {
                        val selectedText = selectedRadioButton?.text?.toString() ?: "Unknown"
                        val title = selectedText.substringBefore("\n")
                        Prefs.setScanMode(title)
                        binding.scanModeText.text = title
                        dismiss()
                    }
                }
            }

            window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setDimAmount(0.6F)
                attributes.windowAnimations = R.style.DialogAnimation
                setGravity(Gravity.BOTTOM)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

                // Ensure visibility flags are set
                WindowInsetsControllerCompat(this, this.decorView).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }


            }
            show()
        }

    }

    private fun showPremiumDialog(type: String? = null) {
        Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            val binding = DialogPremiumBinding.inflate(layoutInflater)
            setContentView(binding.root)

            if (Prefs.isPremiumUnlocked()) {
                binding.coinsAvailable.text = getString(R.string.premium_active_description)
            } else if (Prefs.getCoinBalance() == 0) {
                binding.coinsAvailable.text = getString(R.string.coins_empty)
            } else if (Prefs.getCoinBalance() < 5) {
                binding.coinsAvailable.text =
                    getString(R.string.coins_not_available, Prefs.getCoinBalance())
            } else {
                binding.coinsAvailable.text =
                    getString(R.string.coins_available, Prefs.getCoinBalance())
            }

            binding.watchAdCard.visibility =
                if (Prefs.isPremiumUnlocked()) View.GONE else View.VISIBLE

            BillingManager.getPremiumPrice { price ->
                binding.buyPremiumPrice.text = price ?: getString(R.string.premium_price_fallback)
            }

            binding.buyPremiumCard.setOnClickListener {
//                if (Prefs.isPremiumUnlocked()) return@setOnClickListener
                Log.d("BillingManager","BillingManager cal")

                BillingManager.launchPremiumPurchase(requireActivity()) { result ->
                    Log.d("BillingManager","BillingManager: $result")
                    when (result) {
                        is BillingManager.PurchaseResult.Success -> {
                            Prefs.setPremiumUnlocked(true)
                            renderPremiumState()
                            (activity as? MainActivity)?.refreshPremiumUi(true)
                            Toast.makeText(requireContext(),getString(R.string.premium_purchase_success) , Toast.LENGTH_SHORT).show()
                            dismiss()
                            reopenLockedDialog(type)
                        }

                        is BillingManager.PurchaseResult.Cancelled -> {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.premium_purchase_cancelled),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is BillingManager.PurchaseResult.Error -> {
                            Toast.makeText(
                                requireContext(),
                                result.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            binding.restorePurchaseCard.setOnClickListener {
                BillingManager.restorePurchases { result ->
                    when (result) {
                        is BillingManager.PurchaseResult.Success -> {
                            Prefs.setPremiumUnlocked(true)
                            renderPremiumState()
                            (activity as? MainActivity)?.refreshPremiumUi(true)
                            Toasty.show(
                                requireContext(),
                                getString(R.string.premium_restore_success),
                                backgroundColor = R.color.color_secondary,
                                textColor = R.color.color_on_secondary
                            )
                            dismiss()
                            reopenLockedDialog(type)
                        }

                        BillingManager.PurchaseResult.Cancelled -> Unit

                        is BillingManager.PurchaseResult.Error -> {
                            Toasty.show(
                                requireContext(),
                                result.message,
                                iconRes = R.drawable.ic_cross,
                                backgroundColor = R.color.color_error
                            )
                        }
                    }
                }
            }

            binding.watchAdCard.setOnClickListener {

                AdsManager.showRewardedAd(requireActivity()) { coins ->
                    Prefs.addCoins(coins)

                    dismiss()
                    reopenLockedDialog(type)
                }
            }



            window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setDimAmount(0.6F)
                attributes.windowAnimations = R.style.DialogAnimation
                setGravity(Gravity.BOTTOM)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

                WindowInsetsControllerCompat(this, this.decorView).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }

            show()
        }
    }

    private fun reopenLockedDialog(type: String?) {
        if (type == "limit") {
            openDialog("limit")
        } else if (type == "mode") {
            openDialog("mode")
        }
    }


}
