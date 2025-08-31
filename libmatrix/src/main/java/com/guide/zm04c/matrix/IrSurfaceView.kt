package com.guide.zm04c.matrix

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.blankj.utilcode.util.ScreenUtils

class IrSurfaceView : SurfaceView, SurfaceHolder.Callback {

    private var mHolder: SurfaceHolder? = null // 用于控制SurfaceView
    private var mCanvas: Canvas? = null // 声明一张画布
    private val p: Paint by lazy { Paint() } // 声明一支画笔
    private val mMatrix: Matrix by lazy { Matrix() }
    private var openLut = false

    private val mBeforeRotateMatrixValues = FloatArray(9)
    private val mScaleMatrixValues = FloatArray(9)
    private val mRotateMatrixValues = FloatArray(9)

    @Volatile
    private var isPrepare = false

    @Volatile
    private var isLockImage = false

    private var callback: IfrCamOpenOverCallback? = null

    private var mCtx: Context? = null;

    constructor(context: Context) : super(context) {
        mCtx = context
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mCtx = context
        init()
    }

    private fun init() {
        mHolder = holder // 获得SurfaceHolder对象
        mHolder?.addCallback(this) // 为SurfaceView添加状态监听
        mHolder?.setFormat(PixelFormat.TRANSPARENT)
        p.alpha = 0xff
        mMatrix.setScale(1.0f, 1.0f)
    }

    fun setIsLockImage(isLock: Boolean) {
        isLockImage = isLock
    }

//    fun setMatrix(scale: Float, x: Float, y: Float) {
//        mMatrix.reset()
//        mMatrix.setScale(scale, scale)
//        mMatrix.postTranslate(x, y)
//        mMatrix.getValues(mBeforeRotateMatrixValues)
//    }

    /**
     * @param rotate
     * 0
     * 90
     * 180
     * 270
     */
    fun setMatrix(rotate: Float, w: Float, h: Float) {
        mMatrix.reset()
        when (rotate) {
            90f -> {
                val sca = ScreenUtils.getScreenWidth() / h
                mMatrix.setRotate(rotate, 0f, 0f)
                mMatrix.postTranslate(h, 0f)
                mMatrix.postScale(sca, sca)
            }
            180f -> {
                val sca = ScreenUtils.getScreenWidth() / w
                mMatrix.setRotate(rotate, 0f, 0f)
                mMatrix.postTranslate(w, h)
                mMatrix.postScale(sca, sca)
            }
            270f -> {
                //矩阵转换
                val sca = ScreenUtils.getScreenWidth() / h
                mMatrix.setRotate(rotate, 0f, 0f)
                mMatrix.postTranslate(0f, w)
                mMatrix.postScale(sca, sca)
            }
            else -> {
                val sca = ScreenUtils.getScreenWidth() / w
                mMatrix.postScale(sca, sca)
            }
        }
    }

    /**
     * 自定义画图方法
     */
    fun doDraw(bitmap: Bitmap?, shutterFlag: Int) {

        synchronized(this) {
            if (isLockImage || !isPrepare || null == bitmap || shutterFlag == 1) {
                return@doDraw
            }

            mCanvas = mHolder?.lockCanvas() // 获得画布对象，开始对画布画图

            try {
                mCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
//                mCanvas?.drawBitmap(bitmap, mMatrix, p)
                if (openLut) {
                    mColorMatrixEnhance.setSaturation(saturation * 0.01f * 2.5f + 1f)//对比度
                    p.colorFilter = ColorMatrixColorFilter(mColorMatrixEnhance)//修改色彩矩阵
                } else {
                    p.colorFilter = ColorMatrixColorFilter(mColorMatrix)//恢复色彩矩阵
                }
                mCanvas?.drawBitmap(bitmap, mMatrix, p)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                val surface = mHolder!!.surface
                if (mCanvas != null && mHolder != null && surface != null && surface.isValid) {
                    try {
                        mHolder?.unlockCanvasAndPost(mCanvas) // 完成画画，把画布显示在屏幕上
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

        }
    }

    // 生成色彩矩阵
    private var mColorMatrix = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 01f, 0f
        )
    )

    // 生成色彩矩阵
    private var mColorMatrixLut = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1.5f, 0f, 0f, 25f,
            0.1f, 0.2f, 0.7f, 0f, 25f,
            0f, 0f, 0f, 01f, 0f
        )
    )
    private val n = 1f

    // 生成色彩矩阵
    private var mColorMatrixEnhance = ColorMatrix(
//        floatArrayOf(
//            n, 0f, 0f, 0f, 128 * (1 - n),
//            0f, n, 0f, 0f, 128 * (1 - n),
//            0f, 0f, n, 0f, 128 * (1 - n),
//            0f, 0f, 0f, 1f, 0f
//        )
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    private var saturation = 0 //对比度 0~100

    fun setOpenLut() {
//        openLut = !openLut
        openLut = true
    }

    /**
     * 设置对比度
     * @param saturation 0 ~ 100
     */
    fun setSaturationValue(saturation: Int) {
        this.saturation = saturation
    }

    fun getSaturationValue(): Int {
        return saturation
    }

    fun setAlpha(alpha: Int) {
        if (alpha in 0..255) {
            p?.alpha = alpha
        }
    }

    /**
     * 当SurfaceView创建的时候，调用此函数
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        isPrepare = true
        if (callback != null)
            callback!!.onSurfaceCreated()
        Logger.d(TAG, "holder onSurfaceCreated")
    }

    /**
     * 当SurfaceView的视图发生改变的时候，调用此函数
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Logger.d(TAG, "holder surfaceChanged")
    }

    /**
     * 当SurfaceView销毁的时候，调用此函数
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        synchronized(this) {
            isPrepare = false
            Logger.d(TAG, "holder destroyed")
        }
    }

    companion object {
        private val TAG = "IrSurfaceView"
    }

    interface IfrCamOpenOverCallback {
        fun onSurfaceCreated()
    }

}