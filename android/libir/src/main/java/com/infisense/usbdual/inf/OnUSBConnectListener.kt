package com.infisense.usbdual.inf

import android.hardware.usb.UsbDevice
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.usb.USBMonitor

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
