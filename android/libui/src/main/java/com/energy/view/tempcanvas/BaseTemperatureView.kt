package com.energy.view.tempcanvas

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.NonNull
import com.topdon.lib.ui.R
import com.energy.utils.ScreenUtils
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min

abstract class BaseTemperatureView : SurfaceView, SurfaceHolder.Callback {
    
    companion object {
        private const val TAG = "BaseTemperatureView"
        private const val BORDER_PX = 8
    }

    private lateinit var mContext: Context
    private lateinit var mSurfaceHolder: SurfaceHolder
    private var mTempThread: TempThread? = null
    private var mDrawThread: DrawThread? = null
    private var mCanDraw = false
    private lateinit var mGestureDetector: GestureDetector

    protected lateinit var mPointDraw: PointDraw
    protected lateinit var mLineDraw: LineDraw
    protected lateinit var mRectDraw: RectDraw

    private var mDrawModel = DrawModel.NONE
    private val mCanvasLock = Any()

    private var mFirstX = 0f
    private var mFirstY = 0f
    private var mCurX = 0f
    private var mCurY = 0f
    private var mRawX = 0f
    private var mRawY = 0f
    private var mDistanceX = 0f
    private var mDistanceY = 0f

    protected var mViewWidth = 0
    protected var mViewHeight = 0
    protected var mTempWidth = 0
    protected var mTempHeight = 0
    protected var xScale = 0f
    protected var yScale = 0f

    private val mTextWidth = 110
    private lateinit var mTextPaint: TextPaint

