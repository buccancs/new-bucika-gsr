package com.topdon.thermal.popup

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.widget.PopupWindow
import com.blankj.utilcode.util.SizeUtils
import com.topdon.thermal.R
import com.topdon.thermal.databinding.PopupGalleryChangeBinding

class GalleryChangePopup(private val context: Context) : PopupWindow() {

    private val binding: PopupGalleryChangeBinding = PopupGalleryChangeBinding.inflate(
        LayoutInflater.from(context)
    )

    var onPickListener: ((position: Int, str: String) -> Unit)? = null

    init {
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(
            (context.resources.displayMetrics.widthPixels * 0.6).toInt(), 
            MeasureSpec.EXACTLY
        )
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(
            context.resources.displayMetrics.heightPixels, 
            MeasureSpec.AT_MOST
        )
        
        contentView = binding.root
        contentView.measure(widthMeasureSpec, heightMeasureSpec)

        width = contentView.measuredWidth
        height = contentView.measuredHeight
        isOutsideTouchable = true

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.tvLine.setOnClickListener {
            dismiss()
            onPickListener?.invoke(0, context.getString(R.string.tc_has_line_device))
        }
        
        binding.tvTs004.setOnClickListener {
            dismiss()
            onPickListener?.invoke(1, "TS004")
        }
        
        binding.tvTc007.setOnClickListener {
            dismiss()
            onPickListener?.invoke(2, "TC007")
        }
    }

    fun show(anchor: View) {
        val locationArray = IntArray(2)
        anchor.getLocationInWindow(locationArray)

        val x = locationArray[0] + anchor.width / 2 - width / 2
        val y = locationArray[1] + anchor.height - SizeUtils.dp2px(5f)
        showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
    }
