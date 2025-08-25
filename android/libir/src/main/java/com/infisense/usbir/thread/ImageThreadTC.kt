package com.infisense.usbir.thread

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.annotation.Nullable
import com.elvishew.xlog.XLog
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.SynchronizedBitmap
import com.example.open3d.JNITool
import com.example.suplib.wrapper.SupHelp
import com.infisense.usbir.bean.ColorRGB
import com.infisense.usbir.utils.IRImageHelp
import com.infisense.usbir.utils.OpencvTools
import com.infisense.usbir.utils.PseudocodeUtils
import com.topdon.lib.core.bean.AlarmBean
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.util.*

class ImageThreadTC(
    private val mContext: Context,
    private val imageWidth: Int,
    private val imageHeight: Int
) : Thread() {
    
    companion object {
        const val TYPE_AI_C = -1
        const val TYPE_AI_D = 0
        const val TYPE_AI_H = 1
        const val TYPE_AI_L = 2
        const val MULTIPLE = 2
    }
    
    private val tag = "ImageThread"
    private var bitmap: Bitmap? = null
    private var syncimage: SynchronizedBitmap? = null
    private var imageSrc: ByteArray? = null
    private var temperatureSrc: ByteArray? = null
    private var rotate = false
    private var rotateInt = 0
    
    private var dataFlowMode = CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT
    private val imageYUV422 = ByteArray(imageWidth * imageHeight * 2)
    private val imageARGB = ByteArray(imageWidth * imageHeight * 4)
    private var imageDst = ByteArray(imageWidth * imageHeight * 4)
    private val imgTmp = ByteArray(imageWidth * imageHeight * 4)
    val imageTemp = ByteArray(imageWidth * imageHeight * 4)
    
    private val imageY8 = ByteArray(imageWidth * imageHeight)
    private var max = Float.MAX_VALUE
    private var min = Float.MIN_VALUE
    private var maxColor = 0
    private var minColor = 0
    
    private var pseudocolorMode = 3
    private var alarmBean: AlarmBean? = null
    
    private var firstFrame: ByteArray? = null
    private var firstTemp: ByteArray? = null
    private var typeAi = TYPE_AI_C
    private val irImageHelp = IRImageHelp()
    
    @Volatile
    private var isOpenAmplify = false
    private val amplifyRotateArray = ByteArray(imageWidth * MULTIPLE * imageHeight * MULTIPLE * 4)
    
    init {
        Log.i(tag, "ImageThread create->imageWidth = $imageWidth imageHeight = $imageHeight")
    }
    
    fun setOpenAmplify(openAmplify: Boolean) {
        isOpenAmplify = openAmplify
    }
    
    fun getTypeAi(): Int = typeAi
    
    fun setTypeAi(typeAi: Int) {
        this.typeAi = typeAi
    }
    
    fun getAlarmBean(): AlarmBean? = alarmBean
    
    fun setAlarmBean(alarmBean: AlarmBean?) {
        this.alarmBean = alarmBean
    }
    
    fun setSyncImage(syncimage: SynchronizedBitmap) {
        this.syncimage = syncimage
    }
    
    fun setImageSrc(imageSrc: ByteArray) {
        this.imageSrc = imageSrc
    }
    
    fun getPseudocolorMode(): Int = pseudocolorMode
    
    fun setPseudocolorMode(pseudocolorMode: Int) {
        this.pseudocolorMode = pseudocolorMode
    }
    
    fun setTemperatureSrc(temperatureSrc: ByteArray) {
        this.temperatureSrc = temperatureSrc
    }
    
    fun setRotate(rotate: Boolean) {
        this.rotate = rotate
    }
    
    fun setRotate(rotateInt: Int) {
        this.rotateInt = rotateInt
    }
    
    fun setLimit(max: Float, min: Float) {
        this.max = max
        this.min = min
    }
    
    fun setLimit(max: Float, min: Float, maxColor: Int, minColor: Int) {
        this.max = max
        this.min = min
        this.maxColor = maxColor
        this.minColor = minColor
    }
    
    fun setDataFlowMode(dataFlowMode: CommonParams.DataFlowMode) {
        this.dataFlowMode = dataFlowMode
    }
    
    fun setBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
    }
    
    override fun run() {
        while (!isInterrupted) {
            val sync = syncimage ?: continue
            val srcImage = imageSrc ?: continue
            val tempSrc = temperatureSrc ?: continue
            
            synchronized(sync.dataLock) {
                if (sync.start) {
                    // Convert YUYV to ARGB with pseudocolor
                    if (irImageHelp.colorList != null) {
                        LibIRProcess.convertYuyvMapToARGBPseudocolor(
                            srcImage, imageHeight * imageWidth,
                            CommonParams.PseudoColorType.PSEUDO_1, imageARGB
                        )
                    } else {
                        LibIRProcess.convertYuyvMapToARGBPseudocolor(
                            srcImage, imageHeight * imageWidth,
                            PseudocodeUtils.changePseudocodeModeByOld(pseudocolorMode), imageARGB
                        )
                    }
                    
                    // Apply rotation
                    imageDst = when (rotateInt) {
                        270 -> {
                            val imageRes = LibIRProcess.ImageRes_t().apply {
                                height = imageWidth.toChar()
                                width = imageHeight.toChar()
                            }
                            val rotatedDst = ByteArray(imageARGB.size)
                            LibIRProcess.rotateRight90(
                                imageARGB, imageRes,
                                CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888, rotatedDst
                            )
                            rotatedDst
                        }
                        90 -> {
                            val imageRes = LibIRProcess.ImageRes_t().apply {
                                height = imageWidth.toChar()
                                width = imageHeight.toChar()
                            }
                            val rotatedDst = ByteArray(imageARGB.size)
                            LibIRProcess.rotateLeft90(
                                imageARGB, imageRes,
                                CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888, rotatedDst
                            )
                            rotatedDst
                        }
                        180 -> {
                            val imageRes = LibIRProcess.ImageRes_t().apply {
                                width = imageHeight.toChar()
                                height = imageWidth.toChar()
                            }
                            val rotatedDst = ByteArray(imageARGB.size)
                            LibIRProcess.rotate180(
                                imageARGB, imageRes,
                                CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888, rotatedDst
                            )
                            rotatedDst
                        }
                        else -> imageARGB
                    }
                    
                    irImageHelp.customPseudoColor(imageDst, tempSrc, imageWidth, imageHeight)
                    irImageHelp.setPseudoColorMaxMin(imageDst, tempSrc, max, min, imageWidth, imageHeight)
                }
                
                val currentWidth = if (rotateInt == 270 || rotateInt == 90) imageWidth else imageHeight
                val currentHeight = if (rotateInt == 270 || rotateInt == 90) imageHeight else imageWidth
                
                imageDst = irImageHelp.contourDetection(alarmBean, imageDst, tempSrc, currentWidth, currentHeight)
                
                // Apply AI processing based on type
                when (typeAi) {
                    TYPE_AI_H -> {
                        val dataArray = JNITool.maxTempL(imageDst, tempSrc, currentWidth, currentHeight, -1)
                        val diffMat = Mat(192, 256, CvType.CV_8UC3).apply {
                            put(0, 0, dataArray)
                        }
                        Imgproc.cvtColor(diffMat, diffMat, Imgproc.COLOR_BGR2RGBA)
                        val grayData = ByteArray(diffMat.cols() * diffMat.rows() * 4)
                        diffMat.get(0, 0, grayData)
                        imageDst = grayData
                    }
                    TYPE_AI_L -> {
                        val dataArray = JNITool.lowTemTrack(imageDst, tempSrc, currentWidth, currentHeight, -1)
                        val diffMat = Mat(192, 256, CvType.CV_8UC3).apply {
                            put(0, 0, dataArray)
                        }
                        Imgproc.cvtColor(diffMat, diffMat, Imgproc.COLOR_BGR2RGBA)
                        val grayData = ByteArray(diffMat.cols() * diffMat.rows() * 4)
                        diffMat.get(0, 0, grayData)
                        imageDst = grayData
                    }
                    TYPE_AI_D -> {
                        if (firstFrame == null || firstTemp == null) {
                            firstFrame = imageDst.copyOf()
                            firstTemp = tempSrc.copyOf()
                        } else {
                            if (OpencvTools.getStatus(firstFrame, imageDst)) {
                                try {
                                    val dataArray = JNITool.diff2firstFrameByTempWH(
                                        currentWidth, currentHeight,
                                        firstTemp, tempSrc, imageDst
                                    )
                                    val diffMat = Mat(192, 256, CvType.CV_8UC4).apply {
                                        put(0, 0, dataArray)
                                    }
                                    Imgproc.cvtColor(diffMat, diffMat, Imgproc.COLOR_RGB2RGBA)
                                    val grayData = ByteArray(diffMat.cols() * diffMat.rows() * 4)
                                    diffMat.get(0, 0, grayData)
                                    imageDst = grayData
                                } catch (e: Throwable) {
                                    Log.e("静态闯入异常：", e.message.orEmpty())
                                }
                            } else {
                                firstFrame = imageDst.copyOf()
                                firstTemp = tempSrc.copyOf()
                            }
                        }
                    }
                }
                
                // Apply amplification if enabled
                if (isOpenAmplify && SupHelp.getInstance().an4K != null) {
                    OpencvTools.supImage(imageDst, currentHeight, currentWidth, amplifyRotateArray)
                }
            }
            
            synchronized(sync.viewLock) {
                if (!sync.valid) {
                    try {
                        val targetArray = if (isOpenAmplify && amplifyRotateArray.isNotEmpty()) {
                            amplifyRotateArray
                        } else {
                            imageDst
                        }
                        bitmap?.copyPixelsFromBuffer(ByteBuffer.wrap(targetArray))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    sync.valid = true
                    sync.viewLock.notify()
                }
            }
            
            try {
                SystemClock.sleep(20)
            } catch (e: Exception) {
                XLog.e("Image Thread刷新异常: ${e.message}")
            }
        }
        Log.i(tag, "ImageThread exit")
    }
    
    fun getBaseBitmap(rotateInt: Int): Bitmap? {
        val baseBitmap = if (rotateInt == 0 || rotateInt == 180) {
            Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(192, 256, Bitmap.Config.ARGB_8888)
        }
        baseBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(imageDst))
        return baseBitmap
    }
    
    private fun getColorRGBByMap(map: LinkedHashMap<Int, ColorRGB>, key: Int): ColorRGB? {
        return map[key]
    }
    
    fun setColorList(
        @Nullable colorList: IntArray?,
        @Nullable places: FloatArray?,
        isUseGray: Boolean,
        customMaxTemp: Float,
        customMinTemp: Float
    ) {
        irImageHelp.setColorList(colorList, places, isUseGray, customMaxTemp, customMinTemp)
    }
}