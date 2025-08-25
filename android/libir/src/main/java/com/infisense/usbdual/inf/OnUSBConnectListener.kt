package com.infisense.usbdual.inf

import android.hardware.usb.UsbDevice
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.usb.USBMonitor

/**
 * @ProjectName: ANDROID_IRUVC_SDK
 * @Package: com.infisense.usbdual.utils
 * @ClassName: OnUSBConnectListener
 * @Description:
 * @Author: brilliantzhao
 * @CreateDate: 4/24/2023 1:37 PM
 * @UpdateUser:
 * @UpdateDate: 4/24/2023 1:37 PM
 * @UpdateRemark:
 * @Version: 1.0.0
 */
interface OnUSBConnectListener {

    fun onAttach(device: UsbDevice)

    fun onGranted(usbDevice: UsbDevice, granted: Boolean)

    fun onDettach(device: UsbDevice)

    fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean)

    fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock)

    fun onCancel(device: UsbDevice)

    fun onIRCMDInit(ircmd: IRCMD)

    fun onCompleteInit()

    fun onSetPreviewSizeFail()
