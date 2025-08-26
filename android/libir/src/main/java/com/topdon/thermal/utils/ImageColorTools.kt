package com.topdon.thermal.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import com.example.opengl.render.IROpen3DTools

object ImageColorTools {

    init {
        System.loadLibrary("opencv_java4")
    }

    @JvmStatic
    fun testImageTe(buffer: ByteArray): Bitmap {
        val temperature = IntArray(256 * 192 * 2)
        val image = IntArray(256 * 192 * 2)

        var imgNum = 0
        var teNum = 0
        for (i in 1024 until (1024 + 256 * 192 * 2)) {
            image[imgNum++] = buffer[i].toInt()
        }
        for (i in 1024 + 256 * 192 * 2 until (1024 + 2 * 256 * 192 * 2)) {
            temperature[teNum++] = buffer[i].toInt()
        }

        val customMinTemp = 18f
        val customMaxTemp = 25f

        val src = Mat(192, 256, CvType.CV_64F)
        val temp = DoubleArray(256 * 192)
        var t = 0

        for (i in temperature.indices step 2) {
            val value = (temperature[i + 1] shl 8) + temperature[i]
            val divid = 16.0f
            val gValue = (value / 4.0).toFloat() / divid - 273.15f
            temp[t++] = gValue.toDouble()
        }
        src.put(0, 0, temp)

        val imageMat = Mat(192, 256, CvType.CV_8UC2)
        imageMat.put(0, 0, IROpen3DTools.IntArrayToByteArray(image))

        val colorList = intArrayOf(
            Color.parseColor("#ff0000"),
            Color.parseColor("#00ff00"),
            Color.parseColor("#0000ff")
        )

        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_YUV2GRAY_YUYV)

        val imageDst = matToByteArrayBy4(imageMat)
        var j = 0
        val imageDstLength = imageDst.size

        var index = 0
        while (index < imageDstLength) {
            var temperature0 = (temperature[j] and 0xff) + (temperature[j + 1] and 0xff) * 256
            temperature0 = (temperature0 / 64 - 273.15).toInt()
            if (temperature0 >= customMinTemp && temperature0 <= customMaxTemp) {
                val rgb = getOneColorByTempEx(customMaxTemp, customMinTemp, temperature0.toFloat(), colorList)
                rgb?.let { color ->
                    imageDst[index] = color[0].toByte()
                    imageDst[index + 1] = color[1].toByte()
                    imageDst[index + 2] = color[2].toByte()
                }
            }
            imageDst[index + 3] = 255.toByte()
            index += 4
            j += 2
        }

        val outputBitmap = Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
        outputBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageDst))

        src.release()
        imageMat.release()

        return outputBitmap
    }

    @JvmStatic
    private fun getOneColorByTempEx(maxTemp: Float, minTemp: Float, currentTemp: Float, colorList: IntArray): IntArray? {
        if (currentTemp < minTemp || currentTemp > maxTemp || colorList.isEmpty()) {
            return null
        }

        val normalizedTemp = (currentTemp - minTemp) / (maxTemp - minTemp)
        val segmentIndex = (normalizedTemp * (colorList.size - 1)).toInt().coerceIn(0, colorList.size - 2)
        val localRatio = (normalizedTemp * (colorList.size - 1)) - segmentIndex

        val startColor = colorList[segmentIndex]
        val endColor = colorList[segmentIndex + 1]

        return intArrayOf(
            interpolateR(startColor, endColor, localRatio),
            interpolateG(startColor, endColor, localRatio),
            interpolateB(startColor, endColor, localRatio)
        )
    }

    private fun interpolateR(startColor: Int, endColor: Int, ratio: Double): Int {
        val startR = (startColor shr 16) and 0xFF
        val endR = (endColor shr 16) and 0xFF
        return ((1 - ratio) * startR + ratio * endR).toInt()
    }

    private fun interpolateG(startColor: Int, endColor: Int, ratio: Double): Int {
        val startG = (startColor shr 8) and 0xFF
        val endG = (endColor shr 8) and 0xFF
        return ((1 - ratio) * startG + ratio * endG).toInt()
    }

    private fun interpolateB(startColor: Int, endColor: Int, ratio: Double): Int {
        val startB = startColor and 0xFF
        val endB = endColor and 0xFF
        return ((1 - ratio) * startB + ratio * endB).toInt()
    }

    @JvmStatic
    fun matToByteArrayBy4(mat: Mat): ByteArray {
        val byteArray = ByteArray(mat.rows() * mat.cols() * 4)
        mat[0, 0, byteArray]
        return byteArray
    }

    @JvmStatic
    fun matToByteArrayBy3(mat: Mat): ByteArray {
        val byteArray = ByteArray(mat.rows() * mat.cols() * 3)
        mat[0, 0, byteArray]
        return byteArray
    }

    @JvmStatic
    fun bytes2Bimap(b: ByteArray): Bitmap? {
        return if (b.isNotEmpty()) {
            BitmapFactory.decodeByteArray(b, 0, b.size)
        } else null
    }
}