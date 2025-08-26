package com.infisense.usbir.view

import android.graphics.Bitmap

/**
 * BitmapUtils for image scaling and processing operations
 */
object BitmapUtils {
    
    fun scaleBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    fun scaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return scaleBitmap(bitmap, newWidth, newHeight)
    }
}