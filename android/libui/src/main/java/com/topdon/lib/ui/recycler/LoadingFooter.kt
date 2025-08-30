package com.topdon.lib.ui.recycler

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.topdon.lib.ui.R

class LoadingFooter : LinearLayout {

    private lateinit var llLoading: LinearLayout
    private lateinit var clLoadEnd: ConstraintLayout

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0) {
        inflate(context, R.layout.ui_footer_view, this)
        initViews()
    }

    private fun initViews() {
        llLoading = findViewById(R.id.ll_loading)
        clLoadEnd = findViewById(R.id.cl_load_end)
    }

    fun setNoMoreData(noMoreData: Boolean): Boolean {
        llLoading.isVisible = !noMoreData
        clLoadEnd.isVisible = noMoreData
        return true
    }

    fun showLoading() {
        llLoading.isVisible = true
        clLoadEnd.isVisible = false
    }

    fun showLoadEnd() {
        llLoading.isVisible = false
        clLoadEnd.isVisible = true
    }

    fun hide() {
        llLoading.isVisible = false
        clLoadEnd.isVisible = false
    }
}