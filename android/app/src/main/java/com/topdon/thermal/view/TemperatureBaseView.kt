package com.topdon.thermal.view

import android.view.View

open class TemperatureBaseView : View {
    constructor(context: android.content.Context) : super(context)
    constructor(context: android.content.Context, attrs: android.util.AttributeSet?) : super(context, attrs)
    
    enum class Mode {
        NORMAL, EDIT, CLEAR, POINT, LINE, RECT
    }
}