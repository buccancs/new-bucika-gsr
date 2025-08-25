package com.infisense.usbdual.camera

import android.hardware.usb.UsbDevice
import android.os.SystemClock
import android.util.Log
import com.blankj.utilcode.util.Utils
import com.energy.iruvc.ircmd.ConcreteIRCMDBuilder
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.ircmd.IRCMDType
import com.energy.iruvc.usb.USBMonitor
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.IFrameCallback
import com.energy.iruvc.uvc.ConcreateUVCBuilder
import com.energy.iruvc.uvc.UVCCamera
import com.energy.iruvc.uvc.UVCType
import com.infisense.usbdual.Const
import com.infisense.usbdual.inf.OnUSBConnectListener

object USBMonitorDualManager {
    
    const val TAG = "USBMonitorDualManager"
    
    private var mUSBMonitor: USBMonitor? = null
    private var mIrcmd: IRCMD? = null
    private val mSyncs = Any()
    private val mOnUSBConnectListeners = mutableListOf<OnUSBConnectListener>()
    
    private var mIrUvcCamera: UVCCamera? = null
    private var mVlUvcCamera: UVCCamera? = null
    
    private var mIrOpened = false
    private var mVlOpened = false
    
    private var mVlIFrameCallback: IFrameCallback? = null
    
    fun addOnUSBConnectListener(onUSBConnectListener: OnUSBConnectListener) {
        mOnUSBConnectListeners.add(onUSBConnectListener)
    }
    
    fun removeOnUSBConnectListener(onUSBConnectListener: OnUSBConnectListener) {
        mOnUSBConnectListeners.remove(onUSBConnectListener)
    }
    
