package com.infisense.usbdual.camera

import com.energy.iruvc.dual.DualUVCCamera
import com.infisense.usbdual.Const

abstract class BaseDualView {
    
    interface OnFrameCallback {
        fun onFame(mixData: ByteArray, remapTempData: ByteArray, fpsText: Double)
    }
    
    protected val onFrameCallbacks = mutableListOf<OnFrameCallback>()
    var dualUVCCamera: DualUVCCamera? = null
    
    protected val fusionLength = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4
    protected val irSize = Const.IR_WIDTH * Const.IR_HEIGHT
    protected val vlSize = Const.VL_WIDTH * Const.VL_HEIGHT * 3
    protected val remapTempSize = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2
    
    protected val remapTempData = ByteArray(Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2)
    protected val mixData = ByteArray(fusionLength)
    protected val normalTempData = ByteArray(irSize * 2)
    protected val mixDataRotate: ByteArray? = null
    protected val irData = ByteArray(irSize * 2)
    val vlData = ByteArray(vlSize)
    val vlARGBData = ByteArray(fusionLength)
    
    fun addFrameCallback(onFrameCallback: OnFrameCallback) {
        onFrameCallbacks.add(onFrameCallback)
    }
    
    fun removeFrameCallback(onFrameCallback: OnFrameCallback) {
        onFrameCallbacks.remove(onFrameCallback)
    }
}