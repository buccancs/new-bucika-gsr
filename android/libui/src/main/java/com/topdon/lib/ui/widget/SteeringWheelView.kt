package com.topdon.lib.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import com.topdon.lib.ui.databinding.UiSteeringWheelViewBinding

class SteeringWheelView : LinearLayout, OnClickListener {

    private lateinit var binding: UiSteeringWheelViewBinding

    var listener: ((action: Int, moveX: Int) -> Unit)? = null
    var moveX = 30
    var rotationIR = 270
    set(value) {
        field = value
        if (value == 270 || value == 90){
            binding.tvConfirm.rotation = 270f
            rotation = 90f
        }else{
            binding.tvConfirm.rotation = 0f
            rotation = 0f
        }
        requestLayout()
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun initView() {
        binding = UiSteeringWheelViewBinding.inflate(LayoutInflater.from(context), this, true)
        binding.steeringWheelStartBtn.setOnClickListener(this)
        binding.steeringWheelCenterBtn.setOnClickListener(this)
        binding.steeringWheelEndBtn.setOnClickListener(this)
        if (rotationIR == 270 || rotationIR == 90){
            binding.tvConfirm.rotation = 270f
            rotation = 90f
        }else{
            binding.tvConfirm.rotation = 0f
            rotation = 0f
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.steeringWheelStartBtn -> {
                moveX += 10
                if (moveX > 60) {
                    moveX = 60
                }
                listener?.invoke(-1, moveX)
            }
            binding.steeringWheelCenterBtn -> {
                listener?.invoke(0, moveX)
            }
            binding.steeringWheelEndBtn -> {
                moveX -= 10
                if (moveX < -20) {
                    moveX = -20
                }
                listener?.invoke(1, moveX)
            }
        }
    }