    fun init(
        irPid: Int, irFPS: Int, irWidth: Int, irHeight: Int, irBandWith: Float,
        vlPid: Int, vlFPS: Int, vlWidth: Int, vlHeight: Int, vlBandWith: Float,
        frameCallback: IFrameCallback
    ) {
        mVlIFrameCallback = frameCallback
        
        if (mUSBMonitor == null) {
            mUSBMonitor = USBMonitor(Utils.getApp(), object : USBMonitor.OnDeviceConnectListener {
                
                override fun onAttach(device: UsbDevice) {
                    Log.w(TAG, "USBMonitor-onAttach-getProductId = ${device.productId}")
                    mUSBMonitor?.requestPermission(device)
                    mOnUSBConnectListeners.forEach { listener ->
                        listener.onAttach(device)
                    }
                }
                
                override fun onGranted(usbDevice: UsbDevice, granted: Boolean) {
                    Log.d(TAG, "USBMonitor-onGranted")
                    mOnUSBConnectListeners.forEach { listener ->
                        listener.onGranted(usbDevice, granted)
                    }
                }
                
                override fun onDettach(device: UsbDevice) {
                    Log.d(TAG, "USBMonitor-onDettach")
                    Const.isDeviceConnected = false
                    mOnUSBConnectListeners.forEach { listener ->
                        listener.onDettach(device)
                    }
                }
                
                override fun onConnect(
                    device: UsbDevice,
                    ctrlBlock: USBMonitor.UsbControlBlock,
                    createNew: Boolean
                ) {
                    Log.w(TAG, "USBMonitor-onConnect createNew $createNew")
                    Log.w(TAG, "USBMonitor-onConnect Pid ${device.productId}")
                    
                    if (!mIrOpened) {
                        openIrUVCCamera(irPid, irWidth, irHeight, irFPS, irBandWith, device, ctrlBlock)
                    }
                    if (!mVlOpened) {
                        openVlUVCCamera(vlPid, vlWidth, vlHeight, vlFPS, vlBandWith, device, ctrlBlock)
                    }
                    
                    if (mIrOpened && mVlOpened) {
                        mOnUSBConnectListeners.forEach { listener ->
                            listener.onConnect(device, ctrlBlock, createNew)
                        }
                    }
                }
                
                override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
                    Log.w(TAG, "USBMonitor-onDisconnect")
                    Const.isDeviceConnected = false
                    mOnUSBConnectListeners.forEach { listener ->
                        listener.onDisconnect(device, ctrlBlock)
                    }
                }
            })
            mUSBMonitor?.register()
        }
    }
    
    private fun openIrUVCCamera(
        pid: Int, width: Int, height: Int, fps: Int, bandWidth: Float,
        device: UsbDevice, controlBlock: USBMonitor.UsbControlBlock
    ) {
        synchronized(mSyncs) {
            if (device.productId != pid) {
                return
            }
            Log.w(TAG, "USBMonitor-openIrUVCCamera ${device.productId}")
            
            if (mIrUvcCamera == null) {
                val concreateUVCBuilder = ConcreateUVCBuilder()
                mIrUvcCamera = concreateUVCBuilder
                    .setUVCType(UVCType.USB_UVC)
                    .build()
                
                mIrUvcCamera?.apply {
                    setDefaultPreviewMode(CommonParams.FRAMEFORMATType.FRAME_FORMAT_YUYV)
                    setDefaultBandwidth(bandWidth)
                    setDefaultPreviewMaxFps(fps)
                    openUVCCamera(controlBlock)
                    setUSBPreviewSize(width, height)
                    onStartPreview()
                }
                
                val concreteIRCMDBuilder = ConcreteIRCMDBuilder()
                mIrcmd = concreteIRCMDBuilder
                    .setIRCMDType(IRCMDType.USB_IRCMD)
                    .build()
                mIrcmd?.init(controlBlock)
                
                mIrOpened = true
                Log.w(TAG, "USBMonitor-openIrUVCCamera complete ${device.productId}")
            }
        }
    }
    
    private fun openVlUVCCamera(
        pid: Int, vlWidth: Int, vlHeight: Int, vlFps: Int, vlBandWidth: Float,
        device: UsbDevice, controlBlock: USBMonitor.UsbControlBlock
    ) {
        synchronized(mSyncs) {
            if (device.productId != pid) {
                return
            }
            Log.w(TAG, "USBMonitor-openVlUVCCamera ${device.productId}")
            
            if (mVlUvcCamera == null) {
                val concreateUVCBuilder = ConcreateUVCBuilder()
                mVlUvcCamera = concreateUVCBuilder
                    .setUVCType(UVCType.USB_UVC)
                    .build()
                
                mVlUvcCamera?.apply {
                    setDefaultPreviewMode(CommonParams.FRAMEFORMATType.FRAME_FORMAT_MJPEG)
                    setDefaultBandwidth(vlBandWidth)
                    setDefaultPreviewMaxFps(vlFps)
                    openUVCCamera(controlBlock)
                    setUSBPreviewSize(vlWidth, vlHeight)
                    mVlIFrameCallback?.let { setFrameCallback(it) }
                    onStartPreview()
                }
                
                mVlOpened = true
                Log.w(TAG, "USBMonitor-openVlUVCCamera complete ${device.productId}")
            }
        }
    }
    
    fun stopIrUVCCamera() {
        Log.i(TAG, "stopIrUVCCamera")
        mIrUvcCamera?.let { camera ->
            camera.onStopPreview()
            SystemClock.sleep(200)
            camera.onDestroyPreview()
            mIrUvcCamera = null
            mIrOpened = false
        }
    }
    
    fun stopVlUVCCamera() {
        Log.i(TAG, "stopVlUVCCamera")
        mVlUvcCamera?.let { camera ->
            camera.onStopPreview()
            SystemClock.sleep(200)
            camera.onDestroyPreview()
            mVlUvcCamera = null
            mVlOpened = false
        }
    }
    
    fun getIrcmd(): IRCMD? = mIrcmd
    
    fun getIrUvcCamera(): UVCCamera? = mIrUvcCamera
    
    fun getVlUvcCamera(): UVCCamera? = mVlUvcCamera
    
    fun onRelease() {
        mVlIFrameCallback = null
        mUSBMonitor = null
        mIrcmd = null
        mIrUvcCamera = null
        mVlUvcCamera = null
        mVlOpened = false
        mIrOpened = false
    }
}