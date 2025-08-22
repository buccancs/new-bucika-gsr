package com.multisensor.recording.managers

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbDeviceManager @Inject constructor() {

    interface UsbDeviceCallback {
        fun onSupportedDeviceAttached(device: UsbDevice)
        fun onUnsupportedDeviceAttached(device: UsbDevice)
        fun onDeviceDetached(device: UsbDevice)
        fun onError(message: String)
    }

    private val supportedDevices = listOf(
        Pair(0x0BDA, 0x3901),
        Pair(0x0BDA, 0x5840),
        Pair(0x0BDA, 0x5830),
        Pair(0x0BDA, 0x5838),
        Pair(0x0BDA, 0x5841),
        Pair(0x0BDA, 0x5842),
        Pair(0x0BDA, 0x3902),
        Pair(0x0BDA, 0x3903),
    )

    fun handleUsbDeviceIntent(intent: Intent, callback: UsbDeviceCallback) {
        android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] Handling USB device intent: ${intent.action}")

        try {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val usbDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    }
                    if (usbDevice != null) {
                        handleDeviceAttached(usbDevice, callback)
                    } else {
                        android.util.Log.w(
                            "UsbDeviceManager",
                            "[DEBUG_LOG] USB device attached but no device in intent"
                        )
                        callback.onError("USB device attached but no device information available")
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val usbDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    }
                    if (usbDevice != null) {
                        handleDeviceDetached(usbDevice, callback)
                    } else {
                        android.util.Log.w(
                            "UsbDeviceManager",
                            "[DEBUG_LOG] USB device detached but no device in intent"
                        )
                    }
                }

                else -> {
                    android.util.Log.w("UsbDeviceManager", "[DEBUG_LOG] Unhandled USB intent action: ${intent.action}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UsbDeviceManager", "[DEBUG_LOG] Error handling USB device intent: ${e.message}")
            callback.onError("Failed to handle USB device intent: ${e.message}")
        }
    }

    private fun handleDeviceAttached(usbDevice: UsbDevice, callback: UsbDeviceCallback) {
        android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] USB device attached:")
        android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] - Device name: ${usbDevice.deviceName}")
        android.util.Log.d(
            "UsbDeviceManager",
            "[DEBUG_LOG] - Vendor ID: 0x${String.format("%04X", usbDevice.vendorId)}"
        )
        android.util.Log.d(
            "UsbDeviceManager",
            "[DEBUG_LOG] - Product ID: 0x${String.format("%04X", usbDevice.productId)}"
        )
        android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] - Device class: ${usbDevice.deviceClass}")

        if (isSupportedTopdonDevice(usbDevice)) {
            android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] Supported TOPDON device detected")
            callback.onSupportedDeviceAttached(usbDevice)
        } else {
            android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] Unsupported device detected")
            callback.onUnsupportedDeviceAttached(usbDevice)
        }
    }

    private fun handleDeviceDetached(usbDevice: UsbDevice, callback: UsbDeviceCallback) {
        android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] USB device detached:")
        android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] - Device name: ${usbDevice.deviceName}")
        android.util.Log.d(
            "UsbDeviceManager",
            "[DEBUG_LOG] - Vendor ID: 0x${String.format("%04X", usbDevice.vendorId)}"
        )
        android.util.Log.d(
            "UsbDeviceManager",
            "[DEBUG_LOG] - Product ID: 0x${String.format("%04X", usbDevice.productId)}"
        )

        callback.onDeviceDetached(usbDevice)
    }

    fun isSupportedTopdonDevice(device: UsbDevice): Boolean {
        val vendorId = device.vendorId
        val productId = device.productId

        android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] Checking device support:")
        android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] - Vendor ID: 0x${String.format("%04X", vendorId)}")
        android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] - Product ID: 0x${String.format("%04X", productId)}")

        val isSupported = supportedDevices.any { (supportedVendorId, supportedProductId) ->
            vendorId == supportedVendorId && productId == supportedProductId
        }

        android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] Device support result: $isSupported")
        return isSupported
    }

    fun getConnectedUsbDevices(context: Context): List<UsbDevice> {
        return try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceList = usbManager.deviceList

            android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] Found ${deviceList.size} connected USB devices")

            deviceList.values.toList().also { devices ->
                devices.forEach { device ->
                    android.util.Log.d(
                        "UsbDeviceManager",
                        "[DEBUG_LOG] - ${device.deviceName}: VID=0x${
                            String.format(
                                "%04X",
                                device.vendorId
                            )
                        }, PID=0x${String.format("%04X", device.productId)}"
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UsbDeviceManager", "[DEBUG_LOG] Error getting connected USB devices: ${e.message}")
            emptyList()
        }
    }

    fun getConnectedSupportedDevices(context: Context): List<UsbDevice> {
        return getConnectedUsbDevices(context).filter { device ->
            isSupportedTopdonDevice(device)
        }.also { supportedDevices ->
            android.util.Log.d(
                "UsbDeviceManager",
                "[DEBUG_LOG] Found ${supportedDevices.size} supported TOPDON devices"
            )
        }
    }

    fun hasSupportedDeviceConnected(context: Context): Boolean {
        return getConnectedSupportedDevices(context).isNotEmpty().also { hasSupported ->
            android.util.Log.d("UsbDeviceManager", "[DEBUG_LOG] Has supported device connected: $hasSupported")
        }
    }

    fun getDeviceInfoString(device: UsbDevice): String {
        return buildString {
            append("${device.deviceName}\n")
            append("Vendor ID: 0x${String.format("%04X", device.vendorId)}\n")
            append("Product ID: 0x${String.format("%04X", device.productId)}\n")
            append("Device Class: ${device.deviceClass}")
        }
    }

}
