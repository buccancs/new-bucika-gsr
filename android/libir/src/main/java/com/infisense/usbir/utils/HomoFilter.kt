package com.infisense.usbir.utils

import org.opencv.core.*
import org.opencv.imgproc.CLAHE
import org.opencv.imgproc.Imgproc
import org.opencv.core.Core.*
import org.opencv.core.CvType.*
import org.opencv.imgproc.Imgproc.*

object HomoFilter {
    
    fun calcHU(size: Size, t2: Double): Mat {
        val hu = Mat(size, CV_32FC1)
        val row = hu.rows()
        val col = hu.cols()
        val cx = row / 2
        val cy = row / 2
        
        for (i in 0 until row) {
            for (j in 0 until col) {
                val value = 1 / (1 + Math.pow(
                    Math.sqrt(Math.pow((cx - i).toDouble(), 2.0) + Math.pow((cy - j).toDouble(), 2.0)), 
                    -t2
                ))
                hu.put(i, j, value)
            }
        }
        
        val homo = mutableListOf<Mat>().apply {
            add(hu.clone())
            add(Mat(hu.size(), CV_32FC1, Scalar(0.0)))
        }
        
        val hu2c = Mat(size, CV_32FC2)
        merge(homo, hu2c)
        
        return hu2c
    }
    
    fun iftCenter(src: Mat): Mat {
        val dst = Mat(src.size(), CV_32F, Scalar(0.0))
        val dx = src.rows() / 2
        val dy = src.cols() / 2
        val data = FloatArray(dy)
        
        if (src.rows() % 2 == 0) {
            if (src.cols() % 2 == 0) {
                for (i in 0 until dx) {
                    src.get(i, 0, data)
                    dst.put(dx + i, dy, data)
                }
                for (i in 0 until dx) {
                    src.get(i, dy, data)
                    dst.put(dx + i, 0, data)
                }
                for (i in 0 until dx) {
                    src.get(dx + i, dy, data)
                    dst.put(i, 0, data)
                }
                for (i in 0 until dx) {
                    src.get(dx + i, 0, data)
                    dst.put(i, dy, data)
                }
            } else {
                println("copy failed")
            }
        }
        
        return dst
    }
    
    fun homoMethod(im: ByteArray, r: Int, c: Int): Mat {
        val t = 1
        val t2 = (t - 10) / 110.0
        
        var image = Mat(r, c, CV_8UC2).apply {
            put(0, 0, im)
        }
        
        cvtColor(image, image, COLOR_YUV2GRAY_YUYV)
        normalize(image, image, 0.0, 255.0, NORM_MINMAX)
        image.convertTo(image, CV_8UC1)
        
        val clahe = Imgproc.createCLAHE().apply {
            clipLimit = 1.0
            tilesGridSize = Size(3.0, 3.0)
        }
        clahe.apply(image, image)
        
        val imagepadd = Mat()
        val row = image.rows()
        val col = image.cols()
        val m = getOptimalDFTSize(row)
        val n = getOptimalDFTSize(col)
        
        image.convertTo(imagepadd, CV_32FC1)
        add(imagepadd, Scalar(1.0), imagepadd)
        log(imagepadd, imagepadd)
        copyMakeBorder(imagepadd, imagepadd, 0, m - row, 0, n - col, BORDER_CONSTANT, Scalar(0.0))
        
        val centeredImage = iftCenter(imagepadd)
        val tmpMerge = mutableListOf<Mat>().apply {
            add(centeredImage.clone())
            add(Mat(centeredImage.size(), CV_32FC1, Scalar(0.0)))
        }
        
        val mergedImage = Mat()
        merge(tmpMerge, mergedImage)
        dft(mergedImage, mergedImage)
        
        val imagePadd2c = Mat(mergedImage.size(), CV_32FC2)
        val hu2c = calcHU(mergedImage.size(), t2)
        
        mulSpectrums(mergedImage, hu2c, imagePadd2c, 0)
        idft(imagePadd2c, imagePadd2c, DFT_SCALE)
        println(imagePadd2c.channels())
        
        exp(imagePadd2c, imagePadd2c)
        subtract(imagePadd2c, Scalar(1.0), imagePadd2c)
        
        val imagePaddS = mutableListOf<Mat>()
        split(imagePadd2c, imagePaddS)
        
        val reinforceSrc = Mat()
        magnitude(imagePaddS[0], imagePaddS[1], reinforceSrc)
        
        val temp = Mat()
        normalize(reinforceSrc, temp, 0.0, 255.0, NORM_MINMAX)
        val centeredTemp = iftCenter(temp)
        
        val result = Mat()
        centeredTemp.convertTo(result, CV_8UC1)
        
        return result
    }
}