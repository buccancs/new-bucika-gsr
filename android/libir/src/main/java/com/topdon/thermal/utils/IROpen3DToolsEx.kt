package com.topdon.thermal.utils

import android.util.Log
import com.example.opengl.render.IROpen3DTools
import org.opencv.core.Mat
import org.opencv.core.Core.NORM_MINMAX
import org.opencv.core.Core.normalize
import org.opencv.core.CvType.CV_8UC1
import org.opencv.core.CvType.CV_8UC2
import org.opencv.imgproc.Imgproc.COLOR_YUV2GRAY_YUYV
import org.opencv.imgproc.Imgproc.applyColorMap
import org.opencv.imgproc.Imgproc.cvtColor

class IROpen3DToolsEx : IROpen3DTools() {
    
    private lateinit var img: Mat
    
    override fun init(image_: ByteArray, type: Int) {
        val time = System.currentTimeMillis()
        rws = 192
        cls = 256
        
        if (!::gray_image.isInitialized || gray_image == null) {
            gray_image = Mat()
        }
        
        img = Mat(rws, cls, CV_8UC2)
        img.put(0, 0, image_)
        cvtColor(img, img, COLOR_YUV2GRAY_YUYV)
        normalize(img, img, 0.0, 255.0, NORM_MINMAX)
        img.convertTo(gray_image, CV_8UC1)
        image = Mat()
        applyColorMap(gray_image, image, 15)
        
        halfx = rws.toFloat() / 2
        halfy = cls.toFloat() / 2
    }
}
