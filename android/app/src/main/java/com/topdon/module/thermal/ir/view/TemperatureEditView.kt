package com.topdon.module.thermal.ir.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.infisense.usbir.view.ITsTempListener

class TemperatureEditView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    var isShowName: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    
    var isShowFull: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    
    var mode: Mode = Mode.CLEAR
        set(value) {
            field = value
            invalidate()
        }
    
    private var tempListener: ITsTempListener? = null
    
    fun setITsTempListener(listener: ITsTempListener?) {
        tempListener = listener
    }
    
    fun setImageSize(width: Int, height: Int) {
        // Set image dimensions for thermal processing
        invalidate()
    }
    
    enum class Mode {
        CLEAR, EDIT, POINT, LINE, RECT
    }
}