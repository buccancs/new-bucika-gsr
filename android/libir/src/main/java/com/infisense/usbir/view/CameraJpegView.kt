package com.infisense.usbir.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import com.energy.iruvc.utils.SynchronizedBitmap

class CameraJpegView @JvmOverloads constructor(
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
    
    init {
        runnable = Runnable {
            var canvas: Canvas?
            
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
                            
                            bitmap?.let { bmp ->
                                val mScaledBitmap = Bitmap.createScaledBitmap(bmp, width, height, true)
                                canvas?.drawBitmap(mScaledBitmap, 0f, 0f, null)
                                
                                val paint = Paint().apply {
                                    strokeWidth = 2f
                                    isAntiAlias = true
                                    color = Color.WHITE
                                }
                                
                                val crossLen = 20
                                val centerX = width / 2f
                                val centerY = height / 2f
                                
                                canvas?.apply {
                                    drawLine(centerX - crossLen, centerY, centerX + crossLen, centerY, paint)
                                    drawLine(centerX, centerY - crossLen, centerX, centerY + crossLen, paint)
                                }
                                
                                canvas?.let { unlockCanvasAndPost(it) }
                            }
                            sync.valid = false
                        }
                    }
                }
                
                try {
                    Thread.sleep(1)
                } catch (e: InterruptedException) {
                    Log.d(TAG, "sleep crash")
                    e.printStackTrace()
                    cameraThread?.interrupt()
                }
            }
            Log.w(TAG, "DisplayThread exit:")
        }
    }
    
    fun setBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap
    }
    
    fun setSyncimage(syncimage: SynchronizedBitmap?) {
        this.syncimage = syncimage
    }
    
    fun start() {
        cameraThread = Thread(runnable).apply { start() }
    }
    
    fun stop() {
        cameraThread?.let { thread ->
            thread.interrupt()
            try {
                thread.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}