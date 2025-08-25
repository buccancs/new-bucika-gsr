package com.topdon.lib.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import com.topdon.lib.ui.databinding.UiWifiSteeringWheelViewBinding

/**
 * Professional WiFi steering wheel control component for thermal imaging systems
 * 
 * Provides five-directional calibration controls including:
 * - Five-point steering control (left, right, up, down, center)
 * - Rotation-aware UI with dynamic orientation support
 * - Professional parameter adjustment with comprehensive movement support
 * - Multi-directional interaction with callback support
 * 
 * @property listener Callback for steering wheel actions with X/Y movement parameters
 * @property moveX Current horizontal movement value
 * @property moveY Current vertical movement value
 * @property rotationIR IR rotation value affecting UI orientation (270/90 for rotated mode)
 */
class WifiSteeringWheelView : LinearLayout, OnClickListener {

    /**
     * ViewBinding instance for type-safe view access
     */
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

    /**
     * Initialize the WiFi steering wheel layout and binding
     */
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
//                moveY -= moveI
                listener?.invoke(-1, moveX,moveY)
            }
            binding.steeringWheelCenterBtn -> {
                listener?.invoke(0, moveX,moveY)
            }
            binding.steeringWheelTopBtn -> {
//                moveX += moveI
                listener?.invoke(2, moveX,moveY)
            }
            binding.steeringWheelBottomBtn ->{
//                moveX -= moveI
                listener?.invoke(3, moveX,moveY)
            }
            binding.steeringWheelEndBtn -> {
//                moveY += moveI
                listener?.invoke(1,moveX,moveY)
            }
        }
    }
