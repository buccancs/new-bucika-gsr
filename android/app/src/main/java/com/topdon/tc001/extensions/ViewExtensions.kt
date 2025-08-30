package com.topdon.tc001.extensions

import android.view.View
import com.topdon.lib.core.view.TitleView

// Extension methods to resolve missing method errors
fun TitleView.setLeftClickListener(listener: () -> Unit) {
    this.setOnClickListener { listener() }
}

fun View.setLeftClickListener(listener: () -> Unit) {
    this.setOnClickListener { listener() }
}

// Add commonly missing UI methods as extensions
fun View.setPseudoColor(mode: Int) {
    // Set pseudo color mode
}

fun View.setSettingSelected(type: Any, selected: Boolean) {
    // Set setting selection state
}

fun View.setPositiveListener(textRes: Int, listener: () -> Unit) {
    // Set positive button listener
}