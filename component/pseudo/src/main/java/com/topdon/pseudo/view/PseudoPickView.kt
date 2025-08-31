package com.topdon.pseudo.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.SizeUtils
import com.topdon.pseudo.R
import kotlin.math.abs

/**
 * 自定义伪彩设置页面中，那个支持最多 7 个圆形色块滑来滑去的 View.
 *
 * 提供方法：
 * - [reset] 将当前状态重置为指定颜色值及位置
 * - [refreshColor] 将当前选中的圆形色块设置为指定颜色
 * - [add] 添加一个圆形色块
 * - [del] 删除当前选中圆形色块
 * - [isCurrentOnlyLimit] 判断当前选中圆形色块是不是：(最左 || 最右) && 唯一
 *
 * Created by LCG on 2024/10/15.
 */
class PseudoPickView : View {
    companion object {
        @CheckResult
        private fun IntArray.add(index: Int, element: Int): IntArray {
            val newArray = IntArray(this.size + 1)
            System.arraycopy(this, 0, newArray, 0, index)
            newArray[index] = element
            System.arraycopy(this, index, newArray, index + 1, this.size - index)
            return newArray
        }
        @CheckResult
        private fun FloatArray.add(index: Int, element: Float): FloatArray {
            val newArray = FloatArray(this.size + 1)
            System.arraycopy(this, 0, newArray, 0, index)
            newArray[index] = element
            System.arraycopy(this, index, newArray, index + 1, this.size - index)
            return newArray
        }
        @CheckResult
        private fun IntArray.removeAt(index: Int): IntArray {
            val newArray = IntArray(this.size - 1)
            System.arraycopy(this, 0, newArray, 0, index)
            System.arraycopy(this, index + 1, newArray, index, this.size - index - 1)
            return newArray
        }
        @CheckResult
        private fun FloatArray.removeAt(index: Int): FloatArray {
            val newArray = FloatArray(this.size - 1)
            System.arraycopy(this, 0, newArray, 0, index)
            System.arraycopy(this, index + 1, newArray, index, this.size - index - 1)
            return newArray
        }
    }

    /**
     * 绘制渐变条所用的 Paint.
     */
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    /**
     * 绘制渐变条下面圆形色块所用的 Pint.
     */
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 圆形色块选中时三角形 Drawable.
     */
    private val selectYesDrawable: Drawable
    /**
     * 圆形色块未选中时三角形 Drawable.
     */
    private val selectNotDrawable: Drawable


    /**
     * 选中色块变更事件监听.
     */
    var onSelectChangeListener: ((selectIndex: Int) -> Unit)? = null


