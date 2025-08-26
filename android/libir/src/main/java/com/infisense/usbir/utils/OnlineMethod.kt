package com.infisense.usbir.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc.*
import java.io.IOException
import java.util.*

object OnlineMethod {

    init {
        System.loadLibrary("opencv_java4")
    }

    @JvmStatic
    @Throws(IOException::class)
    fun drawHighTempEdge(image: ByteArray, temperature: ByteArray, highT: Double, colorH: Int, type: Int): Mat {
        val temp = DoubleArray(256 * 192)
        var t = 0

        for (i in temperature.indices) {
            if (i % 2 == 0) {
                val value = (temperature[i + 1].toInt() shl 8) + temperature[i].toInt()
                val divid = 16.0
                val g = (value / 4.0) / divid - 273.15
                temp[t] = g
                t++
            }
        }

        val im = Mat(192, 256, CvType.CV_8UC2)
        im.put(0, 0, image)
        cvtColor(im, im, COLOR_YUV2GRAY_YUYV)
        Core.normalize(im, im, 0.0, 255.0, Core.NORM_MINMAX)
        im.convertTo(im, CvType.CV_8UC1)
        applyColorMap(im, im, 15)

        val tem = Mat(192, 256, CvType.CV_64FC1)
        tem.put(0, 0, temp)
        tem.convertTo(tem, CvType.CV_8UC1)

        val thresGray = Mat()
        threshold(tem, thresGray, highT, 255.0, THRESH_BINARY)

        val cnts = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        findContours(thresGray, cnts, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE)

        val b = Integer.toString(colorH and 255, 2).toInt(2)
        val gc = colorH shr 8
        val g = Integer.toString(gc and 255, 2).toInt(2)
        val rc = colorH shr 16
        val r = Integer.toString(rc and 255, 2).toInt(2)
        val color = Scalar(b.toDouble(), g.toDouble(), r.toDouble())

        cnts.forEachIndexed { i, contour ->
            val contour2f = MatOfPoint2f(*contour.toArray())
            val approxCurve = MatOfPoint2f()
            approxPolyDP(contour2f, approxCurve, 0.0, true)
            val points = MatOfPoint(*approxCurve.toArray())
            val rect = boundingRect(points)
            val area = contourArea(points)

            if (area > 300) {
                when (type) {
                    1 -> drawContours(im, cnts, i, color, 1, 8)
                    else -> rectangle(im, rect.tl(), rect.br(), color, 1, 8, 0)
                }
            }
        }

        return im
    }

    @JvmStatic
    @Throws(IOException::class)
    fun drawTempEdge(src: Mat, temperature: ByteArray, lowT: Double, colorL: Int, type: Int): Mat {
        val temp = DoubleArray(256 * 192)
        var t = 0

        for (i in temperature.indices) {
            if (i % 2 == 0) {
                val value = (temperature[i + 1].toInt() shl 8) + temperature[i].toInt()
                val divid = 16.0
                val g = (value / 4.0) / divid - 273.15
                temp[t] = g
                t++
            }
        }

        val tem = Mat(192, 256, CvType.CV_64FC1)
        tem.put(0, 0, temp)
        tem.convertTo(tem, CvType.CV_8UC1)

        val thresGray = Mat()
        threshold(tem, thresGray, lowT, 255.0, 4)

        val cnts = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        findContours(thresGray, cnts, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE)

        val b = Integer.toString(colorL and 255, 2).toInt(2)
        val gc = colorL shr 8
        val g = Integer.toString(gc and 255, 2).toInt(2)
        val rc = colorL shr 16
        val r = Integer.toString(rc and 255, 2).toInt(2)
        val color = Scalar(b.toDouble(), g.toDouble(), r.toDouble())

        cnts.forEachIndexed { i, contour ->
            val contour2f = MatOfPoint2f(*contour.toArray())
            val approxCurve = MatOfPoint2f()
            approxPolyDP(contour2f, approxCurve, 0.0, true)
            val points = MatOfPoint(*approxCurve.toArray())
            val rect = boundingRect(points)
            val area = contourArea(points)

            if (area > 300) {
                when (type) {
                    1 -> drawContours(src, cnts, i, color, 1, 8)
                    else -> rectangle(src, rect.tl(), rect.br(), color, 1, 8, 0)
                }
            }
        }

        return src
    }

    @JvmStatic
    fun adjustPhotoRotation(bm: Bitmap, orientationDegree: Int): Bitmap? {
        val m = Matrix()
        m.setRotate(orientationDegree.toFloat(), bm.width.toFloat() / 2, bm.height.toFloat() / 2)
        return try {
            Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, m, true)
        } catch (ex: OutOfMemoryError) {
            null
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun drawEdgeFromTempReigonByte(
        image: ByteArray,
        temperature: ByteArray,
        row: Int,
        col: Int,
        highT: Double,
        lowT: Double,
        colorH: Int,
        colorL: Int,
        type: Int
    ): ByteArray {
        val src = drawHighTempEdge(image, temperature, highT, colorH, type)
        val mat = drawTempEdge(src, temperature, lowT, colorL, type)
        cvtColor(mat, mat, COLOR_RGB2RGBA)
        val dstBitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, dstBitmap)
        return ByteArray(192 * 256 * 4)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun drawEdgeFromTempReigon(
        image: ByteArray,
        temperature: ByteArray,
        row: Int,
        col: Int,
        highT: Double,
        lowT: Double,
        colorH: Int,
        colorL: Int,
        type: Int
    ): Mat {
        val src = drawHighTempEdge(image, temperature, highT, colorH, type)
        val mat = drawTempEdge(src, temperature, lowT, colorL, type)
        cvtColor(mat, mat, COLOR_RGB2RGBA)
        val dstBitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, dstBitmap)
        return drawTempEdge(src, temperature, lowT, colorL, type)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun drawEdgeFromTempReigonBitmap(
        image: ByteArray,
        temperature: ByteArray,
        row: Int,
        col: Int,
        highT: Double,
        lowT: Double,
        colorH: Int,
        colorL: Int,
        type: Int
    ): Bitmap {
        val src = drawHighTempEdge(image, temperature, highT, colorH, type)
        val mat = drawTempEdge(src, temperature, lowT, colorL, type)
        cvtColor(mat, mat, COLOR_BGR2RGBA)
        val dstBitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, dstBitmap)
        return dstBitmap
    }
}