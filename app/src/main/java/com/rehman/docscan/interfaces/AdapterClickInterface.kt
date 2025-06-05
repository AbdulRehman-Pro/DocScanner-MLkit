package com.rehman.docscan.interfaces

import android.net.Uri
import android.widget.ImageView

interface AdapterClickInterface {
    fun itemClick(uri: ArrayList<Uri>, position: Int, itemImage: ImageView)
    fun shareClick(uri: Uri)
    fun deleteClick(uri: Uri, position: Int)
}