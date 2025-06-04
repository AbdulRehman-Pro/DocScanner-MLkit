package com.rehman.docscan.ui.mainScreen.navigation

import android.animation.LayoutTransition
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.rehman.docscan.R
import com.rehman.docscan.core.Utils.getAppVersion
import com.rehman.docscan.databinding.FragmentSettingBinding
import androidx.core.view.isVisible
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.core.view.isGone
import com.rehman.docscan.core.Prefs
import com.rehman.docscan.core.Utils
import com.rehman.docscan.core.Utils.enableTransition
import com.rehman.docscan.core.Utils.toggleCardDetails
import com.rehman.docscan.databinding.DialogModeSelectionBinding
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

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


        binding.versionName.text = getString(R.string.version, getAppVersion(requireContext()))

        Utils.applyCustomColor(
            requireContext(),
            binding.playStoreDesc,
            requireContext().getString(R.string.play_store_desc),
            14
        )

        binding.versionCard.setOnClickListener {
            Toast.makeText(requireContext(), "v", Toast.LENGTH_SHORT).show()
        }

        binding.containerLayout.enableTransition()

        binding.scanModeCard.setOnClickListener {
            toggleCardDetails(binding.scanModeCardDetail, binding.scanModeArrow)

        }

        binding.scanLimitCard.setOnClickListener {
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





    }

    private fun getPrefDetails() {
        when (Prefs.getScanMode()) {
            R.id.basicModeRadio -> binding.scanModeText.text = "Basic Mode"
            R.id.basicModeFilterRadio -> binding.scanModeText.text = "Basic Mode with Filters"
            R.id.advanceModeRadio -> binding.scanModeText.text = "Advance Mode"
        }

        when (Prefs.getImageLimit()) {
            R.id.singleModeRadio -> binding.limitModeText.text = "Single Mode"
            R.id.burstModeRadio -> binding.limitModeText.text = "Burst Mode"
        }

        binding.importSwitch.isChecked = Prefs.getImportFromGallery()
    }


    private fun openDialog(type: String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogModeSelectionBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)


        if (type == "limit") {
            dialogBinding.limitRadioGroup.visibility = View.VISIBLE
            dialogBinding.modeRadioGroup.visibility = View.GONE
            Utils.applyCustomFontAndColor(
                requireContext(),
                dialogBinding.singleModeRadio,
                "Single mode\nTake one focused picture at a time.",
                11
            )
            Utils.applyCustomFontAndColor(
                requireContext(),
                dialogBinding.burstModeRadio,
                "Burst Mode\nMultiple pictures seamlessly for rapid scanning.",
                10
            )


            when (Prefs.getImageLimit()) {
                R.id.singleModeRadio -> dialogBinding.singleModeRadio.isChecked = true
                R.id.burstModeRadio -> dialogBinding.burstModeRadio.isChecked = true
            }

            dialogBinding.limitRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                Prefs.setImageLimit(checkedId)
                when (checkedId) {
                    R.id.singleModeRadio -> binding.limitModeText.text = "Single Mode"
                    R.id.burstModeRadio -> binding.limitModeText.text = "Burst Mode"
                }

                dialog.dismiss()
            }

        } else {
            dialogBinding.limitRadioGroup.visibility = View.GONE
            dialogBinding.modeRadioGroup.visibility = View.VISIBLE

            Utils.applyCustomFontAndColor(
                requireContext(),
                dialogBinding.basicModeRadio,
                "Basic Mode\nBasic editing (crop, rotate, reorder pages).",
                10
            )
            Utils.applyCustomFontAndColor(
                requireContext(),
                dialogBinding.basicModeFilterRadio,
                "Basic Mode with Filters\nAdds image filters (grayscale, enhancement).",
                23
            )
            Utils.applyCustomFontAndColor(
                requireContext(),
                dialogBinding.advanceModeRadio,
                "Advance Mode\nML-enabled cleaning (erase stains, fingers) and future major features.",
                12
            )

            when (Prefs.getScanMode()) {
                R.id.basicModeRadio -> dialogBinding.basicModeRadio.isChecked = true
                R.id.basicModeFilterRadio -> dialogBinding.basicModeFilterRadio.isChecked = true
                R.id.advanceModeRadio -> dialogBinding.advanceModeRadio.isChecked = true
            }

            dialogBinding.modeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                Prefs.setScanMode(checkedId)
                when (checkedId) {
                    R.id.basicModeRadio -> binding.scanModeText.text = "Basic Mode"
                    R.id.basicModeFilterRadio -> binding.scanModeText.text = "Basic Mode with Filters"
                    R.id.advanceModeRadio -> binding.scanModeText.text = "Advance Mode"
                }
                dialog.dismiss()
            }
        }



        dialog.show()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setDimAmount(0.8F)
            attributes.windowAnimations = R.style.DialogAnimation
            setGravity(Gravity.BOTTOM)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            // Ensure visibility flags are set

            WindowInsetsControllerCompat(this, this.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }



        }
    }




}