    /**
     * 当前选中的圆形色块在列表中的 index.
     */
    var selectIndex = 0
    /**
     * 由于需求为完全重叠的多个圆形色块，只生效最上方的圆形色块，该数组保存原始的颜色数组.
     * 按 place 排序，若 place 相同则 zAltitude 越大的越靠后.
     * size 与 [actualColors]、[zAltitudes]、[places] 一致。
     */
    var sourceColors: IntArray = intArrayOf(0xff0000ff.toInt(), 0xffff0000.toInt(), 0xffffff00.toInt())
    /**
     * 由于需求为完全重叠的多个圆形色块，只生效最上方的圆形色块，该数组保存实际生效的颜色数组.
     */
    var actualColors: IntArray = intArrayOf(0xff0000ff.toInt(), 0xffff0000.toInt(), 0xffffff00.toInt())
    /**
     * 每个圆形色块对应的 z 轴海拔数组，用来在重叠时判断哪个圆形色块在上面。
     */
    var zAltitudes: IntArray = intArrayOf(0, 0, 0)
    /**
     * 每个圆形色块对应的位置数组.
     */
    var places: FloatArray = floatArrayOf(0f, 0.5f, 1f)


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes:Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        selectYesDrawable = ContextCompat.getDrawable(context, R.drawable.svg_pseudo_triangle_select)!!
        selectNotDrawable = ContextCompat.getDrawable(context, R.drawable.svg_pseudo_triangle_not_select)!!
        selectYesDrawable.setBounds(0, 0, SizeUtils.dp2px(16f), SizeUtils.dp2px(10f))
        selectNotDrawable.setBounds(0, 0, SizeUtils.dp2px(16f), SizeUtils.dp2px(10f))
    }

    /**
     * 将当前状态重置为指定颜色值及位置的配置.
     * @param selectIndex 当前选中的圆形色块 index
     * @param colors 每个圆形色块颜色数组
     * @param zAltitudes 每个圆形色块对应的 z 轴海拔数组
     * @param places 每个圆形色块对应的位置数组
     */
    fun reset(selectIndex: Int, colors: IntArray, zAltitudes: IntArray, places: FloatArray) {
        this.selectIndex = selectIndex
        this.sourceColors = colors
        this.zAltitudes = zAltitudes
        this.places = places
        refreshActualColors()
        barPaint.shader = LinearGradient(barRect.left, 0f, barRect.right, 0f, actualColors, places, Shader.TileMode.CLAMP)
        invalidate()
        onSelectChangeListener?.invoke(selectIndex)
    }

    /**
     * 将当前选中的圆颜色值设置为指定颜色
     */
    fun refreshColor(@ColorInt color: Int) {
        sourceColors[selectIndex] = color
        actualColors[selectIndex] = color
        refreshActualColors()
        barPaint.shader = LinearGradient(barRect.left, 0f, barRect.right, 0f, actualColors, places, Shader.TileMode.CLAMP)
        invalidate()
    }

    /**
     * 需求要添加时颜色按 绿、黑、白、紫 循环，用该变量控制.
     */
    private var addCount = 0
    /**
     * 添加一个圆形色块
     */
    fun add() {
        if (sourceColors.size >= 7) {//最多7个圆形色块
            return
        }
        addCount++
        if (addCount > 4) {
            addCount = 1
        }
        val addColor: Int = when (addCount) {
            1 -> 0xff00ff00.toInt()
            2 -> 0xff000000.toInt()
            3 -> 0xffffffff.toInt()
            else -> 0xff982abc.toInt()
        }
        var addIndex = 0
        for (i in places.size - 1 downTo 1) {
            val place = places[i]
            if (place > 0.75f) {
                addIndex = i
            } else if (place < 0.75f) {
                break
            } else {
                addIndex = i + 1
                break
            }
        }

        sourceColors = sourceColors.add(addIndex, addColor)
        zAltitudes = zAltitudes.add(addIndex, calculateZAltitude(0.75f))
        places = places.add(addIndex, 0.75f)
        selectIndex = addIndex
        refreshActualColors()
        barPaint.shader = LinearGradient(barRect.left, 0f, barRect.right, 0f, actualColors, places, Shader.TileMode.CLAMP)
        invalidate()
        onSelectChangeListener?.invoke(selectIndex)
    }

    /**
     * 删除当前选中圆形色块.
     */
    fun del() {
        if (sourceColors.size <= 3) {
            return
        }
        if (isCurrentOnlyLimit()) {//仅有的最左最右不允许删除
            return
        }

        sourceColors = sourceColors.removeAt(selectIndex)
        zAltitudes = zAltitudes.removeAt(selectIndex)
        places = places.removeAt(selectIndex)
        selectIndex = 0
        for (i in zAltitudes.indices) {
            if (zAltitudes[i] >= zAltitudes[selectIndex]) {
                selectIndex = i
            }
        }
        refreshActualColors()
        barPaint.shader = LinearGradient(barRect.left, 0f, barRect.right, 0f, actualColors, places, Shader.TileMode.CLAMP)
        invalidate()
        onSelectChangeListener?.invoke(selectIndex)
    }

    /**
     * 判断当前选中圆形色块是不是：(最左 || 最右) && 唯一
     */
    fun isCurrentOnlyLimit(): Boolean {
        val place: Float = places[selectIndex]
        if (place == 0f || place == 1f) {//是最左或最右，接下来看看是不是唯一
            for (i in places.indices) {
                if (i != selectIndex && places[i] == place) {
                    return false
                }
            }
            return true
        }
        return false
    }

    /**
     * 当任意圆形色块颜色、位置、z 轴高度变更时，刷新实际生效的颜色数组.
     */
    private fun refreshActualColors() {
        if (actualColors.size != sourceColors.size) {
            actualColors = IntArray(sourceColors.size)
        }
        System.arraycopy(sourceColors, 0, actualColors, 0, sourceColors.size)
        for (i in places.size - 1 downTo 1) {
            if (places[i - 1] == places[i]) {
                actualColors[i - 1] = actualColors[i]
            }
        }
    }

    /**
     * 根据指定的 place 计算对应的 ZAltitude.
     */
    private fun calculateZAltitude(place: Float): Int {
        var result = 0
        val gap: Float = selectRadius * 2 / barRect.width()
        for (i in places.indices) {
            if (abs(places[i] - place) <= gap) {
                result = result.coerceAtLeast(zAltitudes[i] + 1)
            }
        }
        return result
    }




    /**
     * 渐变条 Rect.
     */
    private val barRect = RectF()
    /**
     * 渐变条下面圆形色块选中时半径，单位 px.
     */
    private val selectRadius: Int = SizeUtils.dp2px(12f)

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize: Int = MeasureSpec.getSize(widthMeasureSpec)
        barRect.set(selectRadius.toFloat(), 0f, (widthSize - selectRadius).toFloat(), ((widthSize - selectRadius * 2) * 30 / 311f).toInt().toFloat())
        barPaint.shader = LinearGradient(barRect.left, 0f, barRect.right, 0f, actualColors, places, Shader.TileMode.CLAMP)

        // 2dp 为渐变条与三角形间距
        val wantHeight: Int = barRect.height().toInt() + SizeUtils.dp2px(2f) + selectNotDrawable.bounds.height() + selectRadius * 2

        //宽度为 UNSPECIFIED 的情况目前不存在，不考虑；高度不为 wrap_content 的情况也不存在，不考虑
        setMeasuredDimension(widthSize, wantHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //绘制伪彩条
        val barRadius = SizeUtils.dp2px(4f).toFloat()
        canvas.drawRoundRect(barRect.left, 0f, barRect.right, barRect.bottom, barRadius, barRadius, barPaint)

        canvas.translate(0f, barRect.bottom + SizeUtils.dp2px(2f))
        val strokeWidth: Float = SizeUtils.dp2px(1.5f).toFloat()
        val circleRadius: Float = (selectRadius - strokeWidth * 2).toInt().toFloat()

        var minZAltitude = 0
        var maxZAltitude = 0
        for (altitude in zAltitudes) {
            minZAltitude = minZAltitude.coerceAtMost(altitude)
            maxZAltitude = maxZAltitude.coerceAtLeast(altitude)
        }
        for (altitude in minZAltitude .. maxZAltitude) {
            for (i in zAltitudes.indices) {
                if (zAltitudes[i] == altitude) {
                    val x: Float = barRect.left + barRect.width() * places[i]
                    val y: Float = (selectNotDrawable.bounds.height() + selectRadius).toFloat()
                    if (i == selectIndex) {
                        circlePaint.color = 0xffffffff.toInt()
                        canvas.drawCircle(x, y, selectRadius.toFloat(), circlePaint)
                        circlePaint.color = 0xff16131e.toInt()
                        canvas.drawCircle(x, y, selectRadius - strokeWidth, circlePaint)
                    }

                    circlePaint.color = actualColors[i]
                    canvas.drawCircle(x, y, circleRadius, circlePaint)

                    canvas.save()
                    canvas.translate(x - selectNotDrawable.bounds.width() / 2, 0f)
                    (if (i == selectIndex) selectYesDrawable else selectNotDrawable).draw(canvas)
                    canvas.restore()
                }
            }
        }
    }


    /**
     * Touch Down 时 x 轴坐标，用于计算滑动距离，从而判断是否触发滑动。
     */
    private var downX = 0
    /**
     * 是否需要接手 Touch 事件.
     */
    private var handleTouch = false
    /**
     * 当前选中的滑块是否可拖动，唯一的最左或最右不可滑动。
     */
    private var canDrag = false
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleTouch = false
                canDrag = false
                downX = event.x.toInt()

                //找出点击范围内海拔最高的圆形色块 index
                var targetIndex = -1
                for (i in places.indices) {
                    val centerX: Int = (barRect.left + barRect.width() * places[i]).toInt()
                    if (downX >= centerX - selectRadius && downX <= centerX + selectRadius) {//在该圆形色块范围内
                        if (targetIndex == -1) {
                            targetIndex = i
                            continue
                        }
                        if (zAltitudes[i] >= zAltitudes[targetIndex]) {
                            targetIndex = i
                        }
                    }
                }
                if (targetIndex >= 0) {
                    zAltitudes[targetIndex] = calculateZAltitude(places[targetIndex])
                    selectIndex = targetIndex
                    invalidate()
                    handleTouch = true
                    canDrag = !isCurrentOnlyLimit()
                    onSelectChangeListener?.invoke(selectIndex)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x.coerceAtLeast(barRect.left).coerceAtMost(barRect.right).toInt()
                if (canDrag) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    val oldPlace: Float = places[selectIndex]
                    val newPlace: Float = (x - barRect.left) / barRect.width()
                    if (newPlace == oldPlace) {//没变化，不用往下处理了
                        return handleTouch
                    }
                    val currentColor: Int = sourceColors[selectIndex]
                    val oldIndex: Int = selectIndex
                    var newIndex: Int = selectIndex
                    if (oldPlace < newPlace) {//从左往右移
                        for (i in places.indices) {
                            if (places[i] <= newPlace) {
                                newIndex = i
                            } else {
                                break
                            }
                        }
                    } else {//从右往左移
                        for (i in places.size - 1 downTo 0) {
                            val place = places[i]
                            if (place > newPlace) {
                                newIndex = i
                            } else if (place < newPlace) {
                                break
                            } else {
                                newIndex = i + 1
                                break
                            }
                        }
                    }
                    if (newIndex < oldIndex) {
                        System.arraycopy(sourceColors, newIndex, sourceColors, newIndex + 1, oldIndex - newIndex)
                        System.arraycopy(zAltitudes, newIndex, zAltitudes, newIndex + 1, oldIndex - newIndex)
                        System.arraycopy(places, newIndex, places, newIndex + 1, oldIndex - newIndex)
                        selectIndex = newIndex
                        sourceColors[newIndex] = currentColor
                    } else if (newIndex > oldIndex) {
                        System.arraycopy(sourceColors, oldIndex + 1, sourceColors, oldIndex, newIndex - oldIndex)
                        System.arraycopy(zAltitudes, oldIndex + 1, zAltitudes, oldIndex, newIndex - oldIndex)
                        System.arraycopy(places, oldIndex + 1, places, oldIndex, newIndex - oldIndex)
                        selectIndex = newIndex
                        sourceColors[newIndex] = currentColor
                    }
                    places[newIndex] = newPlace
                    zAltitudes[newIndex] = calculateZAltitude(newPlace)
                    refreshActualColors()
                    barPaint.shader = LinearGradient(barRect.left, 0f, barRect.right, 0f, actualColors, places, Shader.TileMode.CLAMP)
                    invalidate()
                }
            }
        }
        return handleTouch
    }
}