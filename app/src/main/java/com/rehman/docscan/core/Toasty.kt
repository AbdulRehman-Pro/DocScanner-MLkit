package com.rehman.docscan.core

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.PopupWindow
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.rehman.docscan.R
import com.rehman.docscan.databinding.ToastBinding

object Toasty {

    private var currentPopup: PopupWindow? = null
    private var currentDismissHandler: Handler? = null


    fun show(
        context: Context,
        message: String,
        maxLines: Int? = null,
        @ColorRes backgroundColor: Int? = null,
        @ColorRes textColor: Int? = null,
        iconRes: Int? = null,
        iconColor: Int? = null,
        elevation: Int = 20,
        cornerRadius: Int = 30
    ) {
        val inflater = LayoutInflater.from(context)
        val binding = ToastBinding.inflate(inflater)

        // Set message and optional icon
        binding.toastMessage.setTextColor(ContextCompat.getColor(context, textColor ?: android.R.color.white))
        binding.toastMessage.text = message
        if (maxLines != null) {
            binding.toastMessage.maxLines = maxLines
        }

        // Bounce animation for icon
        iconRes?.let {
            binding.toastIcon.setImageResource(it)
            binding.toastIcon.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({

                val scaleX = ObjectAnimator.ofFloat(binding.toastIcon, View.SCALE_X, 0f, 1f)
                val scaleY = ObjectAnimator.ofFloat(binding.toastIcon, View.SCALE_Y, 0f, 1f)

                AnimatorSet().apply {
                    playTogether(scaleX, scaleY)
                    interpolator = OvershootInterpolator() // gives the bounce effect
                    duration = 500
                    start()
                }

            }, 100)

        } ?: run {
            binding.toastIcon.visibility = View.GONE
        }


        // Apply background color and shadow
        binding.toastCard.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                backgroundColor ?: android.R.color.darker_gray
            )
        )
        binding.toastCard.radius = dpToPxFloat(context, cornerRadius)
        binding.toastCard.cardElevation = dpToPxFloat(context, elevation)


        // Dismiss any currently showing toast
        currentDismissHandler?.removeCallbacksAndMessages(null)
        currentPopup?.dismiss()


        val popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isOutsideTouchable = false
            isFocusable = false
            this.elevation = 0f
        }

        currentPopup = popupWindow

        // Attach to the decor view and keep the toast inside the visible window.
        val parentView = (context as Activity).window.decorView.rootView
        val bottomOffset = dpToPxInt(context, 32)

        // Show popup
        popupWindow.showAtLocation(
            parentView,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            0,
            bottomOffset
        )

        // Slide-in animation
        val animIn = AnimationUtils.loadAnimation(context, R.anim.toast_in)
        binding.root.startAnimation(animIn)

        // Dismiss with slide-out animation
        currentDismissHandler = Handler(Looper.getMainLooper()).apply {
            postDelayed({
                val animOut = AnimationUtils.loadAnimation(context, R.anim.toast_out)
                binding.root.startAnimation(animOut)
                Handler(Looper.getMainLooper()).postDelayed({
                    popupWindow.dismiss()
                    if (currentPopup == popupWindow) {
                        currentPopup = null
                    }
                }, animOut.duration)
            }, 3000)
        }
    }


    private fun dpToPxFloat(context: Context, dp: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        )
    }

    private fun dpToPxInt(context: Context, dp: Int): Int {
        return dpToPxFloat(context, dp).toInt()
    }

}


