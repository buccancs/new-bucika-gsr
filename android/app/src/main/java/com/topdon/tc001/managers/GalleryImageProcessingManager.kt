package com.topdon.tc001.managers

import android.graphics.Bitmap
import androidx.lifecycle.lifecycleScope
import com.energy.iruvc.utils.CommonParams
import com.infisense.usbir.utils.OpencvTools
import com.topdon.lib.core.utils.BitmapUtils
import com.infisense.usbir.utils.PseudocodeUtils.changePseudocodeModeByOld
import com.topdon.tc001.IRGalleryEditActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleryImageProcessingManager(
    private val activity: IRGalleryEditActivity
) {
    
    companion object {
        const val IMAGE_WIDTH = 256
        const val IMAGE_HEIGHT = 192
        const val DEFAULT_PSEUDOCODE_MODE = 3
        private const val TAG = "GalleryImageProcessingManager"
    }
    
    private var mFrame = ByteArray(IMAGE_HEIGHT * IMAGE_WIDTH * 4)
    
    private var pseudocodeMode = DEFAULT_PSEUDOCODE_MODE
    private var leftValue = 0f
    private var rightValue = 10000f
    private var maxTemp = 10000f
    private var minTemp = 0f
    
    fun initialize() {
        setupDefaultParameters()
        initializeFrameProcessing()
    }
    
    suspend fun processFrameData(frameData: ByteArray): Bitmap? = withContext(Dispatchers.Default) {
        try {

            val processedFrame = processFrame(
                frameData, 
                pseudocodeMode, 
                leftValue, 
                rightValue
            )
            
            generateDisplayBitmap(processedFrame)
            
        } catch (e: Exception) {

            null
        }
    }
    
    fun updatePseudocodeMode(mode: Int) {
        if (mode in 0..15) {
            pseudocodeMode = mode

            activity.lifecycleScope.launch {
                reprocessCurrentFrame()
            }
        }
    }
    
    fun updateTemperatureRange(minTemp: Float, maxTemp: Float) {
        if (minTemp < maxTemp) {
            this.minTemp = minTemp
            this.maxTemp = maxTemp
            this.leftValue = minTemp
            this.rightValue = maxTemp
            
            activity.lifecycleScope.launch {
                reprocessCurrentFrame()
            }
        }
    }
    
    private suspend fun generateDisplayBitmap(frameData: ByteArray): Bitmap? = withContext(Dispatchers.Default) {
        try {

            val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
            
            processFrameToBitmap(frameData, bitmap)
            
            bitmap
            
        } catch (e: Exception) {
            null
        }
    }
    
    private fun processFrame(frameData: ByteArray, pseudoMode: Int, leftVal: Float, rightVal: Float): ByteArray {

        val processedFrame = changePseudocodeModeByOld(frameData, pseudoMode, leftVal, rightVal)
        return processedFrame ?: frameData
    }
    
    private fun processFrameToBitmap(frameData: ByteArray, bitmap: Bitmap) {

        val pixels = IntArray(IMAGE_WIDTH * IMAGE_HEIGHT)
        
        for (i in pixels.indices) {
            val frameIndex = i * 4
            if (frameIndex + 3 < frameData.size) {
                val b = frameData[frameIndex].toInt() and 0xFF
                val g = frameData[frameIndex + 1].toInt() and 0xFF  
                val r = frameData[frameIndex + 2].toInt() and 0xFF
                val a = frameData[frameIndex + 3].toInt() and 0xFF
                
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        bitmap.setPixels(pixels, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT)
    }
    
    private fun setupDefaultParameters() {
        pseudocodeMode = DEFAULT_PSEUDOCODE_MODE
        leftValue = 0f
        rightValue = 10000f
        maxTemp = 10000f
        minTemp = 0f
    }
    
    private fun initializeFrameProcessing() {

        mFrame = ByteArray(IMAGE_HEIGHT * IMAGE_WIDTH * 4)
    }
    
    private suspend fun reprocessCurrentFrame() {

    }
    
    fun getCurrentParameters(): ProcessingParameters {
        return ProcessingParameters(
            pseudocodeMode = pseudocodeMode,
            minTemp = minTemp,
            maxTemp = maxTemp,
            leftValue = leftValue,
            rightValue = rightValue
        )
    }
    
    data class ProcessingParameters(
        val pseudocodeMode: Int,
        val minTemp: Float,
        val maxTemp: Float,
        val leftValue: Float,
        val rightValue: Float
    )
    
    fun cleanup() {

    }