    constructor(context: Context) : this(context, null)
    
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, -1)
    
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView(context)
    }

    fun start() {
        mDrawThread = DrawThread().apply { start() }
    }

    fun stop() {
        mDrawThread?.apply {
            isRun = false
            interrupt()
        }
        mDrawThread = null
    }

    fun resume() {
        mDrawThread?.let { it.isRun = true }
    }

    fun pause() {
        mDrawThread?.let { it.isRun = false }
    }

    fun setDrawModel(drawModel: DrawModel) {
        this.mDrawModel = drawModel
    }

    fun clearCanvas() {
        pause()
        mPointDraw.removePoint()
        mLineDraw.removeLine()
        mRectDraw.removeRect()
        resume()
    }

    abstract fun getTempWidth(): Int
    abstract fun getTempHeight(): Int

    private fun initView(context: Context) {
        Log.d(TAG, "initView")
        mContext = context
        mSurfaceHolder = holder.apply {
            addCallback(this@BaseTemperatureView)
            setFormat(PixelFormat.TRANSPARENT)
        }

        isFocusableInTouchMode = true

        mTextPaint = TextPaint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = ScreenUtils.sp2px(14).toFloat()
        }

        mPointDraw = PointDraw(mContext)
        mLineDraw = LineDraw(mContext)
        mRectDraw = RectDraw(mContext)

        setZOrderOnTop(true)
        setZOrderMediaOverlay(true)

        mGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onContextClick(e: MotionEvent): Boolean {
                Log.d(TAG, "onContextClick")
                return super.onContextClick(e)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                Log.d(TAG, "onDoubleTap")
                return super.onDoubleTap(e)
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Log.d(TAG, "onDoubleTapEvent ACTION_DOWN")
                    }
                    MotionEvent.ACTION_UP -> {
                        Log.d(TAG, "onDoubleTapEvent ACTION_UP")
                        
                        mCurX = e.x.coerceIn(BORDER_PX.toFloat(), mViewWidth - BORDER_PX.toFloat())
                        mCurY = e.y.coerceIn(BORDER_PX.toFloat(), mViewHeight - BORDER_PX.toFloat())
                        mRawX = e.rawX
                        mRawY = e.rawY
                        
                        Log.d(TAG, "onDoubleTap mCurX : $mCurX")
                        Log.d(TAG, "onDoubleTap mCurY : $mCurY")

                        when (mDrawModel) {
                            DrawModel.DRAW_POINT -> {
                                if (mPointDraw.operateStatus == PointDraw.OPERATE_STATUS_POINT_IN_TOUCH) {
                                    mPointDraw.operateStatus = PointDraw.OPERATE_STATUS_POINT_REMOVE
                                }
                            }
                            DrawModel.DRAW_LINE -> {
                                val status = mLineDraw.operateStatus
                                if (status == LineDraw.OPERATE_STATUS_LINE_IN_TOUCH_START ||
                                    status == LineDraw.OPERATE_STATUS_LINE_IN_TOUCH_END ||
                                    status == LineDraw.OPERATE_STATUS_LINE_IN_TOUCH_CENTER) {
                                    mLineDraw.operateStatus = LineDraw.OPERATE_STATUS_LINE_REMOVE
                                }
                            }
                            DrawModel.DRAW_RECT -> {
                                val status = mRectDraw.operateStatus
                                if (status in arrayOf(
                                    RectDraw.OPERATE_STATUS_RECTANGLE_LEFT_TOP_CORNER,
                                    RectDraw.OPERATE_STATUS_RECTANGLE_RIGHT_TOP_CORNER,
                                    RectDraw.OPERATE_STATUS_RECTANGLE_RIGHT_BOTTOM_CORNER,
                                    RectDraw.OPERATE_STATUS_RECTANGLE_LEFT_BOTTOM_CORNER,
                                    RectDraw.OPERATE_STATUS_RECTANGLE_LEFT_EDGE,
                                    RectDraw.OPERATE_STATUS_RECTANGLE_TOP_EDGE,
                                    RectDraw.OPERATE_STATUS_RECTANGLE_RIGHT_EDGE,
                                    RectDraw.OPERATE_STATUS_RECTANGLE_BOTTOM_EDGE,
                                    RectDraw.OPERATE_STATUS_RECTANGLE_MOVE_ENTIRE)) {
                                    mRectDraw.operateStatus = RectDraw.OPERATE_STATUS_RECTANGLE_STATUS_REMOVE
                                }
                            }
                            else -> {}
                        }
                    }
                }
                return super.onDoubleTapEvent(e)
            }

            override fun onDown(e: MotionEvent): Boolean {
                Log.d(TAG, "onDown")
                pause()

                mFirstX = e.x.coerceIn(BORDER_PX.toFloat(), mViewWidth - BORDER_PX.toFloat())
                mFirstY = e.y.coerceIn(BORDER_PX.toFloat(), mViewHeight - BORDER_PX.toFloat())
                mRawX = e.rawX
                mRawY = e.rawY
                
                Log.d(TAG, "onDown mFirstX : $mFirstX")
                Log.d(TAG, "onDown mFirstY : $mFirstY")
                
                when (mDrawModel) {
                    DrawModel.DRAW_POINT -> {
                        val indexPointTouch = mPointDraw.checkTouchPointInclude(mFirstX, mFirstY)
                        Log.d(TAG, "indexPointTouch : $indexPointTouch")
                        mPointDraw.operateStatus = if (indexPointTouch != -1) {
                            PointDraw.OPERATE_STATUS_POINT_IN_TOUCH
                        } else {
                            PointDraw.OPERATE_STATUS_POINT_ADD
                        }
                    }
                    DrawModel.DRAW_LINE -> {
                        val indexLineTouch = mLineDraw.checkTouchLineInclude(mFirstX.toInt(), mFirstY.toInt())
                        Log.d(TAG, "indexLineTouch : $indexLineTouch")
                        if (indexLineTouch != -1) {
                            mLineDraw.changeTouchLineOperateStatus(mFirstX, mFirstY)
                        } else {
                            mLineDraw.operateStatus = LineDraw.OPERATE_STATUS_LINE_ADD
                        }
                    }
                    DrawModel.DRAW_RECT -> {
                        val indexRectTouch = mRectDraw.checkTouchRectInclude(mFirstX.toInt(), mFirstY.toInt())
                        Log.d(TAG, "indexRectTouch : $indexRectTouch")
                        if (indexRectTouch != -1) {
                            mRectDraw.changeTouchRectOperateStatus(mFirstX, mFirstY)
                        } else {
                            mRectDraw.operateStatus = RectDraw.OPERATE_STATUS_RECTANGLE_STATUS_ADD
                        }
                    }
                    else -> {}
                }
                return super.onDown(e)
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                mCurX = e2.x.coerceIn(BORDER_PX.toFloat(), mViewWidth - BORDER_PX.toFloat())
                mCurY = e2.y.coerceIn(BORDER_PX.toFloat(), mViewHeight - BORDER_PX.toFloat())
                mRawX = e2.rawX
                mRawY = e2.rawY
                mDistanceX = distanceX
                mDistanceY = distanceY

                Log.d(TAG, "onScroll mCurX : $mCurX")
                Log.d(TAG, "onScroll mCurY : $mCurY")
                Log.d(TAG, "onScroll distanceX : $mDistanceX")
                Log.d(TAG, "onScroll distanceY : $mDistanceY")
                
                val moveX = mCurX - mFirstX
                val moveY = mCurY - mFirstY

                when (mDrawModel) {
                    DrawModel.DRAW_POINT -> {
                        if (mPointDraw.operateStatus == PointDraw.OPERATE_STATUS_POINT_IN_TOUCH) {
                            mPointDraw.changeTouchPointLocationByIndex(mPointDraw.touchInclude, mCurX, mCurY)
                        }
                        doTouchDraw()
                    }
                    DrawModel.DRAW_LINE -> {
                        val status = mLineDraw.operateStatus
                        if (status == LineDraw.OPERATE_STATUS_LINE_IN_TOUCH_START ||
                            status == LineDraw.OPERATE_STATUS_LINE_IN_TOUCH_END ||
                            status == LineDraw.OPERATE_STATUS_LINE_IN_TOUCH_CENTER) {
                            mLineDraw.changeTouchLineLocationByIndex(moveX, moveY)
                        }
                        doTouchDraw()
                    }
                    DrawModel.DRAW_RECT -> {
                        val status = mRectDraw.operateStatus
                        if (status in arrayOf(
                            RectDraw.OPERATE_STATUS_RECTANGLE_LEFT_TOP_CORNER,
                            RectDraw.OPERATE_STATUS_RECTANGLE_RIGHT_TOP_CORNER,
                            RectDraw.OPERATE_STATUS_RECTANGLE_RIGHT_BOTTOM_CORNER,
                            RectDraw.OPERATE_STATUS_RECTANGLE_LEFT_BOTTOM_CORNER,
                            RectDraw.OPERATE_STATUS_RECTANGLE_LEFT_EDGE,
                            RectDraw.OPERATE_STATUS_RECTANGLE_TOP_EDGE,
                            RectDraw.OPERATE_STATUS_RECTANGLE_RIGHT_EDGE,
                            RectDraw.OPERATE_STATUS_RECTANGLE_BOTTOM_EDGE,
                            RectDraw.OPERATE_STATUS_RECTANGLE_MOVE_ENTIRE)) {
                            mRectDraw.changeTouchLineLocationByIndex(mRectDraw.touchInclude, moveX, moveY)
                        }
                        doTouchDraw()
                    }
                    else -> {}
                }
                return super.onScroll(e1, e2, distanceX, distanceY)
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                Log.d(TAG, "onFling")
                return super.onFling(e1, e2, velocityX, velocityY)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return super.onSingleTapConfirmed(e)
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                Log.d(TAG, "onSingleTapUp")

                mCurX = e.x.coerceIn(BORDER_PX.toFloat(), mViewWidth - BORDER_PX.toFloat())
                mCurY = e.y.coerceIn(BORDER_PX.toFloat(), mViewHeight - BORDER_PX.toFloat())
                mRawX = e.rawX
                mRawY = e.rawY
                
                when (mDrawModel) {
                    DrawModel.DRAW_POINT, DrawModel.DRAW_LINE, DrawModel.DRAW_RECT -> {
                        // Handle specific cases if needed
                    }
                    else -> {}
                }
                resume()
                return super.onSingleTapUp(e)
            }

            override fun onLongPress(e: MotionEvent) {
                Log.d(TAG, "onLongPress")
                super.onLongPress(e)
            }

            override fun onShowPress(e: MotionEvent) {
                Log.d(TAG, "onShowPress")
                super.onShowPress(e)
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mGestureDetector.onTouchEvent(event)
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {}
            MotionEvent.ACTION_MOVE -> {}
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                Log.d(TAG, "ACTION_UP OR ACTION_POINTER_UP")
                when (mDrawModel) {
                    DrawModel.DRAW_POINT -> {
                        when (mPointDraw.operateStatus) {
                            PointDraw.OPERATE_STATUS_POINT_ADD -> {
                                mPointDraw.addPoint(1, mCurX, mCurY)
                            }
                            PointDraw.OPERATE_STATUS_POINT_REMOVE -> {
                                mPointDraw.removePoint(mPointDraw.touchInclude)
                            }
                        }
                        doShapeDraw()
                    }
                    DrawModel.DRAW_LINE -> {
                        when (mLineDraw.operateStatus) {
                            LineDraw.OPERATE_STATUS_LINE_ADD -> {
                                mLineDraw.addLine(mFirstX.toInt(), mFirstY.toInt(), mCurX.toInt(), mCurY.toInt())
                            }
                            LineDraw.OPERATE_STATUS_LINE_REMOVE -> {
                                mLineDraw.removeLine(mLineDraw.touchInclude)
                            }
                            else -> {
                                mLineDraw.changeTouchPointLocation()
                            }
                        }
                    }
                    DrawModel.DRAW_RECT -> {
                        when (mRectDraw.operateStatus) {
                            RectDraw.OPERATE_STATUS_RECTANGLE_STATUS_ADD -> {
                                val left = min(mFirstX, mCurX).toInt()
                                val right = max(mFirstX, mCurX).toInt()
                                val top = min(mFirstY, mCurY).toInt()
                                val bottom = max(mFirstY, mCurY).toInt()
                                mRectDraw.addRect(left, top, right, bottom)
                            }
                            RectDraw.OPERATE_STATUS_RECTANGLE_STATUS_REMOVE -> {
                                mRectDraw.removeRect(mRectDraw.touchInclude)
                            }
                            else -> {
                                mRectDraw.changeTouchRectLocation()
                            }
                        }
                        doShapeDraw()
                    }
                    else -> {}
                }
                resume()
            }
            MotionEvent.ACTION_CANCEL -> {}
        }
        return true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated")
        mCanDraw = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed")
        mCanDraw = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var initialWidth = MeasureSpec.getSize(widthMeasureSpec)
        var initialHeight = MeasureSpec.getSize(heightMeasureSpec)

        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom

        initialWidth -= paddingLeft + paddingRight
        initialHeight -= paddingTop + paddingBottom

        mViewWidth = initialWidth
        mViewHeight = initialHeight

        mPointDraw.viewWidth = mViewWidth
        mPointDraw.viewHeight = mViewHeight
        mLineDraw.viewWidth = mViewWidth
        mLineDraw.viewHeight = mViewHeight
        mRectDraw.viewWidth = mViewWidth
        mRectDraw.viewHeight = mViewHeight

        mTempWidth = getTempWidth()
        mTempHeight = getTempHeight()

        xScale = initialWidth.toFloat() / mTempWidth.toFloat()
        yScale = initialHeight.toFloat() / mTempHeight.toFloat()

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private inner class TempThread : Thread() {
        var isRun = true

        override fun run() {
            while (!currentThread().isInterrupted) {
                try {
                    if (!isRun) {
                        sleep(1000)
                        continue
                    }

                    Log.d(TAG, "TempThread running")
                    sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    Log.d(TAG, "TempThread InterruptedException : ${e.message}")
                    mTempThread?.let {
                        it.interrupt()
                        Log.d(TAG, "TempThread interrupt")
                    }
                }
            }
        }
    }

    private inner class DrawThread : Thread() {
        var isRun = true

        override fun run() {
            while (!currentThread().isInterrupted) {
                try {
                    if (!isRun) {
                        continue
                    }
                    doShapeDraw()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d(TAG, "DrawThread Exception : ${e.message}")
                }
            }
        }
    }

    private fun doShapeDraw() {
        if (!::mSurfaceHolder.isInitialized || !mCanDraw) {
            return
        }
        
        var canvas: Canvas? = null
        try {
            synchronized(mCanvasLock) {
                canvas = mSurfaceHolder.lockCanvas()
                canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                if (mPointDraw.pointViewList.isNotEmpty() ||
                    mLineDraw.lineViewList.isNotEmpty() ||
                    mRectDraw.rectViewList.isNotEmpty()) {
                    
                    val tempResultBeans = generateViewData(
                        mPointDraw.pointViewList,
                        mLineDraw.lineViewList,
                        mRectDraw.rectViewList
                    )

                    tempResultBeans.forEachIndexed { i, tempResultBean ->
                        // Update point views
                        mPointDraw.pointViewList.filter { it.id == tempResultBean.id }
                            .forEach { it.tempPoint = Point(tempResultBean.max_temp_x, tempResultBean.max_temp_y) }

                        // Update line views
                        mLineDraw.lineViewList.filter { it.id == tempResultBean.id }
                            .forEach { 
                                it.highTempPoint = Point(tempResultBean.max_temp_x, tempResultBean.max_temp_y)
                                it.lowTempPoint = Point(tempResultBean.min_temp_x, tempResultBean.min_temp_y)
                            }

                        // Update rect views
                        mRectDraw.rectViewList.filter { it.id == tempResultBean.id }
                            .forEach { 
                                it.highTempPoint = Point(tempResultBean.max_temp_x, tempResultBean.max_temp_y)
                                it.lowTempPoint = Point(tempResultBean.min_temp_x, tempResultBean.min_temp_y)
                            }
                    }
                    
                    canvas?.let {
                        mPointDraw.onDraw(it, false)
                        mLineDraw.onDraw(it, false)
                        mRectDraw.onDraw(it, false)
                        drawTempData(mContext, mLineDraw.mScreenDegree, it, tempResultBeans)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "DrawThread Exception : ${e.message}")
        } finally {
            canvas?.let { mSurfaceHolder.unlockCanvasAndPost(it) }
        }
    }

    private fun doTouchDraw() {
        if (!::mSurfaceHolder.isInitialized || !mCanDraw) {
            return
        }
        
        var canvas: Canvas? = null
        try {
            synchronized(mCanvasLock) {
                canvas = mSurfaceHolder.lockCanvas()
                canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                when (mDrawModel) {
                    DrawModel.DRAW_POINT -> {
                        if (mPointDraw.operateStatus == PointDraw.OPERATE_STATUS_POINT_ADD) {
                            canvas?.let { mPointDraw.onTempDraw(it, 1, mCurX, mCurY) }
                        }
                    }
                    DrawModel.DRAW_LINE -> {
                        if (mLineDraw.operateStatus == LineDraw.OPERATE_STATUS_LINE_ADD) {
                            canvas?.let { mLineDraw.onTempDraw(it, mFirstX.toInt(), mFirstY.toInt(), mCurX.toInt(), mCurY.toInt()) }
                        }
                    }
                    DrawModel.DRAW_RECT -> {
                        if (mRectDraw.operateStatus == RectDraw.OPERATE_STATUS_RECTANGLE_STATUS_ADD) {
                            canvas?.let { mRectDraw.onTempDraw(it, mFirstX.toInt(), mFirstY.toInt(), mCurX.toInt(), mCurY.toInt()) }
                        }
                    }
                    else -> {}
                }
                
                canvas?.let {
                    mPointDraw.onDraw(it, true)
                    mLineDraw.onDraw(it, true)
                    mRectDraw.onDraw(it, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "DrawThread Exception : ${e.message}")
        } finally {
            canvas?.let { mSurfaceHolder.unlockCanvasAndPost(it) }
        }
    }

    abstract fun generateViewData(
        pointViews: LinkedList<PointDraw.PointView>,
        lineViews: LinkedList<LineDraw.LineView>,
        rectViews: LinkedList<RectDraw.RectView>
    ): CopyOnWriteArrayList<TempResultBean>

    private fun drawTempData(
        context: Context,
        screenDegree: Int,
        canvas: Canvas,
        tempResultBean: CopyOnWriteArrayList<TempResultBean>
    ) {
        if (tempResultBean.isEmpty()) {
            return
        }

        var y = 10
        var x = 10

        when (screenDegree) {
            90 -> {
                x = mViewWidth - 10
                y = 10
            }
            270 -> {
                x = 10
                y = mViewHeight - 10
            }
            180 -> {
                x = mViewWidth - 10
                y = mViewHeight - 10
            }
        }

        val interval = ScreenUtils.dp2px(mTextWidth + 10)
        var count = 0
        var startIndex = 0
        var pointCount = 0
        
        tempResultBean.forEach { result ->
            val stringBuffer = StringBuffer()
            if (result.label.contains("P")) {
                pointCount++
                stringBuffer.append(result.label)
                    .append(result.content)
                    .append("\n")
                    .append(context.getString(R.string.temp_label))
                    .append(result.maxTemperature)
                    .append("\n")
            } else {
                stringBuffer.append(result.label)
                    .append(result.content)
                    .append("\n")
                    .append(context.getString(R.string.temp_max))
                    .append(result.maxTemperature)
                    .append("\n")
                    .append(context.getString(R.string.temp_avg))
                    .append(result.averageTemperature)
                    .append("\n")
                    .append(context.getString(R.string.temp_min))
                    .append(result.minTemperature)
                    .append("\n")
            }

            val layout = StaticLayout(stringBuffer.toString(), mTextPaint, interval, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
            canvas.save()
            if (count != 0 && count % 3 == 0) {
                startIndex = 0
                y += if (pointCount == 3 && count == 3) {
                    ScreenUtils.dp2px(40)
                } else {
                    ScreenUtils.dp2px(80)
                }
            }
            canvas.translate((x + interval * startIndex).toFloat(), y.toFloat())
            canvas.rotate(screenDegree.toFloat())
            layout.draw(canvas)
            canvas.restore()
            count++
            startIndex++
        }
    }
}