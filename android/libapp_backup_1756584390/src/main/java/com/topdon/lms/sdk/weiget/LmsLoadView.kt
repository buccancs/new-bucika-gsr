package com.topdon.lms.sdk.weiget

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar

/**
 * Stub implementation of LmsLoadView for compilation
 * This is a custom loading view widget
 */
class LmsLoadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ProgressBar(context, attrs, defStyleAttr) {
    
    init {
        // Stub initialization - in real app module, this would be handled by actual LMS
    }
    
    fun startAnimation() {
        // Stub method
    }
    
    fun stopAnimation() {
        // Stub method
    }
    
    override fun setProgress(progress: Int) {
        // Stub method
        super.setProgress(progress)
    }
}