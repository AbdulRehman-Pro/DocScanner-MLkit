package com.rehman.docscan.core

import android.animation.LayoutTransition
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import com.rehman.docscan.R
import com.rehman.docscan.databinding.CustomSnackbarBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.net.toUri
import androidx.core.os.BuildCompat
import com.rehman.docscan.BuildConfig


object Utils {


    const val APP_FOLDER_NAME = "DocScanner"
    const val APP_FOLDER_NAME_SHORT = "DS"

    const val IS_PREMIUM = false


    fun Activity.showCustomSnackBar(
        message: String,
        anchorView: View,
        @ColorRes backgroundColor: Int,
        @ColorRes textColor: Int,
        duration: Long = 3000L
    ) {
        val rootView = this.findViewById<ViewGroup>(android.R.id.content)

        // Inflate snackBar layout (without attaching)
        val inflater = LayoutInflater.from(this)
        val snackBarBinding = CustomSnackbarBinding.inflate(inflater, rootView, false)
        val snackBarView = snackBarBinding.root
        snackBarBinding.customSnackBar.setBackgroundColor(ContextCompat.getColor(this, backgroundColor))
        snackBarBinding.snackBarText.text = message
        snackBarBinding.snackBarText.setTextColor(ContextCompat.getColor(this, textColor))

        // Measure anchor position in window
        val anchorLocation = IntArray(2)
        anchorView.getLocationInWindow(anchorLocation)
        val anchorY = anchorLocation[1]

        // Measure snackBar height manually
        snackBarView.measure(
            View.MeasureSpec.makeMeasureSpec(rootView.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val snackBarHeight = snackBarView.measuredHeight

        // Set manual position above anchor
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.topMargin = anchorY - snackBarHeight
        snackBarView.layoutParams = layoutParams

        // Initial state for animation
        snackBarView.alpha = 0f
        snackBarView.translationY = 30f

        // Add to root
        rootView.addView(snackBarView)

        // Animate in
        snackBarView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Auto remove after duration
        Handler(Looper.getMainLooper()).postDelayed({
            snackBarView.animate()
                .alpha(0f)
                .translationY(30f)
                .setDuration(250)
                .withEndAction {
                    rootView.removeView(snackBarView)
                }.start()
        }, duration)
    }


    fun getAppVersion(context: Context): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        return (if (BuildConfig.DEBUG) {
            "$versionName ($versionCode)"  // Show code in debug
        } else {
            versionName  // Clean version for production users
        }).toString()
    }


    fun ViewGroup.enableTransition() {
        val transition = LayoutTransition()
        transition.enableTransitionType(LayoutTransition.CHANGING)
        this.layoutTransition = transition

    }



    fun toggleCardDetails(
        detailLayout: View,
        arrow: ImageView
    ) {

        if (detailLayout.isGone) {
            detailLayout.alpha = 0f
            detailLayout.visibility = View.VISIBLE

            // Animate fade in
            detailLayout.animate()
                .alpha(1f)
                .setDuration(300)
                .start()

            arrow.animate()
                .rotation(180f)
                .setDuration(300)
                .start()
        } else {

            detailLayout.alpha = 1f
            detailLayout.visibility = View.GONE

            // Animate fade out
            detailLayout.animate()
                .alpha(0f)
                .setDuration(300)
                .start()

            arrow.animate()
                .rotation(0f)
                .setDuration(300)
                .start()
        }
    }

    fun TextView.applyCustomColor(
        text: String,
        boldTextLengthStart: Int = 0,
        boldTextLengthEnd: Int
    ) {
        val spannableString = SpannableString(text)

        // Bold style (custom font)
        spannableString.setSpan(
            CustomTypefaceSpan(ResourcesCompat.getFont(context, R.font.roboto_medium)!!),
            boldTextLengthStart,
            boldTextLengthEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Regular style for other parts
        if (boldTextLengthStart > 0) {
            spannableString.setSpan(
                CustomTypefaceSpan(ResourcesCompat.getFont(context, R.font.roboto)!!),
                0,
                boldTextLengthStart,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (boldTextLengthEnd < text.length) {
            spannableString.setSpan(
                CustomTypefaceSpan(ResourcesCompat.getFont(context, R.font.roboto)!!),
                boldTextLengthEnd,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Colors
        spannableString.setSpan(
            ForegroundColorSpan(context.getColor(R.color.color_secondary)),
            boldTextLengthStart,
            boldTextLengthEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (boldTextLengthStart > 0) {
            spannableString.setSpan(
                ForegroundColorSpan(context.getColor(R.color.color_text_secondary)),
                0,
                boldTextLengthStart,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (boldTextLengthEnd < text.length) {
            spannableString.setSpan(
                ForegroundColorSpan(context.getColor(R.color.color_text_secondary)),
                boldTextLengthEnd,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        this.text = spannableString
    }


     fun applyCustomFontAndColor(
        context: Context,
        radioButton: RadioButton,
        text: String,
        boldTextLength: Int,
    ) {

        // Define the text with different styles
        val spannableString = SpannableString(text)

        // Apply bold style to "Burst Mode" and set its font to Roboto Bold
        spannableString.setSpan(
            CustomTypefaceSpan(ResourcesCompat.getFont(context, R.font.roboto_bold)!!),
            0,
            boldTextLength,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )


        spannableString.setSpan(
            CustomTypefaceSpan(ResourcesCompat.getFont(context, R.font.roboto)!!),
            boldTextLength + 1,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )


        // Apply white color to "Burst Mode" and grey color to the remaining text
        val whiteColorSpan = ForegroundColorSpan(context.getColor(R.color.color_text_primary))
        val greyColorSpan = ForegroundColorSpan(context.getColor(R.color.color_text_secondary))
        spannableString.setSpan(whiteColorSpan, 0, boldTextLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(
            greyColorSpan,
            boldTextLength + 1,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Increase the size of "Burst Mode"
        spannableString.setSpan(
            RelativeSizeSpan(1.05f),
            0,
            boldTextLength,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            RelativeSizeSpan(0.88f),
            boldTextLength + 1,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )


        // Set the text with styles to the RadioButton
        radioButton.text = spannableString
    }


    private class CustomTypefaceSpan(private val typeface: Typeface) :
        MetricAffectingSpan() {

        override fun updateDrawState(tp: TextPaint) {
            applyCustomTypeFace(tp, typeface)
        }

        override fun updateMeasureState(p: TextPaint) {
            applyCustomTypeFace(p, typeface)
        }

        private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
            paint.typeface = tf
        }
    }


     fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        val contentResolver: ContentResolver = context.contentResolver
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


     fun saveImageToMediaStore(context: Context, bitmap: Bitmap) {


        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME, "IMG-${
                    getCurrentDateAndTime()
                }"
            )
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/$APP_FOLDER_NAME/"
            )
        }

        val uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            }
        }

    }

    fun saveImageToExternalStorage(context: Context, bitmap: Bitmap) {
        val imageDirectory = File(Environment.getExternalStorageDirectory().path + "/$APP_FOLDER_NAME/")
        if (!imageDirectory.exists()) {
            imageDirectory.mkdirs()
        }

        val imageFile =
            File(imageDirectory, "IMG-${getCurrentDateAndTime()}")
        val outputStream: OutputStream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.close()

        // Refresh the MediaScanner to make the image available in the gallery
        MediaScannerConnection.scanFile(
            context,
            arrayOf(imageFile.absolutePath),
            null,
            null
        )
    }

    private fun getCurrentDateAndTime(): String {
        val format = SimpleDateFormat("yyyyMMdd-hhmmssa", Locale.getDefault())
        val dateTime = format.format(Calendar.getInstance().time)
        return if (dateTime.contains("am")) {
            dateTime.replace("am", "1")
        } else {
            dateTime.replace("pm", "2")
        }

    }


    fun getImagesFromMediaStore(context: Context): ArrayList<Uri> {
        val imageUris = arrayListOf<Uri>()
        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%${Environment.DIRECTORY_PICTURES}/$APP_FOLDER_NAME/%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                imageUris.add(uri)
            }
        }

        return imageUris
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        val contentResolver: ContentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        var fileName: String? = null

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                fileName = cursor.getString(nameIndex)
            }
        }

        return fileName
    }

    private fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        val contentResolver: ContentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.Images.Media.SIZE)
        var fileSize: Long = 0

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                fileSize = cursor.getLong(sizeIndex)
            }
        }

        return fileSize
    }

    private fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> String.format("%d bytes", size)
        }
    }

    fun processFileName(context: Context, item: Uri): Pair<String, String>? {
        val fileName = getFileNameFromUri(context, item) ?: return null

        val parts = fileName.split("-")
        if (parts.size < 3) return null

        val date = parts[1]
        val timePart = parts[2].split(".")
        if (timePart.size < 2) return null

        val cleanTime = timePart[0]
        val ext = timePart[1]
        val amPm = when (cleanTime.last()) {
            '1' -> "AM"
            '2' -> "PM"
            else -> ""
        }

        if (cleanTime.length < 4) return null

        val hours = cleanTime.substring(0, 2)
        val minutes = cleanTime.substring(2, 4)
        val formattedTime = "${hours}:${minutes} $amPm"

        val newFileName = "${parts[0]}-$date-$APP_FOLDER_NAME_SHORT${cleanTime.substring(2, 6)}.$ext"
        val fileSize = formatFileSize(getFileSizeFromUri(context,item))
        val description = "$formattedTime, $fileSize, ${ext.uppercase()} image"

        return Pair(newFileName, description)
    }

    fun deleteFileFromUri(context: Context, uri: Uri): Boolean {
        val contentResolver = context.contentResolver
        return try {
            val rowsDeleted = contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun Context.openPlayStoreDevPage() {

        val developerId = "Abdul-Rehman"

        val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "market://developer?id=$developerId".toUri()
            setPackage("com.android.vending")
        }

        val webIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://play.google.com/store/apps/developer?id=$developerId".toUri()
        }

        try {
            Log.d("PlayStoreIntent", "Trying Play Store Intent: ${playStoreIntent.data}")
            startActivity(playStoreIntent)
        } catch (e: ActivityNotFoundException) {
            Log.w("PlayStoreIntent", "Play Store not found, falling back to web: ${webIntent.data}")
            startActivity(webIntent)
        }
    }

    fun Context.openPlayStoreAppPage() {
        val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "market://details?id=${this@openPlayStoreAppPage.packageName}".toUri()
            setPackage("com.android.vending")
        }

        val webIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://play.google.com/store/apps/details?id=${this@openPlayStoreAppPage.packageName}".toUri()
        }

        try {
            Log.d("PlayStoreIntent", "Trying Play Store Intent: ${playStoreIntent.data}")
            startActivity(playStoreIntent)
        } catch (e: ActivityNotFoundException) {
            Log.w("PlayStoreIntent", "Play Store not found, falling back to web: ${webIntent.data}")
            startActivity(webIntent)
        }
    }



}