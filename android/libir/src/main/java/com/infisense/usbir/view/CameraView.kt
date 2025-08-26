package com.infisense.usbir.view

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.energy.iruvc.utils.SynchronizedBitmap
import com.infisense.usbdual.Const

class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr) {
    
    companion object {
        private const val TAG = "CameraView"
    }
    
    private var bitmap: Bitmap? = null
    private var syncimage: SynchronizedBitmap? = null
    private val runnable: Runnable
    private var cameraThread: Thread? = null
    private var canvas: Canvas? = null
    
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        strokeWidth = 2f
        isAntiAlias = true
        isDither = true
        color = Color.WHITE
    }
    
    private var crossLen = 20
    
    private val greenPaint = Paint().apply {
        strokeWidth = 6f
        textSize = 56f
        color = Color.GREEN
    }
    
    private var drawLine = true
    var productType = Const.TYPE_IR
    private var irWidth = 192
    private var irHeight = 256
    private var isOpenAmplify = false
    
    init {
        runnable = Runnable {
            while (cameraThread?.isInterrupted != true) {
                syncimage?.let { sync ->
                    synchronized(sync.viewLock) {
                        if (!sync.valid) {
                            try {
                                sync.viewLock.wait()
                            } catch (e: InterruptedException) {
                                cameraThread?.interrupt()
                                Log.e(TAG, "lock.wait(): catch an interrupted exception")
                            }
                        }
                        
                        if (sync.valid) {
                            canvas = lockCanvas()
                            if (canvas == null) return@synchronized
                            
                            paint.apply {
                                strokeWidth = 2f
                                isAntiAlias = true
                                isDither = true
                                color = Color.WHITE
                            }
                            
                            bitmap?.let { bmp ->
                                val mScaledBitmap = Bitmap.createScaledBitmap(bmp, width, height, true)
                                canvas?.drawBitmap(mScaledBitmap, 0f, 0f, null)
                                
                                if (drawLine) {
                                    val centerX = width / 2f
                                    val centerY = height / 2f
                                    canvas?.apply {
                                        drawLine(centerX - crossLen, centerY, centerX + crossLen, centerY, paint)
                                        drawLine(centerX, centerY - crossLen, centerX, centerY + crossLen, paint)
                                    }
                                }
                            }
                            
                            canvas?.let { unlockCanvasAndPost(it) }
                            sync.valid = false
                        }
                    }
                }
                SystemClock.sleep(1)
            }
            Log.w(TAG, "DisplayThread exit:")
        }
    }
    
    fun isOpenAmplify(): Boolean = isOpenAmplify
    
    fun setOpenAmplify(openAmplify: Boolean) {
        isOpenAmplify = openAmplify
    }
    
    fun setImageSize(irWidth: Int, irHeight: Int) {
        this.irWidth = irWidth
        this.irHeight = irHeight
    }
    
    fun isDrawLine(): Boolean = drawLine
    
    fun setDrawLine(drawLine: Boolean) {
        this.drawLine = drawLine
    }
    
    fun setBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap
    }
    
    @Nullable
    override fun getBitmap(): Bitmap? = bitmap
    
    fun setSyncimage(syncimage: SynchronizedBitmap?) {
        this.syncimage = syncimage
    }
    
    @NonNull
    fun getScaledBitmap(): Bitmap {
        return syncimage?.let { sync ->
            synchronized(sync.viewLock) {
                bitmap?.let { bmp ->
                    Bitmap.createScaledBitmap(bmp, width, height, true)
                } ?: throw IllegalStateException("Bitmap is null")
            }
        } ?: throw IllegalStateException("Syncimage is null")
    }
    
    fun start() {
        cameraThread = Thread(runnable).apply { start() }
    }
    
    fun setShowCross(isShow: Boolean) {
        try {
            crossLen = if (isShow) 20 else 0
            canvas?.let { c ->
                val centerX = width / 2f
                val centerY = height / 2f
                c.drawLine(centerX - crossLen, centerY, centerX + crossLen, centerY, paint)
                c.drawLine(centerX, centerY - crossLen, centerX, centerY + crossLen, paint)
            }
        } catch (e: Exception) {
            Log.e(TAG, "点异常: ${e.message}")
        }
    }
    
    fun stop() {
        try {
            cameraThread?.let { thread ->
                thread.interrupt()
                thread.join()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}