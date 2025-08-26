package com.topdon.lib.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import com.topdon.lib.ui.databinding.UiWifiSteeringWheelViewBinding

class WifiSteeringWheelView : LinearLayout, OnClickListener {

    private lateinit var binding: UiWifiSteeringWheelViewBinding

    var listener: ((action: Int, moveX: Int,moveY:Int) -> Unit)? = null
    var moveX = 0
    var moveY = 0
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
        binding = UiWifiSteeringWheelViewBinding.inflate(LayoutInflater.from(context), this, true)
        binding.steeringWheelStartBtn.setOnClickListener(this)
        binding.steeringWheelCenterBtn.setOnClickListener(this)
        binding.steeringWheelEndBtn.setOnClickListener(this)
        binding.steeringWheelTopBtn.setOnClickListener(this)
        binding.steeringWheelBottomBtn.setOnClickListener(this)
        if (rotationIR == 270 || rotationIR == 90){
            binding.tvConfirm.rotation = 270f
            rotation = 90f
        }else{
            binding.tvConfirm.rotation = 0f
            rotation = 0f
        }
    }

    val moveI = 2
    override fun onClick(v: View?) {
        when (v) {
            binding.steeringWheelStartBtn -> {

                listener?.invoke(-1, moveX,moveY)
            }
            binding.steeringWheelCenterBtn -> {
                listener?.invoke(0, moveX,moveY)
            }
            binding.steeringWheelTopBtn -> {

                listener?.invoke(2, moveX,moveY)
            }
            binding.steeringWheelBottomBtn ->{

                listener?.invoke(3, moveX,moveY)
            }
            binding.steeringWheelEndBtn -> {

                listener?.invoke(1,moveX,moveY)
            }
        }
    }
