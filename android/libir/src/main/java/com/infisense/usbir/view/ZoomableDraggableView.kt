package com.infisense.usbir.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.infisense.usbir.R
import com.topdon.lib.core.utils.BitmapUtils
import kotlin.math.max
import kotlin.math.min

class ZoomableDraggableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector
    private val matrix = Matrix()
    private var scaleFactor = 1.0f
    private val minScaleFactor = 0.5f
    private val maxScaleFactor = 2.0f
    private var focusX = 0f
    private var focusY = 0f
    private var lastX = 0f
    private var lastY = 0f
    
    private var originalBitmap: Bitmap? = null
    private var imageWidth = 0
    private var imageHeight = 0
    private var viewWidth = 0
    private var viewHeight = 0
    private var xscale = 0f
    private var yscale = 0f
    private var originalBitmapWidth = 0f
    private var originalBitmapHeight = 0f
    
    private val pxBitmapHeight = 150f
    private var showBitmapHeightWidth = 0f
    private var showBitmapHeight = 0f
    private val paint = Paint()
    private var showBitmap: Bitmap? = null
    
    init {
        init(context)
    }
    
    private fun init(context: Context) {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
        
        originalBitmap = (ContextCompat.getDrawable(
            resources, 
            R.drawable.svg_ic_target_horizontal_person_green
        ) as? BitmapDrawable)?.bitmap
        
        originalBitmap?.let { bitmap ->
            originalBitmapWidth = bitmap.width.toFloat()
            originalBitmapHeight = bitmap.height.toFloat()
        }
    }
    
    fun setImageSize(imageWidth: Int, imageHeight: Int) {
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        
        (parent as? ViewGroup)?.let { parentView ->
            viewWidth = parentView.measuredWidth
            viewHeight = parentView.measuredHeight
        }
        
        if (viewWidth != 0) {
            xscale = viewWidth.toFloat() / imageWidth.toFloat()
        }
        if (viewHeight != 0) {
            yscale = viewHeight.toFloat() / imageHeight.toFloat()
        }
        
        showBitmapHeight = pxBitmapHeight / yscale
        showBitmapHeightWidth = pxBitmapHeight * originalBitmapWidth / originalBitmapHeight * xscale
        
        originalBitmap?.let { bitmap ->
            showBitmap = BitmapUtils.scaleWithWH(bitmap, showBitmapHeightWidth, showBitmapHeight)
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.concat(matrix)
        
        showBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, matrix, paint)
        }
        
        super.onDraw(canvas)
        canvas.restore()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }
    
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(minScaleFactor, min(scaleFactor, maxScaleFactor))
            
            focusX = detector.focusX
            focusY = detector.focusY
            
            matrix.setScale(scaleFactor, scaleFactor, focusX, focusY)
            invalidate()
            
            return true
        }
    }
    
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            lastX = e.x
            lastY = e.y
            return true
        }
        
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val deltaX = e2.x - lastX
            val deltaY = e2.y - lastY
            
            lastX = e2.x
            lastY = e2.y
            
            val adjustedDeltaX = deltaX / scaleFactor
            val adjustedDeltaY = deltaY / scaleFactor
            
            matrix.postTranslate(-adjustedDeltaX, -adjustedDeltaY)
            invalidate()
            
            return true
        }
    }
}