package com.energy.view.tempcanvas

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.Log
import com.energy.utils.ScreenUtils
import com.topdon.lib.ui.R
import java.util.*
import kotlin.math.*

class LineDraw(context: Context) : BaseDraw(context) {
    
    companion object {
        private const val TAG = "BaseTemperatureView LineDraw"
        private const val MAX_LINE_COUNT = 3
        private const val STROKE_WIDTH = 8
        private const val TEXT_SIZE = 14
        private const val TOUCH_TOLERANCE = 48
        
        const val OPERATE_STATUS_LINE_IN_TOUCH_START = 0
        const val OPERATE_STATUS_LINE_IN_TOUCH_CENTER = 1
        const val OPERATE_STATUS_LINE_IN_TOUCH_END = 2
        const val OPERATE_STATUS_LINE_ADD = 3
        const val OPERATE_STATUS_LINE_REMOVE = 4
    }
    
    private val mLineList = LinkedList<LineView>()
    private val mLinePaint: Paint
    private val lineStrokeWidth: Int
    private val mBgPaint: Paint
    private var mFontMetrics: Paint.FontMetrics? = null
    private val mTextPaint: Paint
    private val mBgStrokeColor = Color.parseColor("#99000000")
    private val mBgColor = Color.parseColor("#CC1A1A1A")
    private var mTempLine: LineView? = null
    private var mOperateStatus = -1
    
