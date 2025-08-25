package com.jaygoo.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.*
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import java.text.DecimalFormat

class SeekBar(
    private val defRangeSeekBar: DefRangeSeekBar,
    attrs: AttributeSet?,
    private val isLeft: Boolean
) {
    
    companion object {
        const val INDICATOR_SHOW_WHEN_TOUCH = 0
        const val INDICATOR_ALWAYS_HIDE = 1
        const val INDICATOR_ALWAYS_SHOW_AFTER_TOUCH = 2
        const val INDICATOR_ALWAYS_SHOW = 3
        const val WRAP_CONTENT = -1
        const val MATCH_PARENT = -2
    }
    
    @IntDef(INDICATOR_SHOW_WHEN_TOUCH, INDICATOR_ALWAYS_HIDE, INDICATOR_ALWAYS_SHOW_AFTER_TOUCH, INDICATOR_ALWAYS_SHOW)
    @Retention(AnnotationRetention.SOURCE)
    annotation class IndicatorModeDef
    
    private var stepsPaddingLeft: Float = 0f
    private var stepsPaddingRight: Float = 0f
    private var indicatorShowMode = INDICATOR_ALWAYS_HIDE
    private var indicatorHeight = 0
    private var indicatorWidth = 0
    private var indicatorMargin = 0
    private var indicatorDrawableId = 0
    private var indicatorArrowSize = 0
    private var indicatorTextSize = 0
    private var indicatorTextColor = 0
    private var indicatorRadius = 0f
    private var indicatorBackgroundColor = 0
    private var indicatorPaddingLeft = 0
    private var indicatorPaddingRight = 0
    private var indicatorPaddingTop = 0
    private var indicatorPaddingBottom = 0
    private var thumbDrawableId = 0
    private var thumbInactivatedDrawableId = 0
    private var thumbWidth = 0
    private var thumbHeight = 0
    
    var thumbScaleRatio = 1f
    var left = 0
    var right = 0
    var top = 0
    var bottom = 0
    var currPercent = 0f
    var material = 0f
    private var isShowIndicator = false
    var thumbBitmap: Bitmap? = null
    var thumbInactivatedBitmap: Bitmap? = null
    var indicatorBitmap: Bitmap? = null
    var anim: ValueAnimator? = null
    var userText2Draw: String? = null
    var isActivate = false
    var isVisible = true
    var indicatorTextStringFormat: String? = null
    private val indicatorArrowPath = Path()
    private val indicatorTextRect = Rect()
    private val indicatorRect = Rect()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var indicatorTextDecimalFormat: DecimalFormat? = null
    var scaleThumbWidth = 0
    var scaleThumbHeight = 0
    
    init {
        initAttrs(attrs)
        initBitmap()
        initVariables()
    }
    
    private fun initAttrs(attrs: AttributeSet?) {
        val t = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar) ?: return
        
        indicatorMargin = t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_margin, 0f).toInt()
        indicatorDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_indicator_drawable, 0)
        indicatorShowMode = t.getInt(R.styleable.RangeSeekBar_rsb_indicator_show_mode, INDICATOR_ALWAYS_HIDE)
        indicatorHeight = t.getLayoutDimension(R.styleable.RangeSeekBar_rsb_indicator_height, WRAP_CONTENT)
        indicatorWidth = t.getLayoutDimension(R.styleable.RangeSeekBar_rsb_indicator_width, WRAP_CONTENT)
        indicatorTextSize = t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_text_size, Utils.dp2px(getContext(), 14).toFloat()).toInt()
        indicatorTextColor = t.getColor(R.styleable.RangeSeekBar_rsb_indicator_text_color, Color.WHITE)
        indicatorBackgroundColor = t.getColor(R.styleable.RangeSeekBar_rsb_indicator_background_color, ContextCompat.getColor(getContext(), R.color.rsbColorIndicatorBg))
        indicatorPaddingLeft = t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_left, 0f).toInt()
        indicatorPaddingRight = t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_right, 0f).toInt()
        indicatorPaddingTop = t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_top, 0f).toInt()
        indicatorPaddingBottom = t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_padding_bottom, 0f).toInt()
        indicatorArrowSize = t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_arrow_size, 0f).toInt()
        thumbDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_thumb_drawable, R.drawable.rsb_default_thumb)
        thumbInactivatedDrawableId = t.getResourceId(R.styleable.RangeSeekBar_rsb_thumb_inactivated_drawable, 0)
        thumbWidth = t.getDimension(R.styleable.RangeSeekBar_rsb_thumb_width, Utils.dp2px(getContext(), 26).toFloat()).toInt()
        thumbHeight = t.getDimension(R.styleable.RangeSeekBar_rsb_thumb_height, Utils.dp2px(getContext(), 26).toFloat()).toInt()
        thumbScaleRatio = t.getFloat(R.styleable.RangeSeekBar_rsb_thumb_scale_ratio, 1f)
        indicatorRadius = t.getDimension(R.styleable.RangeSeekBar_rsb_indicator_radius, 0f)
        
        t.recycle()
    }
    
    protected fun initVariables() {
        scaleThumbWidth = thumbWidth
        scaleThumbHeight = thumbHeight
        if (indicatorHeight == WRAP_CONTENT) {
            indicatorHeight = Utils.measureText("8", indicatorTextSize).height() + indicatorPaddingTop + indicatorPaddingBottom
        }
        if (indicatorArrowSize <= 0) {
            indicatorArrowSize = thumbWidth / 4
        }
    }
    
    fun getContext(): Context = defRangeSeekBar.context
    
    fun getResources(): Resources? = getContext().resources
    
    private fun initBitmap() {
        setIndicatorDrawableId(indicatorDrawableId)
        setThumbDrawableId(thumbDrawableId, thumbWidth, thumbHeight)
        setThumbInactivatedDrawableId(thumbInactivatedDrawableId, thumbWidth, thumbHeight)
    }
    
    protected fun onSizeChanged(x: Int, y: Int) {
        initVariables()
        initBitmap()
        left = (x - getThumbScaleWidth() / 2).toInt()
        right = (x + getThumbScaleWidth() / 2).toInt()
        top = y - getThumbHeight() / 2
        bottom = y + getThumbHeight() / 2
    }
    
    fun scaleThumb() {
        scaleThumbWidth = getThumbScaleWidth().toInt()
        scaleThumbHeight = getThumbScaleHeight().toInt()
        val y = defRangeSeekBar.getProgressBottom()
        top = y - scaleThumbHeight / 2
        bottom = y + scaleThumbHeight / 2
        setThumbDrawableId(thumbDrawableId, scaleThumbWidth, scaleThumbHeight)
    }
    
    fun resetThumb() {
        scaleThumbWidth = getThumbWidth()
        scaleThumbHeight = getThumbHeight()
        val y = defRangeSeekBar.getProgressBottom()
        top = y - scaleThumbHeight / 2
        bottom = y + scaleThumbHeight / 2
        setThumbDrawableId(thumbDrawableId, scaleThumbWidth, scaleThumbHeight)
    }
    
    fun getRawHeight(): Float {
        return getIndicatorHeight() + getIndicatorArrowSize() + getIndicatorMargin() + getThumbScaleHeight()
    }
    
    protected fun draw(canvas: Canvas) {
        if (!isVisible) return
        
        val offset = (defRangeSeekBar.getProgressWidth() * currPercent).toInt()
        canvas.save()
        canvas.translate(offset.toFloat(), 0f)
        
        canvas.translate(left.toFloat(), 0f)
        when {
            currPercent <= 0 -> canvas.translate(defRangeSeekBar.stepsPaddingLeft, 0f)
            currPercent >= 1 -> canvas.translate(-defRangeSeekBar.stepsPaddingRight, 0f)
        }
        
        if (isShowIndicator) {
            onDrawIndicator(canvas, paint, formatCurrentIndicatorText(userText2Draw))
        }
        onDrawThumb(canvas)
        canvas.restore()
    }
    
    protected fun onDrawThumb(canvas: Canvas) {
        val yPosition = defRangeSeekBar.getProgressTop() + (defRangeSeekBar.getProgressHeight() - scaleThumbHeight) / 2f
        
        when {
            thumbInactivatedBitmap != null && !isActivate -> 
                canvas.drawBitmap(thumbInactivatedBitmap!!, 0f, yPosition, null)
            thumbBitmap != null -> 
                canvas.drawBitmap(thumbBitmap!!, 0f, yPosition, null)
        }
    }
    
    protected fun formatCurrentIndicatorText(text2Draw: String?): String? {
        val states = defRangeSeekBar.getRangeSeekBarState()
        var formattedText = text2Draw
        
        if (TextUtils.isEmpty(formattedText)) {
            formattedText = if (isLeft) {
                indicatorTextDecimalFormat?.format(states[0].value) ?: states[0].indicatorText
            } else {
                indicatorTextDecimalFormat?.format(states[1].value) ?: states[1].indicatorText
            }
        }
        
        indicatorTextStringFormat?.let { format ->
            formattedText = String.format(format, formattedText)
        }
        
        return formattedText
    }
    
    protected fun onDrawIndicator(canvas: Canvas, paint: Paint, text2Draw: String?) {
        text2Draw ?: return
        
        paint.apply {
            textSize = indicatorTextSize.toFloat()
            style = Paint.Style.FILL
            color = indicatorBackgroundColor
        }
        
        paint.getTextBounds(text2Draw, 0, text2Draw.length, indicatorTextRect)
        
        var realIndicatorWidth = indicatorTextRect.width() + indicatorPaddingLeft + indicatorPaddingRight
        if (indicatorWidth > realIndicatorWidth) {
            realIndicatorWidth = indicatorWidth
        }
        
        var realIndicatorHeight = indicatorTextRect.height() + indicatorPaddingTop + indicatorPaddingBottom
        if (indicatorHeight > realIndicatorHeight) {
            realIndicatorHeight = indicatorHeight
        }
        
        indicatorRect.apply {
            left = (scaleThumbWidth / 2f - realIndicatorWidth / 2f).toInt()
            top = bottom - realIndicatorHeight - scaleThumbHeight - indicatorMargin
            right = left + realIndicatorWidth
            bottom = top + realIndicatorHeight
        }
        
        if (indicatorBitmap == null) {
            drawIndicatorBackground(canvas, paint, realIndicatorWidth, realIndicatorHeight)
        } else {
            canvas.drawBitmap(indicatorBitmap!!, indicatorRect.left.toFloat(), indicatorRect.top.toFloat(), paint)
        }
        
        drawIndicatorText(canvas, paint, text2Draw, realIndicatorWidth, realIndicatorHeight)
    }
    
    private fun drawIndicatorBackground(canvas: Canvas, paint: Paint, width: Int, height: Int) {
        if (indicatorRadius > 0) {
            val rectF = RectF(indicatorRect)
            canvas.drawRoundRect(rectF, indicatorRadius, indicatorRadius, paint)
        } else {
            canvas.drawRect(indicatorRect, paint)
        }
        
        // Draw arrow
        if (indicatorArrowSize > 0) {
            indicatorArrowPath.reset()
            val arrowCenterX = indicatorRect.left + width / 2f
            val arrowTopY = indicatorRect.bottom.toFloat()
            val arrowBottomY = arrowTopY + indicatorArrowSize
            
            indicatorArrowPath.moveTo(arrowCenterX - indicatorArrowSize / 2f, arrowTopY)
            indicatorArrowPath.lineTo(arrowCenterX + indicatorArrowSize / 2f, arrowTopY)
            indicatorArrowPath.lineTo(arrowCenterX, arrowBottomY)
            indicatorArrowPath.close()
            
            canvas.drawPath(indicatorArrowPath, paint)
        }
    }
    
    private fun drawIndicatorText(canvas: Canvas, paint: Paint, text: String, width: Int, height: Int) {
        paint.color = indicatorTextColor
        val textX = indicatorRect.left + (width - indicatorTextRect.width()) / 2f - indicatorTextRect.left
        val textY = indicatorRect.top + (height - indicatorTextRect.height()) / 2f - indicatorTextRect.top
        canvas.drawText(text, textX, textY, paint)
    }
    
    // Utility methods
    fun getThumbWidth(): Int = thumbWidth
    fun getThumbHeight(): Int = thumbHeight
    fun getThumbScaleWidth(): Float = thumbWidth * thumbScaleRatio
    fun getThumbScaleHeight(): Float = thumbHeight * thumbScaleRatio
    fun getIndicatorHeight(): Int = indicatorHeight
    fun getIndicatorArrowSize(): Int = indicatorArrowSize
    fun getIndicatorMargin(): Int = indicatorMargin
    
    // Setter methods for customization
    fun setIndicatorDrawableId(@DrawableRes drawableId: Int) {
        if (drawableId <= 0) {
            indicatorBitmap = null
            return
        }
        
        val drawable = ContextCompat.getDrawable(getContext(), drawableId) ?: return
        indicatorBitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, 
            drawable.intrinsicHeight, 
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(indicatorBitmap!!)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
    }
    
    fun setThumbDrawableId(@DrawableRes drawableId: Int, width: Int, height: Int) {
        if (drawableId <= 0) return
        
        val drawable = ContextCompat.getDrawable(getContext(), drawableId) ?: return
        thumbBitmap = Bitmap.createScaledBitmap(
            drawable.toBitmap(),
            width,
            height,
            false
        )
    }
    
    fun setThumbInactivatedDrawableId(@DrawableRes drawableId: Int, width: Int, height: Int) {
        if (drawableId <= 0) return
        
        val drawable = ContextCompat.getDrawable(getContext(), drawableId) ?: return
        thumbInactivatedBitmap = Bitmap.createScaledBitmap(
            drawable.toBitmap(),
            width,
            height,
            false
        )
    }
    
    // Animation support
    fun showIndicator(isAnimate: Boolean = true) {
        if (indicatorShowMode == INDICATOR_ALWAYS_HIDE) return
        
        if (isAnimate) {
            startShowIndicatorAnimation()
        } else {
            isShowIndicator = true
        }
    }
    
    fun hideIndicator(isAnimate: Boolean = true) {
        if (indicatorShowMode == INDICATOR_ALWAYS_SHOW) return
        
        if (isAnimate) {
            startHideIndicatorAnimation()
        } else {
            isShowIndicator = false
        }
    }
    
    private fun startShowIndicatorAnimation() {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                // Apply animation transformations
                defRangeSeekBar.invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isShowIndicator = true
                }
            })
            start()
        }
    }
    
    private fun startHideIndicatorAnimation() {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 200
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                // Apply animation transformations
                defRangeSeekBar.invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isShowIndicator = false
                }
            })
            start()
        }
    }
}