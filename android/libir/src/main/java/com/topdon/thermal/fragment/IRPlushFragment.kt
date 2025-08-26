package com.topdon.thermal.fragment

import android.graphics.Bitmap
import android.view.SurfaceView
import android.view.View
import com.infisense.usbdual.Const
import com.infisense.usbdual.camera.DualViewWithExternalCameraCommonApi
import com.infisense.usbir.view.TemperatureView
import com.topdon.thermal.R
import com.topdon.thermal.activity.BaseIRPlushFragment
import com.topdon.thermal.databinding.FragmentIrPlushBinding

class IRPlushFragment : BaseIRPlushFragment() {

    private lateinit var binding: FragmentIrPlushBinding

    override fun getSurfaceView(): SurfaceView {
        return binding.dualTextureViewNativeCamera
    }

    override fun getTemperatureDualView(): TemperatureView {
        return binding.temperatureView
    }

    override suspend fun onDualViewCreate(dualView: DualViewWithExternalCameraCommonApi?) {

    }

    override fun isDualIR(): Boolean = true

    override fun setTemperatureViewType() {
        getTemperatureDualView().productType = Const.TYPE_IR_DUAL
    }

    override fun initContentView(): Int {
        binding = FragmentIrPlushBinding.inflate(layoutInflater)
        return R.layout.fragment_ir_plush
    }

    override fun initData() {

    }

    override fun initView() {
        super.initView()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun getBitmap(): Bitmap? = dualView?.scaledBitmap
