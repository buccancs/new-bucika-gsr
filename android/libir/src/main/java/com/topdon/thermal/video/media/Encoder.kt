package com.topdon.thermal.video.media

import android.graphics.Bitmap
import android.util.Log
import java.util.*

abstract class Encoder {
    
    interface EncodeFinishListener {
        fun onEncodeFinished()
    }
    
    companion object {
        private val TAG = Encoder::class.java.simpleName
        protected const val STATE_IDLE = 0
        protected const val STATE_RECORDING = 1
        protected const val STATE_RECORDING_UNTIL_LAST_FRAME = 2
    }
    
    private var bitmapQueue: MutableList<Bitmap> = Collections.synchronizedList(mutableListOf())
    private var encodeFinishListener: EncodeFinishListener? = null
    private var encodingOptions: EncodingOptions
    private var encodingThread: Thread? = null
    private var frameDelay = 50
    private var height: Int = 0
    protected var outputFilePath: String? = null
    private var state = STATE_IDLE
    private var width: Int = 0
    
    private val mRunnableEncoder = Runnable {
        while (true) {
            if (state != STATE_RECORDING && bitmapQueue.size <= 0) {
                break
            } else if (bitmapQueue.size > 0) {
                var bitmap: Bitmap? = null
                try {
                    bitmap = bitmapQueue.removeAt(0)
                } catch (e: IndexOutOfBoundsException) {
                    Log.e(TAG, e.message.orEmpty())
                }
                bitmap?.let {
                    try {
                        onAddFrame(it)
                    } catch (e: ArrayIndexOutOfBoundsException) {
                        Log.e(TAG, e.message.orEmpty())
                    }
                    it.recycle()
                }
                if (state == STATE_RECORDING_UNTIL_LAST_FRAME && bitmapQueue.isEmpty()) {
                    Log.d(TAG, "Last frame added")
                    break
                }
            }
        }
        Log.d(TAG, "add Frame finished")
        onStop()
        notifyEncodeFinish()
    }
    
    constructor() {
        encodingOptions = setDefaultEncodingOptions()
        init()
    }
    
    constructor(options: EncodingOptions) {
        encodingOptions = options
        init()
    }
    
    private fun init() {
        onInit()
    }
    
    private fun setDefaultEncodingOptions(): EncodingOptions {
        return EncodingOptions().apply {
            compressLevel = 0
        }
    }
    
    fun setOutputFilePath(outputFilePath: String) {
        this.outputFilePath = outputFilePath
    }
    
    fun setOutputSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }
    
    fun setFrameDelay(delay: Int) {
        frameDelay = delay
    }
    
    fun startEncode() {
        bitmapQueue.clear()
        onStart()
        setState(STATE_RECORDING)
        encodingThread = Thread(mRunnableEncoder).apply {
            name = "EncodeThread"
            start()
        }
    }
    
    private fun notifyEncodeFinish() {
        encodeFinishListener?.onEncodeFinished()
    }
    
    fun stopEncode() {
        encodingThread?.takeIf { it.isAlive }?.interrupt()
        setState(STATE_IDLE)
    }
    
    fun addFrame(bitmap: Bitmap) {
        if (state == STATE_RECORDING) {
            bitmapQueue.add(bitmap)
        }
    }
    
    fun setEncodeFinishListener(listener: EncodeFinishListener) {
        encodeFinishListener = listener
    }
    
    fun notifyLastFrameAdded() {
        setState(STATE_RECORDING_UNTIL_LAST_FRAME)
    }
    
    private fun setState(state: Int) {
        this.state = state
    }
    
    protected abstract fun onAddFrame(bitmap: Bitmap)
    protected abstract fun onInit()
    protected abstract fun onStart()
    protected abstract fun onStop()
    
    protected fun getFrameDelay(): Int = frameDelay
    protected fun getHeight(): Int = height
    protected fun getWidth(): Int = width
    protected fun getEncodingOptions(): EncodingOptions = encodingOptions
}