package com.energy.commoncomponent.view.rangeseekbar

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import kotlin.math.*

object Utils {

    private const val TAG = "RangeSeekBar"

    fun print(log: String) {
        Log.d(TAG, log)
    }

    fun print(vararg logs: Any) {
        val stringBuilder = StringBuilder()
        for (log in logs) {
            stringBuilder.append(log)
        }
        Log.d(TAG, stringBuilder.toString())
    }

    fun drawableToBitmap(context: Context?, width: Int, height: Int, drawableId: Int): Bitmap? {
        if (context == null || width <= 0 || height <= 0 || drawableId == 0) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawableToBitmap(width, height, context.resources.getDrawable(drawableId, null))
        } else {
            @Suppress("DEPRECATION")
            drawableToBitmap(width, height, context.resources.getDrawable(drawableId))
        }
    }

    /**
     * make a drawable to a bitmap
     *
     * @param drawable drawable you want convert
     * @return converted bitmap
     */
    fun drawableToBitmap(width: Int, height: Int, drawable: Drawable): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            if (drawable is BitmapDrawable) {
                bitmap = drawable.bitmap
                if (bitmap != null && bitmap.height > 0) {
                    val matrix = Matrix()
                    val scaleWidth = width * 1.0f / bitmap.width
                    val scaleHeight = height * 1.0f / bitmap.height
                    matrix.postScale(scaleWidth, scaleHeight)
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    return bitmap
                }
            }
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmap
    }

    /**
     * draw 9Path
     *
     * @param canvas Canvas
     * @param bmp    9path bitmap
     * @param rect   9path rect
     */
    fun drawNinePath(canvas: Canvas, bmp: Bitmap, rect: Rect) {
        NinePatch.isNinePatchChunk(bmp.ninePatchChunk)
        val patch = NinePatch(bmp, bmp.ninePatchChunk, null)
        patch.draw(canvas, rect)
    }

    fun drawBitmap(canvas: Canvas, paint: Paint?, bmp: Bitmap, rect: Rect) {
        try {
            if (NinePatch.isNinePatchChunk(bmp.ninePatchChunk)) {
                drawNinePath(canvas, bmp, rect)
                return
            }
        } catch (e: Exception) {
            // Ignore exception
        }
        canvas.drawBitmap(bmp, rect.left.toFloat(), rect.top.toFloat(), paint)
    }

    fun dp2px(context: Context?, dpValue: Float): Int {
        if (context == null || compareFloat(0f, dpValue) == 0) return 0
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * Compare the size of two floating point numbers
     *
     * @param a
     * @param b
     * @return 1 is a > b
     * -1 is a < b
     * 0 is a == b
     */
    fun compareFloat(a: Float, b: Float): Int {
        val ta = (a * 1000000).roundToInt()
        val tb = (b * 1000000).roundToInt()
        return when {
            ta > tb -> 1
            ta < tb -> -1
            else -> 0
        }
    }

    /**
     * Compare the size of two floating point numbers with accuracy
     *
     * @param a
     * @param b
     * @return 1 is a > b
     * -1 is a < b
     * 0 is a == b
     */
    fun compareFloat(a: Float, b: Float, degree: Int): Int {
        return if (abs(a - b) < 0.1.pow(degree)) {
            0
        } else {
            if (a < b) -1 else 1
        }
    }

    fun parseFloat(s: String): Float {
        return try {
            s.toFloat()
        } catch (e: NumberFormatException) {
            0f
        }
    }

    fun measureText(text: String, textSize: Float): Rect {
        val paint = Paint()
        val textRect = Rect()
        paint.textSize = textSize
        paint.getTextBounds(text, 0, text.length, textRect)
        paint.reset()
        return textRect
    }

    fun verifyBitmap(bitmap: Bitmap?): Boolean {
        return bitmap != null && !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0
    }

    fun getColor(context: Context?, @ColorRes colorId: Int): Int {
        return if (context != null) {
            ContextCompat.getColor(context.applicationContext, colorId)
        } else {
            Color.WHITE
        }
    }
}
