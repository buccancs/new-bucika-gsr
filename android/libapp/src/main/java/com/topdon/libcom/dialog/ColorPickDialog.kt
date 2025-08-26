package com.topdon.libcom.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorInt
import com.topdon.lib.core.R

class ColorPickDialog(context: Context, @ColorInt private var color: Int, var textSize: Int, var textSizeIsDP: Boolean = false) : Dialog(context, R.style.InfoDialog), View.OnClickListener {

    var onPickListener: ((color: Int, textSize: Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        // Simplified implementation - just close dialog
    }

    override fun onClick(v: View?) {
        dismiss()
        onPickListener?.invoke(color, textSize)
    }
}