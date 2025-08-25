package com.infisense.usbir.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import com.blankj.utilcode.util.SizeUtils
import com.energy.iruvc.dual.DualUVCCamera
import com.energy.iruvc.sdkisp.LibIRTemp
import com.energy.iruvc.utils.DualCameraParams
import com.energy.iruvc.utils.Line
import com.energy.iruvc.utils.SynchronizedBitmap
import com.infisense.usbdual.Const
import com.infisense.usbdual.camera.BaseDualView
import com.infisense.usbir.R
import com.infisense.usbir.inf.ILiteListener
import com.infisense.usbir.utils.TempDrawHelper
import com.infisense.usbir.utils.TempUtil
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.tools.UnitTools
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class TemperatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle), SurfaceHolder.Callback, View.OnTouchListener, BaseDualView.OnFrameCallback {

    companion object {
        private const val TAG = "TemperatureView"
        private val TOUCH_TOLERANCE = SizeUtils.sp2px(7f)

        const val REGION_MODE_RESET = -1
        const val REGION_MODE_POINT = 0
        const val REGION_MODE_LINE = 1
        const val REGION_MODE_RECTANGLE = 2
        const val REGION_MODE_CENTER = 3
        const val REGION_NODE_TREND = 4
        const val REGION_MODE_CLEAN = 5
    }

    @IntDef(REGION_MODE_RESET, REGION_MODE_POINT, REGION_MODE_LINE, REGION_MODE_RECTANGLE, REGION_MODE_CENTER, REGION_NODE_TREND, REGION_MODE_CLEAN)
    @Retention(AnnotationRetention.SOURCE)
    annotation class RegionMode

    private var drawCount = 3

    private val POINT_MAX_COUNT: Int
    private val LINE_MAX_COUNT: Int
    private val RECTANGLE_MAX_COUNT: Int

    private var irtemp: LibIRTemp? = null

    private var xScale = 0f
    private var yScale = 0f
    private var viewWidth = 0
    private var viewHeight = 0
    private var temperatureWidth = 0
    private var temperatureHeight = 0

    private val helper = TempDrawHelper()

    @RegionMode
    private var temperatureRegionMode = REGION_MODE_CLEAN

    @RegionMode
    fun getTemperatureRegionMode(): Int = temperatureRegionMode

    fun setTemperatureRegionMode(@RegionMode temperatureRegionMode: Int) {
        this.temperatureRegionMode = temperatureRegionMode
        when (temperatureRegionMode) {
            REGION_MODE_CENTER -> isShowFull = true
            REGION_MODE_CLEAN -> isShowFull = false
        }
    }

    private var isShowFull = false
    fun isShowFull(): Boolean = isShowFull
    fun setShowFull(showFull: Boolean) {
        isShowFull = showFull
        if (temperatureRegionMode == REGION_MODE_CLEAN) {
            temperatureRegionMode = REGION_MODE_CENTER
        }
    }

    fun setTextSize(textSize: Int) {
        helper.textSize = textSize
        refreshRegion()
    }

    fun setLinePaintColor(@ColorInt color: Int) {
        helper.textColor = color
        refreshRegion()
    }

    private fun refreshRegion() {
        val surfaceViewCanvas = holder.lockCanvas()
        surfaceViewCanvas?.let { canvas ->
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            setBitmap()
            canvas.drawBitmap(regionBitmap!!, Rect(0, 0, viewWidth, viewHeight), Rect(0, 0, viewWidth, viewHeight), null)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private var onTrendChangeListener: OnTrendChangeListener? = null
    fun setOnTrendChangeListener(listener: OnTrendChangeListener?) {
        this.onTrendChangeListener = listener
    }

    private var onTrendAddListener: Runnable? = null
    fun setOnTrendAddListener(listener: Runnable?) {
        this.onTrendAddListener = listener
    }

    private var onTrendRemoveListener: Runnable? = null
    fun setOnTrendRemoveListener(listener: Runnable?) {
        this.onTrendRemoveListener = listener
    }

    private var iLiteListener: ILiteListener? = null
    fun setiLiteListener(listener: ILiteListener?) {
        this.iLiteListener = listener
    }

    private var listener: TempListener? = null
    fun getListener(): TempListener? = listener
    fun setListener(listener: TempListener?) {
        this.listener = listener
    }

    private var isMonitor = false
    fun setMonitor(monitor: Boolean) {
        isMonitor = monitor
    }

    private var isUserHighTemp = false
    fun isUserHighTemp(): Boolean = isUserHighTemp
    fun setUserHighTemp(userHighTemp: Boolean) {
        this.isUserHighTemp = userHighTemp
    }

    private var isUserLowTemp = false
    fun isUserLowTemp(): Boolean = isUserLowTemp
    fun setUserLowTemp(userLowTemp: Boolean) {
        this.isUserLowTemp = userLowTemp
    }

    private var syncimage: SynchronizedBitmap? = null
    fun setSyncimage(syncimage: SynchronizedBitmap?) {
        this.syncimage = syncimage
    }

    private var temperature: ByteArray? = null
    fun setTemperature(temperature: ByteArray?) {
        this.temperature = temperature
    }

    private fun setDefPoint(point: Point) {
        when {
            point.x > temperatureWidth && point.x > 0 -> point.x = temperatureWidth
            point.x <= 0 -> point.x = 0
        }
        when {
            point.y > temperatureHeight -> point.y = temperatureHeight
            point.y < 0 -> point.y = 0
        }
    }

    fun getPointTemp(point: Point): LibIRTemp.TemperatureSampleResult? {
        return irtemp?.let { temp ->
            setDefPoint(point)
            temp.getTemperatureOfPoint(point)
        }
    }

    fun getLineTemp(line: Line): LibIRTemp.TemperatureSampleResult? {
        return irtemp?.let { temp ->
            setDefPoint(line.start)
            setDefPoint(line.end)
            temp.getTemperatureOfLine(line)
        }
    }

    fun getRectTemp(rect: Rect): LibIRTemp.TemperatureSampleResult? {
        return irtemp?.let { temp ->
            rect.apply {
                if (top < 0) top = 0
                if (bottom > temperatureHeight) bottom = temperatureHeight
                if (left < 0) left = 0
                if (right > temperatureWidth) right = temperatureWidth
            }
            temp.getTemperatureOfRect(rect)
        }
    }

    var productType = Const.TYPE_IR

    private var trendLine: Line? = null
    private val pointList = ArrayList<Point>()
    private val lineList = ArrayList<Line>()
    private val rectList = ArrayList<Rect>()

    private val pointResultList = ArrayList<LibIRTemp.TemperatureSampleResult>(3)
    private val lineResultList = ArrayList<LibIRTemp.TemperatureSampleResult>(3)
    private val rectangleResultList = ArrayList<LibIRTemp.TemperatureSampleResult>(3)

    private var regionBitmap: Bitmap? = null
    private var regionAndValueBitmap: Bitmap? = null

    fun getRegionBitmap(): Bitmap? = regionAndValueBitmap
    fun getRegionAndValueBitmap(): Bitmap? {
        synchronized(regionLock) {
            return regionAndValueBitmap
        }
    }

    private val runnable: Runnable
    private var temperatureThread: Thread? = null
    private val regionLock = Any()
    @Volatile
    private var runflag = false

    private val isShowC = SharedManager.INSTANCE.temperature == 1

    private var iTsTempListenerWeakReference: WeakReference<ITsTempListener>? = null

    fun setImageSize(imageWidth: Int, imageHeight: Int, iTsTempListener: ITsTempListener?) {
        iTsTempListener?.let { listener ->
            iTsTempListenerWeakReference = WeakReference(listener)
        }
        this.temperatureWidth = imageWidth
        this.temperatureHeight = imageHeight
        if (viewWidth == 0) {
            viewWidth = measuredWidth
        }
        if (viewHeight == 0) {
            viewHeight = measuredHeight
        }
        xScale = viewWidth.toFloat() / imageWidth.toFloat()
        yScale = viewHeight.toFloat() / imageHeight.toFloat()
        irtemp = LibIRTemp(imageWidth, imageHeight)
        llTempData = ByteArray(imageHeight * imageWidth * 2)
        repeat(drawCount) {
            pointResultList.add(irtemp!!.TemperatureSampleResult())
            lineResultList.add(irtemp!!.TemperatureSampleResult())
            rectangleResultList.add(irtemp!!.TemperatureSampleResult())
        }
    }

    fun restView() {
        viewWidth = 0
        viewHeight = 0
        viewWidth = measuredWidth
        xScale = viewWidth.toFloat() / temperatureWidth.toFloat()
        viewHeight = measuredHeight
        yScale = viewHeight.toFloat() / temperatureHeight.toFloat()
    }

    private var isShow = false

    fun start() {
        if (!runflag) {
            runflag = true
            temperatureThread = Thread(runnable)
            visibility = if (isShow) VISIBLE else INVISIBLE
            temperatureThread?.start()
        }
    }

    fun stop() {
        runflag = false
        isShow = visibility == View.VISIBLE
        try {
            temperatureThread?.apply {
                interrupt()
                join()
            }
            temperatureThread = null
        } catch (ignored: InterruptedException) {
        }
    }

    fun clear() {
        onTrendRemoveListener?.run()
        trendLine = null
        pointList.clear()
        lineList.clear()
        rectList.clear()
        regionBitmap?.eraseColor(0)
        
        val surfaceViewCanvas = holder.lockCanvas()
        surfaceViewCanvas?.let { canvas ->
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            regionBitmap?.let { bitmap ->
                canvas.drawBitmap(bitmap, Rect(0, 0, viewWidth, viewHeight), Rect(0, 0, viewWidth, viewHeight), null)
            }
            holder.unlockCanvasAndPost(canvas)
        }

        pointResultList.forEach { it.index = 0 }
        lineResultList.forEach { it.index = 0 }
        rectangleResultList.forEach { it.index = 0 }
    }

    fun addScalePoint(point: Point) {
        val sx = measuredWidth.toFloat() / temperatureWidth.toFloat()
        val sy = measuredHeight.toFloat() / temperatureHeight.toFloat()
        val viewX = TempDrawHelper.correctPoint((point.x * sx).toInt(), measuredWidth)
        val viewY = TempDrawHelper.correctPoint((point.y * sy).toInt(), measuredHeight)
        if (pointList.size == POINT_MAX_COUNT) {
            pointList.removeAt(0)
        }
        pointList.add(Point(viewX, viewY))
    }

    fun addScaleLine(l: Line) {
        val sx = measuredWidth.toFloat() / temperatureWidth.toFloat()
        val sy = measuredHeight.toFloat() / temperatureHeight.toFloat()
        val line = Line(Point(), Point()).apply {
            start.x = TempDrawHelper.correct((l.start.x * sx).toInt(), measuredWidth)
            start.y = TempDrawHelper.correct((l.start.y * sy).toInt(), measuredHeight)
            end.x = TempDrawHelper.correct((l.end.x * sx).toInt(), measuredWidth)
            end.y = TempDrawHelper.correct((l.end.y * sy).toInt(), measuredHeight)
        }
        if (lineList.size == LINE_MAX_COUNT) {
            lineList.removeAt(0)
        }
        lineList.add(line)
    }

    fun addScaleRectangle(r: Rect) {
        val sx = measuredWidth.toFloat() / temperatureWidth.toFloat()
        val sy = measuredHeight.toFloat() / temperatureHeight.toFloat()
        val rectangle = Rect().apply {
            left = (r.left * sx).toInt()
            top = (r.top * sy).toInt()
            right = (r.right * sx).toInt()
            bottom = (r.bottom * sy).toInt()
        }
        
        if (rectList.size < RECTANGLE_MAX_COUNT) {
            rectList.add(rectangle)
        } else {
            for (index in 0 until rectList.size - 1) {
                val tempRectangle = rectList[index + 1]
                rectList[index] = tempRectangle
            }
            rectList[rectList.size - 1] = rectangle
        }
    }

    fun getPoint(): Point? {
        return if (pointList.isEmpty()) null
        else Point((pointList[0].x / xScale).toInt(), (pointList[0].y / yScale).toInt())
    }

    fun getLine(): Line? {
        return if (lineList.isNotEmpty()) {
            Line(Point(), Point()).apply {
                start.x = (lineList[0].start.x / xScale).toInt()
                start.y = (lineList[0].start.y / yScale).toInt()
                end.x = (lineList[0].end.x / xScale).toInt()
                end.y = (lineList[0].end.y / yScale).toInt()
            }
        } else null
    }

    fun getRectangle(): Rect? {
        return if (rectList.isNotEmpty()) {
            Rect().apply {
                left = (rectList[0].left / xScale).toInt()
                top = (rectList[0].top / yScale).toInt()
                right = (rectList[0].right / xScale).toInt()
                bottom = (rectList[0].bottom / yScale).toInt()
            }
        } else null
    }

    fun drawLine() {
        setBitmap()
    }

    // Enums for movement types
    private enum class LineMoveType { ALL, START, END }
    private enum class RectMoveType { ALL, EDGE, CORNER }
    private enum class RectMoveEdge { LEFT, TOP, RIGHT, BOTTOM }
    private enum class RectMoveCorner { LT, RT, RB, LB }

    // Touch handling variables
    private var downX = 0f
    private var downY = 0f
    private var currentLineIndex = -1
    private var currentPointIndex = -1
    private var currentRectangleIndex = -1
    private var lineMoveType = LineMoveType.ALL
    private var rectMoveType = RectMoveType.ALL
    private var rectMoveEdge = RectMoveEdge.LEFT
    private var rectMoveCorner = RectMoveCorner.LT
    private var isTrendMove = false

    init {
        setZOrderOnTop(true)
        holder.addCallback(this)
        setOnTouchListener(this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.TemperatureView)
        try {
            drawCount = ta.getInteger(R.styleable.TemperatureView_temperature_count, 3)
        } catch (e: Exception) {
            // Handle exception
        } finally {
            ta.recycle()
        }

        POINT_MAX_COUNT = drawCount
        LINE_MAX_COUNT = drawCount
        RECTANGLE_MAX_COUNT = drawCount

        runnable = Runnable {
            while (!Thread.currentThread().isInterrupted && runflag) {
                val tempArray = when (productType) {
                    Const.TYPE_IR_DUAL -> {
                        try {
                            if (remapTempData == null) {
                                Log.d(TAG, "remapTempData == NULL")
                                if (dualUVCCamera != null && llTempData != null && dualUVCCamera!!.getDualIRWidthFromFrame() != 0 && dualUVCCamera!!.getDualIRHeightFromFrame() != 0) {
                                    dualUVCCamera!!.getTemperatureDataFromFrame(llTempData)
                                    llTempData
                                } else {
                                    SystemClock.sleep(50)
                                    continue
                                }
                            } else {
                                remapTempData
                            }
                        } catch (e: Exception) {
                            SystemClock.sleep(50)
                            continue
                        }
                    }
                    else -> {
                        if (syncimage?.start != true) {
                            SystemClock.sleep(50)
                            continue
                        }
                        synchronized(syncimage!!.dataLock) {
                            temperature
                        }
                    }
                }

                tempArray?.let { array ->
                    synchronized(regionLock) {
                        irtemp?.let { temp ->
                            temp.updateTemperatureData(array)

                            var max = temp.getMaxTemperature()
                            var min = temp.getMinTemperature()

                            if (isUserHighTemp || isUserLowTemp) {
                                if (isUserHighTemp) {
                                    max = temp.getUserHighTemperature()
                                }
                                if (isUserLowTemp) {
                                    min = temp.getUserLowTemperature()
                                }
                            }

                            max = getTSTemp(max)
                            min = getTSTemp(min)

                            listener?.getTemp(max, min, array)

                            if (isShowFull) {
                                regionAndValueBitmap = drawRegionAndValue()
                            }

                            // Handle trend line temperature updates
                            trendLine?.let { trend ->
                                val tempResult = temp.getTemperatureOfLine(
                                    Line(
                                        Point((trend.start.x / xScale).toInt(), (trend.start.y / yScale).toInt()),
                                        Point((trend.end.x / xScale).toInt(), (trend.end.y / yScale).toInt())
                                    )
                                )
                                val temps = mutableListOf<Float>()
                                for (i in 0 until tempResult.count) {
                                    temps.add(getTSTemp(tempResult.temperatureArray!![i]))
                                }
                                onTrendChangeListener?.onChange(temps)
                            }
                        }
                    }
                }
                SystemClock.sleep(100)
            }
        }
    }

    private fun drawRegionAndValue(): Bitmap? {
        val bitmap = regionBitmap?.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        val canvas = Canvas(bitmap)

        // Draw temperature values for points
        pointList.forEachIndexed { index, point ->
            if (index < pointResultList.size) {
                val result = getPointTemp(Point((point.x / xScale).toInt(), (point.y / yScale).toInt()))
                result?.let { r ->
                    val tempText = UnitTools.formatTemperatureUnit(getTSTemp(r.averageTemperature), isShowC)
                    drawTempText(canvas, tempText, point)
                }
            }
        }

        // Draw temperature values for lines
        lineList.forEachIndexed { index, line ->
            if (index < lineResultList.size) {
                val result = getLineTemp(
                    Line(
                        Point((line.start.x / xScale).toInt(), (line.start.y / yScale).toInt()),
                        Point((line.end.x / xScale).toInt(), (line.end.y / yScale).toInt())
                    )
                )
                result?.let { r ->
                    val maxTemp = UnitTools.formatTemperatureUnit(getTSTemp(r.maxTemperature), isShowC)
                    val minTemp = UnitTools.formatTemperatureUnit(getTSTemp(r.minTemperature), isShowC)
                    val avgTemp = UnitTools.formatTemperatureUnit(getTSTemp(r.averageTemperature), isShowC)
                    val tempText = "MAX:$maxTemp MIN:$minTemp AVG:$avgTemp"
                    
                    val centerX = (line.start.x + line.end.x) / 2
                    val centerY = (line.start.y + line.end.y) / 2
                    drawTempText(canvas, tempText, centerX, centerY)
                }
            }
        }

        // Draw temperature values for rectangles  
        rectList.forEachIndexed { index, rect ->
            if (index < rectangleResultList.size) {
                val result = getRectTemp(
                    Rect(
                        (rect.left / xScale).toInt(), (rect.top / yScale).toInt(),
                        (rect.right / xScale).toInt(), (rect.bottom / yScale).toInt()
                    )
                )
                result?.let { r ->
                    val maxTemp = UnitTools.formatTemperatureUnit(getTSTemp(r.maxTemperature), isShowC)
                    val minTemp = UnitTools.formatTemperatureUnit(getTSTemp(r.minTemperature), isShowC)
                    val avgTemp = UnitTools.formatTemperatureUnit(getTSTemp(r.averageTemperature), isShowC)
                    val tempText = "MAX:$maxTemp MIN:$minTemp AVG:$avgTemp"
                    
                    val centerX = (rect.left + rect.right) / 2
                    val centerY = (rect.top + rect.bottom) / 2
                    drawTempText(canvas, tempText, centerX, centerY)

                    // Draw max/min position dots
                    drawDot(canvas, Point(r.maxTemperatureX, r.maxTemperatureY), true)
                    drawDot(canvas, Point(r.minTemperatureX, r.minTemperatureY), false)
                }
            }
        }

        return bitmap
    }

    // Touch event handling
    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        if (temperatureRegionMode == REGION_MODE_CLEAN) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> handleTouchDown(event)
            MotionEvent.ACTION_MOVE -> handleTouchMove(event)
            MotionEvent.ACTION_UP -> handleTouchUp()
        }
        return true
    }

    private fun handleTouchDown(event: MotionEvent) {
        downX = event.x
        downY = event.y

        when (temperatureRegionMode) {
            REGION_MODE_POINT -> addPoint(downX.toInt(), downY.toInt())
            REGION_MODE_LINE -> startDrawLine(downX.toInt(), downY.toInt())
            REGION_MODE_RECTANGLE -> startDrawRect(downX.toInt(), downY.toInt())
            REGION_NODE_TREND -> startDrawTrend(downX.toInt(), downY.toInt())
            else -> checkForExistingRegionTouch(downX, downY)
        }
    }

    private fun handleTouchMove(event: MotionEvent) {
        val moveX = event.x
        val moveY = event.y

        when {
            currentPointIndex >= 0 -> movePoint(currentPointIndex, moveX.toInt(), moveY.toInt())
            currentLineIndex >= 0 -> moveLine(currentLineIndex, moveX.toInt(), moveY.toInt())
            currentRectangleIndex >= 0 -> moveRect(currentRectangleIndex, moveX.toInt(), moveY.toInt())
            isTrendMove -> moveTrendLine(moveX.toInt(), moveY.toInt())
            temperatureRegionMode == REGION_MODE_LINE && lineList.isNotEmpty() -> {
                updateDrawingLine(moveX.toInt(), moveY.toInt())
            }
            temperatureRegionMode == REGION_MODE_RECTANGLE && rectList.isNotEmpty() -> {
                updateDrawingRect(moveX.toInt(), moveY.toInt())
            }
            temperatureRegionMode == REGION_NODE_TREND && trendLine != null -> {
                updateDrawingTrend(moveX.toInt(), moveY.toInt())
            }
        }
    }

    private fun handleTouchUp() {
        currentPointIndex = -1
        currentLineIndex = -1
        currentRectangleIndex = -1
        isTrendMove = false

        when (temperatureRegionMode) {
            REGION_NODE_TREND -> {
                trendLine?.let {
                    onTrendAddListener?.run()
                }
            }
        }
        
        refreshDisplay()
    }

    private fun addPoint(x: Int, y: Int) {
        if (pointList.size >= POINT_MAX_COUNT) {
            pointList.removeAt(0)
        }
        pointList.add(Point(x, y))
        refreshDisplay()
    }

    private fun startDrawLine(x: Int, y: Int) {
        val line = Line(Point(x, y), Point(x, y))
        if (lineList.size >= LINE_MAX_COUNT) {
            lineList.removeAt(0)
        }
        lineList.add(line)
    }

    private fun startDrawRect(x: Int, y: Int) {
        val rect = Rect(x, y, x, y)
        if (rectList.size >= RECTANGLE_MAX_COUNT) {
            rectList.removeAt(0)
        }
        rectList.add(rect)
    }

    private fun startDrawTrend(x: Int, y: Int) {
        trendLine = Line(Point(x, y), Point(x, y))
    }

    private fun updateDrawingLine(x: Int, y: Int) {
        lineList.lastOrNull()?.end?.set(x, y)
        refreshDisplay()
    }

    private fun updateDrawingRect(x: Int, y: Int) {
        rectList.lastOrNull()?.let { rect ->
            rect.right = x
            rect.bottom = y
        }
        refreshDisplay()
    }

    private fun updateDrawingTrend(x: Int, y: Int) {
        trendLine?.end?.set(x, y)
        refreshDisplay()
    }

    private fun checkForExistingRegionTouch(x: Float, y: Float) {
        // Check for point touch
        pointList.forEachIndexed { index, point ->
            if (isPointTouched(point, x, y)) {
                currentPointIndex = index
                return
            }
        }

        // Check for line touch
        lineList.forEachIndexed { index, line ->
            val touchResult = isLineTouched(line, x, y)
            if (touchResult != LineMoveType.ALL) {
                currentLineIndex = index
                lineMoveType = touchResult
                return
            }
        }

        // Check for rectangle touch
        rectList.forEachIndexed { index, rect ->
            val touchResult = isRectTouched(rect, x, y)
            if (touchResult != RectMoveType.ALL) {
                currentRectangleIndex = index
                rectMoveType = touchResult
                return
            }
        }

        // Check for trend line touch
        trendLine?.let { trend ->
            if (isLineTouched(trend, x, y) != LineMoveType.ALL) {
                isTrendMove = true
                return
            }
        }
    }

    private fun isPointTouched(point: Point, x: Float, y: Float): Boolean {
        val dx = kotlin.math.abs(point.x - x)
        val dy = kotlin.math.abs(point.y - y)
        return dx <= TOUCH_TOLERANCE && dy <= TOUCH_TOLERANCE
    }

    private fun isLineTouched(line: Line, x: Float, y: Float): LineMoveType {
        // Check start point
        if (isPointTouched(line.start, x, y)) return LineMoveType.START
        
        // Check end point
        if (isPointTouched(line.end, x, y)) return LineMoveType.END
        
        // Check line segment
        val dist = TempUtil.distanceFromPointToLine(x, y, line.start.x.toFloat(), line.start.y.toFloat(), line.end.x.toFloat(), line.end.y.toFloat())
        return if (dist <= TOUCH_TOLERANCE) LineMoveType.ALL else LineMoveType.ALL
    }

    private fun isRectTouched(rect: Rect, x: Float, y: Float): RectMoveType {
        // Check corners first
        val cornerSize = TOUCH_TOLERANCE * 2
        
        when {
            // Top-left corner
            x >= rect.left - cornerSize && x <= rect.left + cornerSize && 
            y >= rect.top - cornerSize && y <= rect.top + cornerSize -> {
                rectMoveCorner = RectMoveCorner.LT
                return RectMoveType.CORNER
            }
            // Top-right corner
            x >= rect.right - cornerSize && x <= rect.right + cornerSize &&
            y >= rect.top - cornerSize && y <= rect.top + cornerSize -> {
                rectMoveCorner = RectMoveCorner.RT
                return RectMoveType.CORNER
            }
            // Bottom-right corner
            x >= rect.right - cornerSize && x <= rect.right + cornerSize &&
            y >= rect.bottom - cornerSize && y <= rect.bottom + cornerSize -> {
                rectMoveCorner = RectMoveCorner.RB
                return RectMoveType.CORNER
            }
            // Bottom-left corner
            x >= rect.left - cornerSize && x <= rect.left + cornerSize &&
            y >= rect.bottom - cornerSize && y <= rect.bottom + cornerSize -> {
                rectMoveCorner = RectMoveCorner.LB
                return RectMoveType.CORNER
            }
        }

        // Check edges
        val edgeTolerance = TOUCH_TOLERANCE
        when {
            // Left edge
            x >= rect.left - edgeTolerance && x <= rect.left + edgeTolerance &&
            y >= rect.top && y <= rect.bottom -> {
                rectMoveEdge = RectMoveEdge.LEFT
                return RectMoveType.EDGE
            }
            // Right edge
            x >= rect.right - edgeTolerance && x <= rect.right + edgeTolerance &&
            y >= rect.top && y <= rect.bottom -> {
                rectMoveEdge = RectMoveEdge.RIGHT
                return RectMoveType.EDGE
            }
            // Top edge
            y >= rect.top - edgeTolerance && y <= rect.top + edgeTolerance &&
            x >= rect.left && x <= rect.right -> {
                rectMoveEdge = RectMoveEdge.TOP
                return RectMoveType.EDGE
            }
            // Bottom edge
            y >= rect.bottom - edgeTolerance && y <= rect.bottom + edgeTolerance &&
            x >= rect.left && x <= rect.right -> {
                rectMoveEdge = RectMoveEdge.BOTTOM
                return RectMoveType.EDGE
            }
            // Inside rectangle
            x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom -> {
                return RectMoveType.ALL
            }
        }

        return RectMoveType.ALL
    }

    private fun movePoint(index: Int, x: Int, y: Int) {
        if (index < pointList.size) {
            pointList[index].set(x, y)
            refreshDisplay()
        }
    }

    private fun moveLine(index: Int, x: Int, y: Int) {
        if (index < lineList.size) {
            val line = lineList[index]
            val deltaX = x - downX.toInt()
            val deltaY = y - downY.toInt()

            when (lineMoveType) {
                LineMoveType.START -> line.start.offset(deltaX, deltaY)
                LineMoveType.END -> line.end.offset(deltaX, deltaY)
                LineMoveType.ALL -> {
                    line.start.offset(deltaX, deltaY)
                    line.end.offset(deltaX, deltaY)
                }
            }
            
            downX = x.toFloat()
            downY = y.toFloat()
            refreshDisplay()
        }
    }

    private fun moveRect(index: Int, x: Int, y: Int) {
        if (index < rectList.size) {
            val rect = rectList[index]
            val deltaX = x - downX.toInt()
            val deltaY = y - downY.toInt()

            when (rectMoveType) {
                RectMoveType.ALL -> rect.offset(deltaX, deltaY)
                RectMoveType.EDGE -> {
                    when (rectMoveEdge) {
                        RectMoveEdge.LEFT -> rect.left += deltaX
                        RectMoveEdge.RIGHT -> rect.right += deltaX
                        RectMoveEdge.TOP -> rect.top += deltaY
                        RectMoveEdge.BOTTOM -> rect.bottom += deltaY
                    }
                }
                RectMoveType.CORNER -> {
                    when (rectMoveCorner) {
                        RectMoveCorner.LT -> {
                            rect.left += deltaX
                            rect.top += deltaY
                        }
                        RectMoveCorner.RT -> {
                            rect.right += deltaX
                            rect.top += deltaY
                        }
                        RectMoveCorner.RB -> {
                            rect.right += deltaX
                            rect.bottom += deltaY
                        }
                        RectMoveCorner.LB -> {
                            rect.left += deltaX
                            rect.bottom += deltaY
                        }
                    }
                }
            }
            
            downX = x.toFloat()
            downY = y.toFloat()
            refreshDisplay()
        }
    }

    private fun moveTrendLine(x: Int, y: Int) {
        trendLine?.let { trend ->
            val deltaX = x - downX.toInt()
            val deltaY = y - downY.toInt()
            trend.start.offset(deltaX, deltaY)
            trend.end.offset(deltaX, deltaY)
            downX = x.toFloat()
            downY = y.toFloat()
            refreshDisplay()
        }
    }

    private fun refreshDisplay() {
        val surfaceViewCanvas = holder.lockCanvas()
        surfaceViewCanvas?.let { canvas ->
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            setBitmap()
            regionBitmap?.let { bitmap ->
                canvas.drawBitmap(bitmap, Rect(0, 0, viewWidth, viewHeight), Rect(0, 0, viewWidth, viewHeight), null)
            }
            holder.unlockCanvasAndPost(canvas)
        }
    }

    // Drawing helper methods
    private fun drawPoint(canvas: Canvas, x: Int, y: Int) {
        helper.drawPoint(canvas, x, y)
    }

    private fun drawLine(canvas: Canvas, startX: Int, startY: Int, endX: Int, endY: Int, isTrend: Boolean) {
        helper.drawLine(canvas, startX, startY, endX, endY, isTrend)
    }

    private fun drawRect(canvas: Canvas, left: Int, top: Int, right: Int, bottom: Int) {
        helper.drawRect(canvas, left, top, right, bottom)
    }

    private fun drawCircle(canvas: Canvas, x: Int, y: Int, isMax: Boolean) {
        helper.drawCircle(canvas, x, y, isMax)
    }

    private fun drawDot(canvas: Canvas, point: Point, isMax: Boolean) {
        val x = TempDrawHelper.correct((point.x * xScale).toInt(), width)
        val y = TempDrawHelper.correct((point.y * yScale).toInt(), height)
        helper.drawCircle(canvas, x, y, isMax)
    }

    private fun drawTempText(canvas: Canvas, text: String, x: Int, y: Int) {
        helper.drawTempText(canvas, text, width, x, y)
    }

    private fun drawTempText(canvas: Canvas, text: String, point: Point) {
        val x = TempDrawHelper.correct((point.x * xScale).toInt(), width)
        val y = TempDrawHelper.correct((point.y * yScale).toInt(), height)
        helper.drawTempText(canvas, text, width, x, y)
    }

    private fun setBitmap() {
        regionBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(regionBitmap!!)
        
        pointList.forEach { point ->
            drawPoint(canvas, point.x, point.y)
        }
        
        lineList.forEach { line ->
            drawLine(canvas, line.start.x, line.start.y, line.end.x, line.end.y, false)
        }
        
        rectList.forEach { rect ->
            drawRect(canvas, rect.left, rect.top, rect.right, rect.bottom)
        }
        
        trendLine?.let { trend ->
            drawLine(canvas, trend.start.x, trend.start.y, trend.end.x, trend.end.y, true)
        }
    }

    // SurfaceHolder.Callback implementation
    override fun surfaceCreated(holder: SurfaceHolder) {
        holder.setFormat(PixelFormat.TRANSLUCENT)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        if (temperatureWidth != 0 && temperatureHeight != 0) {
            xScale = width.toFloat() / temperatureWidth.toFloat()
            yScale = height.toFloat() / temperatureHeight.toFloat()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stop()
    }

    // Interface definitions
    interface OnTrendChangeListener {
        fun onChange(temps: List<Float>)
    }

    interface TempListener {
        fun getTemp(max: Float, min: Float, tempData: ByteArray)
    }

    fun getCompensateTemp(temp: Float): Float {
        return iLiteListener?.compensateTemp(temp) ?: temp
    }

    fun getTSTemp(temp: Float): Float {
        return iTsTempListenerWeakReference?.get()?.tempCorrectByTs(getCompensateTemp(temp)) ?: getCompensateTemp(temp)
    }

    fun setUseIRISP(useIRISP: Boolean) {
        irtemp?.setScale(if (useIRISP) 16 else 64)
    }

    fun setCurrentFusionType(currentFusionType: DualCameraParams.FusionType) {
        this.mCurrentFusionType = currentFusionType
    }

    fun setDualUVCCamera(dualUVCCamera: DualUVCCamera) {
        this.dualUVCCamera = dualUVCCamera
    }

    // Dual camera support variables
    private var mCurrentFusionType: DualCameraParams.FusionType? = null
    private var remapTempData: ByteArray? = null
    private var dualUVCCamera: DualUVCCamera? = null
    private var llTempData: ByteArray? = null

    override fun onFame(mixData: ByteArray, tempData: ByteArray, fpsText: Double) {
        if (Const.TYPE_IR_DUAL == productType) {
            when (mCurrentFusionType) {
                DualCameraParams.FusionType.IROnlyNoFusion -> {
                    if (remapTempData == null) {
                        remapTempData = ByteArray(Const.IR_WIDTH * Const.IR_HEIGHT * 2)
                    }
                    System.arraycopy(tempData, 0, remapTempData!!, 0, Const.IR_WIDTH * Const.IR_HEIGHT * 2)
                }
                else -> {
                    if (remapTempData == null) {
                        remapTempData = ByteArray(Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2)
                    }
                    System.arraycopy(tempData, 0, remapTempData!!, 0, Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2)
                }
            }
        }
    }
}