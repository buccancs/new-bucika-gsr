package com.infisense.usbir.utils

interface USBMonitorCallback {

    fun onAttach()

    fun onGranted()

    fun onConnect()

    fun onDisconnect()

    fun onDettach()

    fun onCancel()
}