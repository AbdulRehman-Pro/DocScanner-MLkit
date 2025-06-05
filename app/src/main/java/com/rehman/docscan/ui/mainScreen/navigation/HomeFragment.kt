package com.rehman.docscan.ui.mainScreen.navigation

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.rehman.docscan.R
import com.rehman.docscan.core.Prefs
import com.rehman.docscan.core.Utils.getBitmapFromUri
import com.rehman.docscan.core.Utils.saveImageToExternalStorage
import com.rehman.docscan.core.Utils.saveImageToMediaStore
import com.rehman.docscan.databinding.FragmentHomeBinding
import com.rehman.docscan.interfaces.SnackBarListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    companion object {

        private const val TAG = "HomeFragment"

        private const val FULL_MODE = "FULL"
        private const val BASIC_MODE = "BASE"
        private const val BASIC_MODE_WITH_FILTER = "BASE_WITH_FILTER"

        private const val SINGLE_PAGE = 1
        private const val MULTI_PAGE = -1
    }

    private lateinit var binding: FragmentHomeBinding
    private lateinit var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>

    private var selectedMode = FULL_MODE
    private var selectedPage = MULTI_PAGE
    private var isGalleryImport: Boolean = false


    private var callback: SnackBarListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ensure the host activity implements the interface
        if (context is SnackBarListener) {
            callback = context
        } else {
            throw RuntimeException("$context must implement MyFragmentCallback")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)

        // Initialize the scanner launcher for Google ML Kit, which is used to launch the document scanner.
        scannerLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                handleActivityResult(result)
            }


        // Set click listener for the scanner button. When clicked, it launches the document scanner.
        binding.scannerBtn.setOnClickListener {

            val options =
                GmsDocumentScannerOptions.Builder()
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                    .setGalleryImportAllowed(isGalleryImport)

            when (selectedMode) {
                FULL_MODE -> options.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                BASIC_MODE -> options.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
                BASIC_MODE_WITH_FILTER -> options.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER)
                else -> Log.e(TAG, "Unknown selectedMode: $selectedMode")
            }

            if (selectedPage == SINGLE_PAGE) {
                options.setPageLimit(SINGLE_PAGE)
            }

            GmsDocumentScanning.getClient(options.build())
                .getStartScanIntent(requireActivity())
                .addOnSuccessListener { intentSender: IntentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                .addOnFailureListener() { e: Exception ->
                    Log.e(TAG, "Exception -> $e")
                }
        }


        return binding.root
    }


    private fun handleActivityResult(activityResult: ActivityResult) {
        val resultCode = activityResult.resultCode
        val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
        if (resultCode == Activity.RESULT_OK && result != null) {

            Log.i(TAG, "Scan Result -> $result")


            lifecycleScope.launch(Dispatchers.IO) {
                val pages = result.pages
                if (pages!!.isNotEmpty()) {
                    pages.forEach {
                        val bitmap = getBitmapFromUri(requireContext(), it.imageUri)
                        if (bitmap != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                saveImageToMediaStore(requireContext(), bitmap)
                            } else {
                                saveImageToExternalStorage(requireContext(), bitmap)
                            }

                        } else {
                            Log.e(TAG, "Failed to get bitmap from URI")
                            callback?.showSnackBar(
                                "Failed to get image",
                                R.color.color_error,
                                R.color.color_text_primary,
                                2000L
                            )
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (pages.size > 1) {
                            callback?.showSnackBar(
                                "Images saved successfully.",
                                R.color.color_on_primary,
                                R.color.color_on_secondary,
                                2000L
                            )
                        } else {
                            callback?.showSnackBar(
                                "Image saved successfully.",
                                R.color.color_on_primary,
                                R.color.color_on_secondary,
                                2000L
                            )
                        }
                    }

                }
            }


        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.e("Scanner", "Scan Result -> Cancelled...")
            callback?.showSnackBar(
                "Cancelled...",
                R.color.color_error,
                R.color.color_text_primary,
                2000L
            )

        } else {
            Log.e("Scanner", "Scan Result -> Failed...")
        }
    }


    override fun onResume() {
        super.onResume()

        when (Prefs.getScanMode(requireContext())) {
            getString(R.string.basic_mode) -> selectedMode = BASIC_MODE
            getString(R.string.basic_mode_with_filters) -> selectedMode = BASIC_MODE_WITH_FILTER
            getString(R.string.advance_mode) -> selectedMode = FULL_MODE
        }
        when (Prefs.getImageLimit(requireContext())) {
            getString(R.string.single_mode) -> selectedPage = SINGLE_PAGE
            getString(R.string.burst_mode) -> selectedPage = MULTI_PAGE
        }
        isGalleryImport = Prefs.getImportFromGallery()

    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }


}