package com.rehman.docscan.core

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.rehman.docscan.R
import com.rehman.docscan.databinding.CustomSnackbarBinding


object Utils {


    fun View.showSnackBar(message: String, anchorView: View? = null, marginBottom: Int = 30) {
        val snackBar = Snackbar.make(this, message, Snackbar.LENGTH_SHORT)

        // Set background and text color
        snackBar.setBackgroundTint(ContextCompat.getColor(this.context, R.color.color_secondary))
        snackBar.setTextColor(ContextCompat.getColor(this.context, R.color.color_primary))
        snackBar.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
        snackBar.setTextMaxLines(3)

        // Get SnackBar view
        val snackBarView = snackBar.view
        val snackBarText =
            snackBarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        snackBarText.typeface = ResourcesCompat.getFont(context, R.font.roboto_bold)
        snackBarView.translationZ = 0f


        val layoutParams = snackBarView.layoutParams as FrameLayout.LayoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        layoutParams.setMargins(0, 0, 0, 0)
        snackBarView.layoutParams = layoutParams


        if (anchorView != null)
            snackBar.setAnchorView(anchorView)

        // Show the SnackBar
        snackBar.show()

    }


    fun Fragment.showSnackBar(message: String, anchorView: View? = null, marginBottom: Int = 30) {
        view?.showSnackBar(message, anchorView, marginBottom)
    }

    fun Activity.showCustomSnackBar(
        message: String,
        anchorView: View,
        duration: Long = 3000L
    ) {
        val rootView = this.findViewById<ViewGroup>(android.R.id.content)

        // Inflate snackbar layout (without attaching)
        val inflater = LayoutInflater.from(this)
        val snackBarBinding = CustomSnackbarBinding.inflate(inflater, rootView, false)
        val snackbarView = snackBarBinding.root
        snackBarBinding.snackBarText.text = message

        // Measure anchor position in window
        val anchorLocation = IntArray(2)
        anchorView.getLocationInWindow(anchorLocation)
        val anchorY = anchorLocation[1]

        // Measure snackbar height manually
        snackbarView.measure(
            View.MeasureSpec.makeMeasureSpec(rootView.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val snackbarHeight = snackbarView.measuredHeight

        // Set manual position above anchor
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.topMargin = anchorY - snackbarHeight
        snackbarView.layoutParams = layoutParams

        // Initial state for animation
        snackbarView.alpha = 0f
        snackbarView.translationY = 30f

        // Add to root
        rootView.addView(snackbarView)

        // Animate in
        snackbarView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Auto remove after duration
        Handler(Looper.getMainLooper()).postDelayed({
            snackbarView.animate()
                .alpha(0f)
                .translationY(30f)
                .setDuration(250)
                .withEndAction {
                    rootView.removeView(snackbarView)
                }.start()
        }, duration)
    }




}