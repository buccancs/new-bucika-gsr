package com.infisense.usbir.utils

/**
 * @ProjectName: ANDROID_IRUVC_SDK
 * @Package: com.infisense.usbirmini640.utils
 * @ClassName: USBMonitorCallback
 * @Description:
 * @Author: brilliantzhao
 * @CreateDate: 3/16/2023 1:20 PM
 * @UpdateUser:
 * @UpdateDate: 3/16/2023 1:20 PM
 * @UpdateRemark:
 * @Version: 1.0.0
 */
interface USBMonitorCallback {

    fun onAttach()

    fun onGranted()

    fun onConnect()

    fun onDisconnect()

    fun onDettach()

    fun onCancel()
}