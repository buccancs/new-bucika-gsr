package com.infisense.usbir.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Matrix

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
    
    /**
     * Scale bitmap to specified width and height
     */
    fun scaleWithWH(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    
    /**
     * Merge two bitmaps together
     */
    fun mergeBitmap(backgroundBitmap: Bitmap, overlayBitmap: Bitmap): Bitmap {
        val mergedBitmap = Bitmap.createBitmap(
            backgroundBitmap.width,
            backgroundBitmap.height,
            backgroundBitmap.config ?: Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(mergedBitmap)
        canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)
        canvas.drawBitmap(overlayBitmap, 0f, 0f, null)
        return mergedBitmap
    }
    
    /**
     * Rotate bitmap by specified degrees
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Draw center label on bitmap for thermal overlays
     */
    fun drawCenterLabel(bitmap: Bitmap, label: String): Bitmap {
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            isAntiAlias = true
        }
        val textWidth = paint.measureText(label)
        val x = (bitmap.width - textWidth) / 2f
        val y = bitmap.height / 2f
        canvas.drawText(label, x, y, paint)
        return bitmap
    }
}