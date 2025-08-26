package com.infisense.usbdual.camera

import android.os.Handler
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import com.energy.commonlibrary.view.SurfaceNativeWindow
import com.energy.iruvc.dual.ConcreateDualBuilder
import com.energy.iruvc.dual.DualType
import com.energy.iruvc.dual.DualUVCCamera
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.IAlignCallback
import com.energy.iruvc.utils.IFrameCallback
import com.energy.iruvc.uvc.UVCCamera
import com.infisense.usbdual.Const.HIDE_LOADING

class DualViewWithManualAlignExternalCamera(
    irWidth: Int,
    irHeight: Int,
    vlWidth: Int,
    vlHeight: Int,
    dualWidth: Int,
    dualHeight: Int,
    val cameraview: SurfaceView,
    iruvc: UVCCamera,
    dataFlowMode: CommonParams.DataFlowMode
) : BaseParamDualView(irWidth, irHeight, vlWidth, vlHeight, dualWidth, dualHeight) {
    
    private val tag = "DualViewWithManualAlignExternalCamera"
    private val dualUVCCamera: DualUVCCamera
    private var handler: Handler? = null
    
    private val fusionLength = mDualWidth * mDualHeight * 4
    private val mixData = ByteArray(fusionLength)
    private var firstFrame = false
    private val mSurfaceNativeWindow = SurfaceNativeWindow()
    private var mSurface: Surface? = null
    
    private val iFrameCallback = IFrameCallback { frame ->
        Log.d(tag, "onFrame")
        System.arraycopy(frame, 0, mixData, 0, fusionLength)
        
        mSurface = cameraview.holder.surface
        mSurface?.let { surface ->
            mSurfaceNativeWindow.onDrawFrame(surface, mixData, mDualWidth, mDualHeight)
        }
        
        if (!firstFrame) {
            firstFrame = true
            handler?.sendEmptyMessage(HIDE_LOADING)
        }
    }
    
    private val iAlignCallback = IAlignCallback { frame ->
        System.arraycopy(frame, 0, mixData, 0, fusionLength)
        mSurface = cameraview.holder.surface
        mSurface?.let { surface ->
            mSurfaceNativeWindow.onDrawFrame(surface, mixData, mDualWidth, mDualHeight)
        }
    }
    
    init {
        dualUVCCamera = ConcreateDualBuilder()
            .setDualType(DualType.USB_DUAL)
            .setIRSize(mIrWidth, mIrHeight)
            .setVLSize(mVlWidth, mVlHeight)
            .setDualSize(mDualHeight, mDualWidth)
            .setDataFlowMode(dataFlowMode)
            .setPreviewCameraStyle(CommonParams.PreviewCameraStyle.EXTERNAL_CAMERA)
            .setDeviceStyle(CommonParams.DeviceStyle.ALL_IN_ONE)
            .setUseDualGPU(false)
            .setMultiThreadHandleDualEnable(false)
            .build()
        
        dualUVCCamera.addIrUVCCamera(iruvc)
    }
    
    fun setHandler(handler: Handler) {
        this.handler = handler
    }
    
    fun startPreview() {
        dualUVCCamera.setFrameCallback(iFrameCallback)
        dualUVCCamera.onStartPreview()
        firstFrame = false
    }
    
    fun getDualUVCCamera(): DualUVCCamera = dualUVCCamera
    
    fun stopPreview() {
        dualUVCCamera.setFrameCallback(null)
        dualUVCCamera.onStopPreview()
    }
    
    fun destroyPreview() {
        dualUVCCamera.onDestroy()
    }
    
    fun startAlign() {
        dualUVCCamera.setAlignCallback(iAlignCallback)
        dualUVCCamera.startManualAlign()
    }
    
    fun onDraw() {
        mSurface = cameraview.holder.surface
        mSurface?.let { surface ->
            mSurfaceNativeWindow.onDrawFrame(surface, mixData, mDualWidth, mDualHeight)
        }
    }
}