    init {
        lineStrokeWidth = ScreenUtils.dp2px(1)
        
        mLinePaint = Paint().apply {
            color = Color.WHITE
            strokeWidth = lineStrokeWidth.toFloat()
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        
        mTextPaint = Paint().apply {
            strokeWidth = ScreenUtils.dp2px(STROKE_WIDTH).toFloat()
            textSize = ScreenUtils.sp2px(TEXT_SIZE).toFloat()
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        
        mBgPaint = TextPaint().apply {
            strokeWidth = ScreenUtils.dp2px(1).toFloat()
        }
    }
    
    fun getOperateStatus(): Int = mOperateStatus
    
    fun setOperateStatus(operateStatus: Int) {
        mOperateStatus = operateStatus
        Log.d(TAG, "setOperateStatus = $operateStatus")
    }
    
    fun addLine(startX: Int, startY: Int, endX: Int, endY: Int) {
        if (abs(endX - startX) > TOUCH_TOLERANCE || abs(endY - startY) > TOUCH_TOLERANCE) {
            val lineView = LineView(mContext, startX, startY, endX, endY)
            val size = mLineList.size
            
            if (mLineList.size < MAX_LINE_COUNT) {
                val newLabel = "L${size + 1}"
                Log.d(TAG, "addLine newLabel : $newLabel")
                
                val hasSame = mLineList.any { it.label == newLabel }
                
                if (hasSame) {
                    mLineList.add(lineView)
                    mLineList.forEachIndexed { index, line -> line.label = "L${index + 1}" }
                } else {
                    lineView.label = newLabel
                    mLineList.add(lineView)
                }
                
                mTouchIndex = size
            } else {
                Log.d(TAG, "line remove and add")
                mLineList.removeFirst()
                mLineList.add(lineView)
                mLineList.forEachIndexed { index, line -> line.label = "L${index + 1}" }
                mTouchIndex = MAX_LINE_COUNT - 1
            }
        }
    }
    
    fun removeLine(index: Int) {
        if (mLineList.size > index) {
            mLineList.removeAt(index)
        }
    }
    
    fun removeLine() {
        mLineList.clear()
    }
    
    override fun onDraw(canvas: Canvas, isScroll: Boolean) {
        for (lineView in mLineList) {
            drawLabel(canvas, lineView)
            canvas.drawLine(
                lineView.mStartMovingLineX.toFloat(), 
                lineView.mStartMovingLineY.toFloat(), 
                lineView.mEndMovingLineX.toFloat(), 
                lineView.mEndMovingLineY.toFloat(), 
                mLinePaint
            )
            
            if (!isScroll) {
                lineView.highTempPoint?.let { point ->
                    canvas.drawBitmap(lineView.mHighPointBitmap, point.x.toFloat(), point.y.toFloat(), null)
                }
                lineView.lowTempPoint?.let { point ->
                    canvas.drawBitmap(lineView.mLowPointBitmap, point.x.toFloat(), point.y.toFloat(), null)
                }
            }
        }
    }
    
    fun onTempDraw(canvas: Canvas, startX: Int, startY: Int, endX: Int, endY: Int) {
        if (mTempLine == null) {
            mTempLine = LineView(mContext, startX, startY, endX, endY).apply {
                label = "L"
            }
        } else {
            mTempLine?.changeLocation(startX, startY, endX, endY)
        }
        
        mTempLine?.let { tempLine ->
            drawLabel(canvas, tempLine)
            canvas.drawLine(
                tempLine.mStartMovingLineX.toFloat(), 
                tempLine.mStartMovingLineY.toFloat(), 
                tempLine.mEndMovingLineX.toFloat(), 
                tempLine.mEndMovingLineY.toFloat(), 
                mLinePaint
            )
        }
    }
    
    private fun drawLabel(canvas: Canvas, lineView: LineView) {
        canvas.save()
        canvas.rotate(mScreenDegree.toFloat(), 
                     lineView.mStartMovingLineX + (lineView.mEndMovingLineX - lineView.mStartMovingLineX) / 2f,
                     lineView.mStartMovingLineY + (lineView.mEndMovingLineY - lineView.mStartMovingLineY) / 2f)
        
        val tempRectF = RectF().apply {
            val centerY = lineView.mStartMovingLineY + (lineView.mEndMovingLineY - lineView.mStartMovingLineY) / 2f
            val centerX = lineView.mStartMovingLineX + (lineView.mEndMovingLineX - lineView.mStartMovingLineX) / 2f
            top = centerY
            bottom = centerY
            left = centerX
            right = centerX
        }
        
        drawCustomTextBg(canvas, lineView.label, tempRectF)
        canvas.restore()
    }
    
    private fun drawCustomTextBg(canvas: Canvas, text: String, rectF: RectF): RectF {
        val rectWidth = (mTextPaint.measureText(text) * 2).toInt()
        var left = rectF.left - rectWidth / 2
        var right = rectF.right + rectWidth / 2
        var top = rectF.top
        var bottom = rectF.bottom
        
        if (left < 0) {
            left = 0f
            right = rectWidth.toFloat()
        }
        
        if (right > mViewWidth) {
            left = (mViewWidth - rectWidth).toFloat()
            right = mViewWidth.toFloat()
        }
        
        if (top < 0) {
            top = 0f
            bottom = 0f
        }
        
        rectF.apply {
            this.left = left
            this.right = right
            this.top = top
            this.bottom = bottom
        }
        
        mBgPaint.apply {
            style = Paint.Style.FILL
            color = mBgStrokeColor
        }
        canvas.drawRect(rectF, mBgPaint)
        
        mBgPaint.apply {
            style = Paint.Style.STROKE
            color = mBgColor
        }
        canvas.drawRect(rectF, mBgPaint)
        
        mFontMetrics = mTextPaint.fontMetrics
        val topMetrics = mFontMetrics?.top ?: 0f
        val bottomMetrics = mFontMetrics?.bottom ?: 0f
        val baseLineY = (rectF.centerY() - topMetrics / 2 - bottomMetrics / 2).toInt()
        
        canvas.drawText(text, rectF.centerX(), baseLineY.toFloat(), mTextPaint)
        return rectF
    }
    
    fun changeTouchLineOperateStatus(startX: Float, startY: Float) {
        if (mTouchIndex < 0 || mTouchIndex >= mLineList.size) return
        
        val lineView = mLineList[mTouchIndex]
        
        when {
            startX > lineView.mStartMovingLineX - TOUCH_TOLERANCE && startX < lineView.mStartMovingLineX + TOUCH_TOLERANCE &&
            startY > lineView.mStartMovingLineY - TOUCH_TOLERANCE && startY < lineView.mStartMovingLineY + TOUCH_TOLERANCE ->
                setOperateStatus(OPERATE_STATUS_LINE_IN_TOUCH_START)
                
            startX > lineView.mEndMovingLineX - TOUCH_TOLERANCE && startX < lineView.mEndMovingLineX + TOUCH_TOLERANCE &&
            startY > lineView.mEndMovingLineY - TOUCH_TOLERANCE && startY < lineView.mEndMovingLineY + TOUCH_TOLERANCE ->
                setOperateStatus(OPERATE_STATUS_LINE_IN_TOUCH_END)
                
            else -> setOperateStatus(OPERATE_STATUS_LINE_IN_TOUCH_CENTER)
        }
    }
    
    fun changeTouchLineLocationByIndex(moveX: Float, moveY: Float) {
        if (mTouchIndex < 0 || mTouchIndex >= mLineList.size) return
        
        Log.d(TAG, "mOperateStatus : $mOperateStatus")
        val lineView = mLineList[mTouchIndex]
        
        when (mOperateStatus) {
            OPERATE_STATUS_LINE_IN_TOUCH_START -> {
                val startMovingLineX = (lineView.mStartPoint.x + moveX).toInt().coerceIn(MIN_SIZE_PIX_COUNT, mViewWidth - MIN_SIZE_PIX_COUNT)
                val startMovingLineY = (lineView.mStartPoint.y + moveY).toInt().coerceIn(MIN_SIZE_PIX_COUNT, mViewHeight - MIN_SIZE_PIX_COUNT)
                val endMovingLineX = lineView.mEndPoint.x.coerceIn(MIN_SIZE_PIX_COUNT, mViewWidth - MIN_SIZE_PIX_COUNT)
                val endMovingLineY = lineView.mEndPoint.y.coerceIn(MIN_SIZE_PIX_COUNT, mViewHeight - MIN_SIZE_PIX_COUNT)
                
                mLineList[mTouchIndex].changeLocation(startMovingLineX, startMovingLineY, endMovingLineX, endMovingLineY)
            }
            
            OPERATE_STATUS_LINE_IN_TOUCH_END -> {
                val startMovingLineX = lineView.mStartPoint.x.coerceIn(MIN_SIZE_PIX_COUNT, mViewWidth - MIN_SIZE_PIX_COUNT)
                val startMovingLineY = lineView.mStartPoint.y.coerceIn(MIN_SIZE_PIX_COUNT, mViewHeight - MIN_SIZE_PIX_COUNT)
                val endMovingLineX = (lineView.mEndPoint.x + moveX).toInt().coerceIn(MIN_SIZE_PIX_COUNT, mViewWidth - MIN_SIZE_PIX_COUNT)
                val endMovingLineY = (lineView.mEndPoint.y + moveY).toInt().coerceIn(MIN_SIZE_PIX_COUNT, mViewHeight - MIN_SIZE_PIX_COUNT)
                
                mLineList[mTouchIndex].changeLocation(startMovingLineX, startMovingLineY, endMovingLineX, endMovingLineY)
            }
            
            OPERATE_STATUS_LINE_IN_TOUCH_CENTER -> {
                val startMovingLineX = (lineView.mStartPoint.x + moveX).toInt().coerceIn(MIN_SIZE_PIX_COUNT, mViewWidth - MIN_SIZE_PIX_COUNT)
                val startMovingLineY = (lineView.mStartPoint.y + moveY).toInt().coerceIn(MIN_SIZE_PIX_COUNT, mViewHeight - MIN_SIZE_PIX_COUNT)
                val endMovingLineX = (lineView.mEndPoint.x + moveX).toInt().coerceIn(MIN_SIZE_PIX_COUNT, mViewWidth - MIN_SIZE_PIX_COUNT)
                val endMovingLineY = (lineView.mEndPoint.y + moveY).toInt().coerceIn(MIN_SIZE_PIX_COUNT, mViewHeight - MIN_SIZE_PIX_COUNT)
                
                mLineList[mTouchIndex].changeLocation(startMovingLineX, startMovingLineY, endMovingLineX, endMovingLineY)
            }
        }
    }
    
    fun changeTouchPointLocation() {
        if (mTouchIndex < 0 || mTouchIndex >= mLineList.size) return
        mLineList[mTouchIndex].changePointLocation()
    }
    
    fun checkTouchLineInclude(x: Int, y: Int): Int {
        mTouchIndex = -1
        
        mLineList.forEachIndexed { index, lineView ->
            val tempDistance = ((lineView.mEndMovingLineY - lineView.mStartMovingLineY) * x - 
                               (lineView.mEndMovingLineX - lineView.mStartMovingLineX) * y + 
                               lineView.mEndMovingLineX * lineView.mStartMovingLineY - 
                               lineView.mStartMovingLineX * lineView.mEndMovingLineY)
            
            val distance = (tempDistance / sqrt(
                (lineView.mEndMovingLineY - lineView.mStartMovingLineY).toDouble().pow(2.0) + 
                (lineView.mEndMovingLineX - lineView.mStartMovingLineX).toDouble().pow(2.0)
            )).toInt()
            
            if (abs(distance) < TOUCH_TOLERANCE && 
                x > min(lineView.mStartMovingLineX, lineView.mEndMovingLineX) - TOUCH_TOLERANCE && 
                x < max(lineView.mStartMovingLineX, lineView.mEndMovingLineX) + TOUCH_TOLERANCE) {
                mTouchIndex = index
                Log.d(TAG, "checkTouchLineInclude true mTouchIndex = $mTouchIndex")
                return index
            }
        }
        return mTouchIndex
    }
    
    val lineViewList: LinkedList<LineView> get() = mLineList
    
    class LineView(context: Context, startX: Int, startY: Int, endX: Int, endY: Int) : BaseView() {
        
        companion object {
            private const val TOUCH_EXTRA = 10f
        }
        
        val mStartPoint = Point(startX, startY)
        val mEndPoint = Point(endX, endY)
        val mHighPointBitmap: Bitmap
        val mLowPointBitmap: Bitmap
        var highTempPoint: Point? = null
        var lowTempPoint: Point? = null
        
        var mStartMovingLineX = startX
        var mStartMovingLineY = startY
        var mEndMovingLineX = endX
        var mEndMovingLineY = endY
        
        init {
            mPointSize = ScreenUtils.dp2px(20f)
            mId = UUID.randomUUID().toString()
            mHighPointBitmap = getCustomSizeImg(BitmapFactory.decodeResource(context.resources, R.mipmap.icon_point_red), mPointSize, mPointSize)
            mLowPointBitmap = getCustomSizeImg(BitmapFactory.decodeResource(context.resources, R.mipmap.icon_point_green), mPointSize, mPointSize)
        }
        
        fun changeLocation(startX: Int, startY: Int, endX: Int, endY: Int) {
            mStartMovingLineX = startX
            mStartMovingLineY = startY
            mEndMovingLineX = endX
            mEndMovingLineY = endY
        }
        
        fun changePointLocation() {
            mStartPoint.apply {
                x = mStartMovingLineX
                y = mStartMovingLineY
            }
            mEndPoint.apply {
                x = mEndMovingLineX
                y = mEndMovingLineY
            }
        }
    }
}