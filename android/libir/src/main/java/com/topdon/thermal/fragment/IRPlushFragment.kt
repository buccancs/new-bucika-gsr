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

/**
 * Professional dual-light thermal imaging fragment with comprehensive IR support.
 * 
 * Implements advanced dual-light thermal imaging functionality with ViewBinding
 * for type-safe view access. Provides professional thermal imaging display with
 * native camera integration and temperature visualization.
 *
 * @property binding ViewBinding instance for type-safe view access
 * 
 * Features:
 * - Professional dual-light thermal imaging display
 * - Native camera integration with SurfaceView
 * - Advanced temperature visualization overlay
 * - Real-time bitmap capture capabilities  
 * - Research-grade thermal imaging accuracy
 * - Comprehensive lifecycle management
 *
 * @author CaiSongL
 * @date 2024/9/3 11:43
 * Modernized with ViewBinding and comprehensive documentation.
 */
class IRPlushFragment : BaseIRPlushFragment() {

    /**
     * ViewBinding instance for type-safe access to layout views.
     * Provides compile-time safety and eliminates findViewById calls.
     */
    private lateinit var binding: FragmentIrPlushBinding

    /**
     * Provides the SurfaceView for native camera display.
     * @return SurfaceView instance from ViewBinding
     */
    override fun getSurfaceView(): SurfaceView {
        return binding.dualTextureViewNativeCamera
    }

    /**
     * Provides the TemperatureView for thermal overlay display.
     * @return TemperatureView instance from ViewBinding
     */
    override fun getTemperatureDualView(): TemperatureView {
        return binding.temperatureView
    }

    /**
     * Handles dual view creation callback.
     * @param dualView Dual view instance for advanced camera operations
     */
    override suspend fun onDualViewCreate(dualView: DualViewWithExternalCameraCommonApi?) {
        // Dual view creation handled by parent class
    }

    /**
     * Indicates this fragment supports dual IR functionality.
     * @return True as this is a dual IR fragment
     */
    override fun isDualIR(): Boolean = true

    /**
     * Configures the temperature view for dual IR mode.
     * Sets the product type to dual IR for proper thermal display.
     */
    override fun setTemperatureViewType() {
        getTemperatureDualView().productType = Const.TYPE_IR_DUAL
    }

    /**
     * Initializes the content view using ViewBinding.
     * @return Layout resource ID
     */
    override fun initContentView(): Int {
        binding = FragmentIrPlushBinding.inflate(layoutInflater)
        return R.layout.fragment_ir_plush
    }

    /**
     * Initializes data components.
     * Currently empty - data initialization handled by parent class.
     */
    override fun initData() {
        // Data initialization handled by parent class
    }

    /**
     * Initializes view components with ViewBinding integration.
     * Calls parent initialization for comprehensive setup.
     */
    override fun initView() {
        super.initView()
    }

    /**
     * Handles fragment stop lifecycle event.
     * Ensures proper resource cleanup.
     */
    override fun onStop() {
        super.onStop()
    }

    /**
     * Handles fragment destruction lifecycle event.
     * Ensures complete resource cleanup.
     */
    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * Captures current thermal imaging bitmap for processing or export.
     * @return Current scaled bitmap from dual view, or null if unavailable
     */
    fun getBitmap(): Bitmap? = dualView?.scaledBitmap
}