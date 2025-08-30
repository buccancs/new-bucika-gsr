package com.topdon.module.thermal.adapter

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ScreenUtils

class MenuRecyclerView : RecyclerView {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun initType(type: Int) {
        val span = when (type) {
            1 -> 2
            2 -> 6
            4 -> 4
            else -> 4
        }
        if (span == 2) {
            setPadding(
                (ScreenUtils.getAppScreenWidth() / 3.5f).toInt(),
                0,
                (ScreenUtils.getAppScreenWidth() / 3.5f).toInt(),
                0
            )
        } else {
            setPadding(0, 0, 0, 0)
        }
        layoutManager = if (type == 3) {
            LinearLayoutManager(context, HORIZONTAL, false)
        } else {
            GridLayoutManager(context, span)
        }
        val menuTabAdapter = adapter
        if (menuTabAdapter is MenuTabAdapter) {
            (menuTabAdapter as MenuTabAdapter).initType(type)
        }
    }
}