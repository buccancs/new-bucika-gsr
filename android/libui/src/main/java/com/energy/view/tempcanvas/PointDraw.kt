package com.energy.view.tempcanvas

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.Log
import com.energy.utils.ScreenUtils
import com.topdon.lib.ui.R
import java.util.*

class PointDraw(context: Context) : BaseDraw(context) {

    companion object {
        private const val TAG = "BaseTemperatureView PointDraw"
        const val OPERATE_STATUS_POINT_IN_TOUCH = 0
        const val OPERATE_STATUS_POINT_ADD = 1
        const val OPERATE_STATUS_POINT_REMOVE = 2
        private const val MAX_POINT_COUNT = 3
        private const val STROKE_WIDTH = 8
        private const val TEXT_SIZE = 14
        private const val TOUCH_EXTRA = 20f
    }

    private val textPointMargin = ScreenUtils.dp2px(4)
    private val labelPointMargin = ScreenUtils.dp2px(24)
    private val pointList = LinkedList<PointView>()
    private var tempPoint: PointView? = null
    
    private val textPaint = Paint().apply {
        strokeWidth = ScreenUtils.dp2px(STROKE_WIDTH).toFloat()
        textSize = ScreenUtils.sp2px(TEXT_SIZE).toFloat()
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    
    private val bgPaint = TextPaint().apply {
        strokeWidth = ScreenUtils.dp2px(1).toFloat()
    }
    
    private var fontMetrics: Paint.FontMetrics? = null
    private val bgStrokeColor = Color.parseColor("#99000000")
    private val bgColor = Color.parseColor("#CC1A1A1A")
    private var operateStatus = -1

    fun getOperateStatus(): Int = operateStatus

    fun setOperateStatus(operateStatus: Int) {
        this.operateStatus = operateStatus
        Log.d(TAG, "setOperateStatus = $operateStatus")
    }

    fun addPoint(mode: Int, centerX: Float, centerY: Float) {
        val pointView = PointView(mContext, mode, centerX, centerY)
        val size = pointList.size
        if (pointList.size < MAX_POINT_COUNT) {
            Log.d(TAG, "addPoint")

            val newLabel = "P${size + 1}"
            val hasSame = pointList.any { it.label == newLabel }

            if (hasSame) {
                pointList.add(pointView)
                pointList.forEachIndexed { i, point ->
                    point.label = "P${i + 1}"
                }
            } else {
                pointView.label = newLabel
                pointList.add(pointView)
            }
            mTouchIndex = size
        } else {
            Log.d(TAG, "point remove and add")
            pointList.removeFirst()
            pointList.add(pointView)
            pointList.forEachIndexed { i, point ->
                point.label = "P${i + 1}"
            }
            mTouchIndex = MAX_POINT_COUNT - 1
        }
    }

    fun removePoint(index: Int) {
        if (pointList.size > index) {
            pointList.removeAt(index)
        }
    }

    fun removePoint() {
        pointList.clear()
    }

    override fun onDraw(canvas: Canvas, isScroll: Boolean) {
        pointList.forEach { pointView ->
            drawLabel(canvas, pointView)
            canvas.drawBitmap(
                pointView.pointBitmap,
                pointView.centerX - pointView.mPointSize / 2,
                pointView.centerY - pointView.mPointSize / 2,
                null
            )
            if (!isScroll && pointView.tempPoint != null) {
                canvas.drawBitmap(
                    pointView.pointBitmap,
                    pointView.tempPoint!!.x.toFloat(),
                    pointView.tempPoint!!.y.toFloat(),
                    null
                )
            }
        }
    }

    fun onTempDraw(canvas: Canvas, mode: Int, centerX: Float, centerY: Float) {
        if (tempPoint == null) {
            tempPoint = PointView(mContext, mode, centerX, centerY).apply {
                label = "P"
            }
        } else {
            tempPoint?.changeLocation(centerX, centerY)
        }
        tempPoint?.let { point ->
            drawLabel(canvas, point)
            canvas.drawBitmap(
                point.pointBitmap,
                point.centerX - point.mPointSize / 2,
                point.centerY - point.mPointSize / 2,
                null
            )
        }
    }

    private fun drawLabel(canvas: Canvas, pointView: PointView) {
        canvas.save()
        canvas.rotate(
            mScreenDegree.toFloat(),
            pointView.centerX + textPointMargin,
            pointView.centerY - textPointMargin
        )
        
        val tempRectF = RectF()
        var top = pointView.centerY - textPointMargin - labelPointMargin - pointView.mPointSize / 2
        var bottom = pointView.centerY - textPointMargin - pointView.mPointSize / 2
        
        if (top < 0) {
            top = pointView.centerY + textPointMargin + pointView.mPointSize / 2
            bottom = top + labelPointMargin
        }
        
        tempRectF.apply {
            this.top = top
            this.bottom = bottom
            left = pointView.centerX
            right = pointView.centerX
        }

        drawCustomTextBg(canvas, pointView.label, tempRectF)
        canvas.restore()
    }

    private fun drawCustomTextBg(canvas: Canvas, text: String, rectF: RectF): RectF {
        val rectWidth = textPaint.measureText(text).toInt() + textPointMargin * 2
        var left = rectF.left - rectWidth / 2
        var right = rectF.right + rectWidth / 2
        val top = rectF.top
        val bottom = rectF.bottom
        
        if (left < 0) {
            left = 0f
            right = rectWidth.toFloat()
        }

        if (right > mViewWidth) {
            left = mViewWidth - rectWidth.toFloat()
            right = mViewWidth.toFloat()
        }
        
        rectF.apply {
            this.left = left
            this.right = right
        }
        
        bgPaint.apply {
            style = Paint.Style.FILL
            color = bgStrokeColor
        }
        canvas.drawRect(rectF, bgPaint)
        
        bgPaint.apply {
            style = Paint.Style.STROKE
            color = bgColor
        }
        canvas.drawRect(rectF, bgPaint)

        fontMetrics = textPaint.fontMetrics
        val topMetrics = fontMetrics!!.top
        val bottomMetrics = fontMetrics!!.bottom
        val baseLineY = (rectF.centerY() - topMetrics / 2 - bottomMetrics / 2).toInt()
        canvas.drawText(text, rectF.centerX(), baseLineY.toFloat(), textPaint)
        return rectF
    }

    fun changeTouchPointLocationByIndex(touchIndex: Int, centerX: Float, centerY: Float) {
        if (touchIndex in 0 until pointList.size) {
            pointList[touchIndex].changeLocation(centerX, centerY)
        }
    }

    fun checkTouchPointInclude(rawX: Float, rawY: Float): Int {
        mTouchIndex = -1
        pointList.forEachIndexed { i, pointView ->
            if (pointView.inRect.contains(rawX.toInt(), rawY.toInt())) {
                mTouchIndex = i
                return i
            }
        }
        return mTouchIndex
    }

    class PointView(context: Context, private val mode: Int, centerX: Float, centerY: Float) : BaseView() {

        var centerX: Float = centerX
            private set
        var centerY: Float = centerY
            private set
        
        val inRect = Rect()
        val pointBitmap: Bitmap
        var tempPoint: Point? = null

        init {
            mId = UUID.randomUUID().toString()
            mPointSize = ScreenUtils.dp2px(20f)
            this.centerX = centerX
            this.centerY = centerY
            
            pointBitmap = when (mode) {
                1 -> getCustomSizeImg(BitmapFactory.decodeResource(context.resources, R.mipmap.icon_point_green), mPointSize, mPointSize)
                2 -> getCustomSizeImg(BitmapFactory.decodeResource(context.resources, R.mipmap.icon_point_blue), mPointSize, mPointSize)
                3 -> getCustomSizeImg(BitmapFactory.decodeResource(context.resources, R.mipmap.icon_point_red), mPointSize, mPointSize)
                else -> throw IllegalArgumentException("Invalid mode: $mode")
            }

            updateRect()
        }

        private fun updateRect() {
            inRect.apply {
                left = (centerX - mPointSize / 2 - TOUCH_EXTRA).toInt()
                right = (centerX + mPointSize / 2 + TOUCH_EXTRA).toInt()
                top = (centerY - mPointSize / 2 - TOUCH_EXTRA).toInt()
                bottom = (centerY + mPointSize / 2 + TOUCH_EXTRA).toInt()
            }
        }

        fun changeLocation(centerX: Float, centerY: Float) {
            this.centerX = centerX
            this.centerY = centerY
            updateRect()
        }
    }

    fun getPointViewList(): LinkedList<PointView> = pointList
}