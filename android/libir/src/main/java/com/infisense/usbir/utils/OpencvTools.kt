package com.infisense.usbir.utils

import android.graphics.Bitmap
import android.util.Log
import com.example.suplib.wrapper.SupHelp
import com.topdon.lib.core.BaseApplication
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.CLAHE
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*
import org.opencv.core.Core.*
import org.opencv.core.CvType.*
import java.nio.ByteBuffer
import java.util.*

object OpencvTools {
    
    init {
        System.loadLibrary("opencv_java4")
    }
    
    private var resultMat = Mat()
    
    fun supImageMix(imageARGB: ByteArray, width: Int, height: Int, resulARGB: ByteArray): ByteArray {
        val argbMat = Mat(width, height, CV_8UC4).apply {
            put(0, 0, imageARGB)
        }
        
        val downscaledMat = Mat()
        resize(argbMat, downscaledMat, Size((height / 2).toDouble(), (width / 2).toDouble()))
        
        val bgrMat = Mat()
        cvtColor(downscaledMat, bgrMat, COLOR_RGBA2BGR)
        
        try {
            SupHelp.getInstance().runImage(bgrMat, resultMat)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        
        val resulArgbMat = Mat()
        cvtColor(resultMat, resulArgbMat, COLOR_BGR2RGBA)
        
        val dstBitmap = Bitmap.createBitmap(resulArgbMat.width(), resulArgbMat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resulArgbMat, dstBitmap)
        val byteBuffer = ByteBuffer.wrap(resulARGB)
        dstBitmap.copyPixelsToBuffer(byteBuffer)
        return resulARGB
    }
    
    fun supImageFour(inBitmap: Bitmap): Bitmap {
        val startTime = System.currentTimeMillis()
        val rawData = ByteBuffer.wrap(SupRUtils.bitmapToByteArray(inBitmap))
        val dataIn = ByteBuffer.allocateDirect(rawData.array().size).apply {
            put(rawData)
        }
        val dataOut = ByteBuffer.allocateDirect(rawData.array().size * 4)
        SupHelp.getInstance().imgUpScalerFour(BaseApplication.instance, dataIn, dataOut)
        
        val byteArray = ByteArray(dataOut.capacity())
        dataOut.get(byteArray)
        Log.e("4倍超分模型：", (System.currentTimeMillis() - startTime).toString())
        return SupRUtils.byteArrayToBitmap(byteArray)
    }
    
    fun supImageFourExToByte(imgByte: ByteArray): ByteArray {
        val startTime = System.currentTimeMillis()
        val dataIn = ByteBuffer.wrap(imgByte)
        val dataOut = ByteBuffer.allocateDirect(imgByte.size * 4)
        
        SupHelp.getInstance().imgUpScalerFour(BaseApplication.instance, dataIn, dataOut)
        Log.e("AI_UPSCALE 4倍超分模型2：", (System.currentTimeMillis() - startTime).toString())
        
        val outputData = ByteArray(dataOut.capacity())
        dataOut.get(outputData)
        Log.e("4倍超分模型：", (System.currentTimeMillis() - startTime).toString())
        val bitmap = SupRUtils.byteArrayToBitmap(outputData)
        return outputData
    }
    
    fun supImageFourExToBitmap(dstArgbBytes: ByteArray, width: Int, height: Int): Bitmap {
        val startTime = System.currentTimeMillis()
        
        val dataIn = ByteBuffer.allocateDirect(dstArgbBytes.size).apply {
            put(dstArgbBytes)
        }
        val dataOut = ByteBuffer.allocateDirect(dstArgbBytes.size * 4)
        
        SupHelp.getInstance().imgUpScalerFour(BaseApplication.instance, dataIn, dataOut)
        Log.e("AI_UPSCALE 4倍超分模型2：", "${System.currentTimeMillis() - startTime}////${dstArgbBytes.size}")
        
        val outputData = ByteArray(dataOut.capacity())
        dataOut.get(outputData)
        
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outputBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(outputData))
        
        val srcMat = Mat()
        Utils.bitmapToMat(outputBitmap, srcMat)
        
        val dstMat = Mat()
        resize(srcMat, dstMat, Size((srcMat.cols() * 4).toDouble(), (srcMat.rows() * 4).toDouble()))
        
        val finalBitmap = Bitmap.createBitmap(dstMat.cols(), dstMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dstMat, finalBitmap)
        
        srcMat.release()
        dstMat.release()
        Log.e("4倍超分模型：", (System.currentTimeMillis() - startTime).toString())
        
        return finalBitmap
    }
    
    fun supImageFourExToBitmap(inBitmap: Bitmap): Bitmap {
        val startTime = System.currentTimeMillis()
        
        val rawData = SupRUtils.bitmapToByteArray(inBitmap)
        
        val dataIn = ByteBuffer.allocateDirect(rawData.size).apply {
            put(rawData)
        }
        val dataOut = ByteBuffer.allocateDirect(256 * 192 * 4 * 4)
        
        SupHelp.getInstance().imgUpScalerFour(BaseApplication.instance, dataIn, dataOut)
        Log.e("AI_UPSCALE 4倍超分模型2：", "${System.currentTimeMillis() - startTime}////${rawData.size}")
        
        val outputData = ByteArray(dataOut.capacity())
        dataOut.get(outputData)
        
        val outputBitmap = SupRUtils.byteArrayToBitmap(outputData)
        
        val srcMat = Mat()
        Utils.bitmapToMat(outputBitmap, srcMat)
        
        val dstMat = Mat()
        resize(srcMat, dstMat, Size((srcMat.cols() * 4).toDouble(), (srcMat.rows() * 4).toDouble()))
        
        val finalBitmap = Bitmap.createBitmap(dstMat.cols(), dstMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dstMat, finalBitmap)
        
        srcMat.release()
        dstMat.release()
        Log.e("4倍超分模型：", (System.currentTimeMillis() - startTime).toString())
        return finalBitmap
    }
    
