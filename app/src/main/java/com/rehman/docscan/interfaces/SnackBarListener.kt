package com.rehman.docscan.interfaces

import androidx.annotation.ColorRes
import com.rehman.docscan.R

interface SnackBarListener {
    fun showSnackBar(message: String ,@ColorRes backgroundColor: Int = R.color.color_secondary, @ColorRes textColor: Int = R.color.color_on_secondary ,duration: Long = 3000L)
}