    fun supImage(imageARGB: ByteArray, width: Int, height: Int, resulARGB: ByteArray): ByteArray {
        val argbMat = Mat(width, height, CV_8UC4).apply {
            put(0, 0, imageARGB)
        }
        
        val bgrMat = Mat()
        cvtColor(argbMat, bgrMat, COLOR_RGBA2BGR)
        try {
            SupHelp.getInstance().runImage(bgrMat, resultMat)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        val resulArgbMat = Mat()
        cvtColor(resultMat, resulArgbMat, COLOR_BGR2RGBA)
        
        val dstBitmap = Bitmap.createBitmap(resulArgbMat.width(), resulArgbMat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resulArgbMat, dstBitmap)
        val byteBuffer = ByteBuffer.wrap(resulARGB)
        dstBitmap.copyPixelsToBuffer(byteBuffer)
        return resulARGB
    }
    
    fun convertSingleByteToDoubleByte(singleByteImage: ByteArray?): ByteArray {
        requireNotNull(singleByteImage) { "输入的byte数组不能为null" }
        val singleLength = singleByteImage.size
        val doubleLength = singleLength * 2
        val doubleByteImage = ByteArray(doubleLength)
        
        for (i in 0 until singleLength) {
            doubleByteImage[2 * i] = singleByteImage[i]
        }
        return doubleByteImage
    }
    
    fun convertCelsiusToOriginalBytes(temp: FloatArray?): ByteArray {
        if (temp == null) {
            return ByteArray(0)
        }
        var maxValue = 0f
        
        val temperature = ByteArray(temp.size * 2)
        for (i in temp.indices) {
            val j = i * 2
            if (maxValue < temp[i]) {
                maxValue = temp[i]
            }
            
            val tempInKelvin = temp[i] + 273.15f
            val originalValue = tempInKelvin * 64
            val intValue = originalValue.toInt()
            
            val low = (intValue and 0xFF).toByte()
            val high = ((intValue shr 8) and 0xFF).toByte()
            
            temperature[j] = low
            temperature[j + 1] = high
        }
        return temperature
    }
    
    fun getColorByTemp(customMaxTemp: Float, customMinTemp: Float, colorList: IntArray): LinkedHashMap<Int, IntArray> {
        val temp = 0.1f
        val tempValue = customMaxTemp - customMinTemp
        val map = LinkedHashMap<Int, IntArray>()
        
        var i = customMinTemp
        while (i <= customMaxTemp) {
            val time = System.currentTimeMillis()
            val ratio = (i - customMinTemp) / tempValue
            val colorNumber = colorList.size - 1
            val avg = 1f / colorNumber
            var colorIndex = colorNumber
            
            for (index in 1..colorNumber) {
                if (ratio == 0f) {
                    colorIndex = 0
                    break
                }
                if (ratio < (avg * index)) {
                    colorIndex = index
                    break
                }
            }
            
            val adjustedRatio = (ratio - (avg * (colorIndex - 1))) / avg
            val r = interpolateR(lastColor(colorList, colorIndex), colorList[colorIndex], adjustedRatio)
            val g = interpolateG(lastColor(colorList, colorIndex), colorList[colorIndex], adjustedRatio)
            val b = interpolateB(lastColor(colorList, colorIndex), colorList[colorIndex], adjustedRatio)
            
            val intKey = (i * 10).toInt()
            val rgb = intArrayOf(r, g, b)
            map[intKey] = rgb
            i += temp
        }
        return map
    }
    
    fun matToByteArray(mat: Mat): ByteArray {
        val rows = mat.rows()
        val cols = mat.cols()
        val type = mat.type()
        val channels = CvType.channels(type)
        val depth = CvType.depth(type)
        
        val data = ByteArray(rows * cols * channels)
        mat.get(0, 0, data)
        return data
    }
    
    // Helper functions for color interpolation
    private fun lastColor(colorList: IntArray, colorIndex: Int): Int {
        return if (colorIndex > 0) colorList[colorIndex - 1] else colorList[0]
    }
    
    private fun interpolateR(color1: Int, color2: Int, ratio: Float): Int {
        val r1 = (color1 shr 16) and 0xFF
        val r2 = (color2 shr 16) and 0xFF
        return (r1 + ratio * (r2 - r1)).toInt()
    }
    
    private fun interpolateG(color1: Int, color2: Int, ratio: Float): Int {
        val g1 = (color1 shr 8) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        return (g1 + ratio * (g2 - g1)).toInt()
    }
    
    private fun interpolateB(color1: Int, color2: Int, ratio: Float): Int {
        val b1 = color1 and 0xFF
        val b2 = color2 and 0xFF
        return (b1 + ratio * (b2 - b1)).toInt()
    }
}