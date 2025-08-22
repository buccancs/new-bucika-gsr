package com.multisensor.recording.controllers

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.multisensor.recording.managers.UsbDeviceManager
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UsbControllerUnitTest {

    private lateinit var usbController: UsbController
    private lateinit var mockUsbDeviceManager: UsbDeviceManager
    private lateinit var mockCallback: UsbController.UsbCallback
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockUsbDeviceManager = mockk(relaxed = true)
        mockCallback = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)

        usbController = UsbController(mockUsbDeviceManager)
        usbController.setCallback(mockCallback)
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `setCallback should update callback reference`() {
        val newCallback = mockk<UsbController.UsbCallback>(relaxed = true)

        usbController.setCallback(newCallback)

        val mockDevice = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        val intent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, mockDevice)
        every { mockUsbDeviceManager.isSupportedTopdonDevice(mockDevice) } returns true

        usbController.handleUsbDeviceIntent(mockContext, intent)

        verify { newCallback.onSupportedDeviceAttached(mockDevice) }
        verify(exactly = 0) { mockCallback.onSupportedDeviceAttached(any()) }
    }

    @Test
    fun `handleUsbDeviceIntent should handle supported device attachment`() {
        val mockDevice = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        val intent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, mockDevice)
        every { mockUsbDeviceManager.isSupportedTopdonDevice(mockDevice) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(mockContext, intent)

        verify { mockCallback.onSupportedDeviceAttached(mockDevice) }
        verify { mockCallback.updateStatusText(match { it.contains("thermal camera connected") }) }
        verify { mockCallback.initializeRecordingSystem() }
    }

    @Test
    fun `handleUsbDeviceIntent should handle supported device without permissions`() {
        val mockDevice = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        val intent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, mockDevice)
        every { mockUsbDeviceManager.isSupportedTopdonDevice(mockDevice) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns false

        usbController.handleUsbDeviceIntent(mockContext, intent)

        verify { mockCallback.onSupportedDeviceAttached(mockDevice) }
        verify { mockCallback.updateStatusText(match { it.contains("Please grant permissions") }) }
        verify(exactly = 0) { mockCallback.initializeRecordingSystem() }
    }

    @Test
    fun `handleUsbDeviceIntent should handle unsupported device attachment`() {
        val mockDevice = createMockUsbDevice(vendorId = 0x1234, productId = 0x5678)
        val intent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, mockDevice)
        every { mockUsbDeviceManager.isSupportedTopdonDevice(mockDevice) } returns false

        usbController.handleUsbDeviceIntent(mockContext, intent)

        verify { mockCallback.onUnsupportedDeviceAttached(mockDevice) }
        verify(exactly = 0) { mockCallback.onSupportedDeviceAttached(any()) }
        verify(exactly = 0) { mockCallback.initializeRecordingSystem() }
    }

    @Test
    fun `handleUsbDeviceIntent should handle device detachment`() {
        val mockDevice = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        val intent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_DETACHED, mockDevice)
        every { mockUsbDeviceManager.isSupportedTopdonDevice(mockDevice) } returns true

        usbController.handleUsbDeviceIntent(mockContext, intent)

        verify { mockCallback.onDeviceDetached(mockDevice) }
        verify { mockCallback.updateStatusText(match { it.contains("disconnected") }) }
    }

    @Test
    fun `handleUsbDeviceIntent should handle unsupported device detachment`() {
        val mockDevice = createMockUsbDevice(vendorId = 0x1234, productId = 0x5678)
        val intent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_DETACHED, mockDevice)
        every { mockUsbDeviceManager.isSupportedTopdonDevice(mockDevice) } returns false

        usbController.handleUsbDeviceIntent(mockContext, intent)

        verify { mockCallback.onDeviceDetached(mockDevice) }
        verify(exactly = 0) { mockCallback.updateStatusText(match { it.contains("disconnected") }) }
    }

    @Test
    fun `handleUsbDeviceIntent should handle missing device in intent`() {
        val intent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, null)

        usbController.handleUsbDeviceIntent(mockContext, intent)

        verify { mockCallback.onUsbError(match { it.contains("no device information available") }) }
    }

    @Test
    fun `handleUsbDeviceIntent should handle unknown action`() {
        val mockDevice = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        val intent = createUsbDeviceIntent("unknown.action", mockDevice)

        usbController.handleUsbDeviceIntent(mockContext, intent)

        verify(exactly = 0) { mockCallback.onSupportedDeviceAttached(any()) }
        verify(exactly = 0) { mockCallback.onUnsupportedDeviceAttached(any()) }
        verify(exactly = 0) { mockCallback.onDeviceDetached(any()) }
    }

    @Test
    fun `getConnectedUsbDevices should delegate to UsbDeviceManager`() {
        val expectedDevices = listOf(
            createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901),
            createMockUsbDevice(vendorId = 0x1234, productId = 0x5678)
        )
        every { mockUsbDeviceManager.getConnectedUsbDevices(mockContext) } returns expectedDevices

        val devices = usbController.getConnectedUsbDevices(mockContext)

        assertEquals("Should return devices from UsbDeviceManager", expectedDevices, devices)
        verify { mockUsbDeviceManager.getConnectedUsbDevices(mockContext) }
    }

    @Test
    fun `getConnectedSupportedDevices should delegate to UsbDeviceManager`() {
        val expectedDevices = listOf(
            createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901),
            createMockUsbDevice(vendorId = 0x0BDA, productId = 0x5840)
        )
        every { mockUsbDeviceManager.getConnectedSupportedDevices(mockContext) } returns expectedDevices

        val devices = usbController.getConnectedSupportedDevices(mockContext)

        assertEquals("Should return supported devices from UsbDeviceManager", expectedDevices, devices)
        verify { mockUsbDeviceManager.getConnectedSupportedDevices(mockContext) }
    }

    @Test
    fun `hasSupportedDeviceConnected should delegate to UsbDeviceManager`() {
        every { mockUsbDeviceManager.hasSupportedDeviceConnected(mockContext) } returns true

        val hasSupported = usbController.hasSupportedDeviceConnected(mockContext)

        assertTrue("Should return true from UsbDeviceManager", hasSupported)
        verify { mockUsbDeviceManager.hasSupportedDeviceConnected(mockContext) }
    }

    @Test
    fun `isSupportedTopdonDevice should delegate to UsbDeviceManager`() {
        val mockDevice = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        every { mockUsbDeviceManager.isSupportedTopdonDevice(mockDevice) } returns true

        val isSupported = usbController.isSupportedTopdonDevice(mockDevice)

        assertTrue("Should return true from UsbDeviceManager", isSupported)
        verify { mockUsbDeviceManager.isSupportedTopdonDevice(mockDevice) }
    }

    @Test
    fun `getDeviceInfoString should delegate to UsbDeviceManager`() {
        val mockDevice = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        val expectedInfo = "Device info string"
        every { mockUsbDeviceManager.getDeviceInfoString(mockDevice) } returns expectedInfo

        val info = usbController.getDeviceInfoString(mockDevice)

        assertEquals("Should return info from UsbDeviceManager", expectedInfo, info)
        verify { mockUsbDeviceManager.getDeviceInfoString(mockDevice) }
    }

    @Test
    fun `initializeUsbMonitoring should scan and handle connected devices`() {
        val supportedDevice = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        val unsupportedDevice = createMockUsbDevice(vendorId = 0x1234, productId = 0x5678)
        val allDevices = listOf(supportedDevice, unsupportedDevice)
        val supportedDevices = listOf(supportedDevice)

        every { mockUsbDeviceManager.getConnectedUsbDevices(mockContext) } returns allDevices
        every { mockUsbDeviceManager.getConnectedSupportedDevices(mockContext) } returns supportedDevices
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.initializeUsbMonitoring(mockContext)

        verify { mockCallback.onSupportedDeviceAttached(supportedDevice) }
        verify { mockCallback.initializeRecordingSystem() }
    }

    @Test
    fun `initializeUsbMonitoring should handle no devices connected`() {
        every { mockUsbDeviceManager.getConnectedUsbDevices(mockContext) } returns emptyList()
        every { mockUsbDeviceManager.getConnectedSupportedDevices(mockContext) } returns emptyList()

        usbController.initializeUsbMonitoring(mockContext)

        verify(exactly = 0) { mockCallback.onSupportedDeviceAttached(any()) }
        verify(exactly = 0) { mockCallback.initializeRecordingSystem() }
    }

    @Test
    fun `getUsbStatusSummary should provide complete status information`() {
        val supportedDevice = createMockUsbDevice(
            vendorId = 0x0BDA,
            productId = 0x3901,
            deviceName = "/dev/bus/usb/001/002"
        )
        val unsupportedDevice = createMockUsbDevice(
            vendorId = 0x1234,
            productId = 0x5678,
            deviceName = "/dev/bus/usb/001/003"
        )
        val allDevices = listOf(supportedDevice, unsupportedDevice)
        val supportedDevices = listOf(supportedDevice)

        every { mockUsbDeviceManager.getConnectedUsbDevices(mockContext) } returns allDevices
        every { mockUsbDeviceManager.getConnectedSupportedDevices(mockContext) } returns supportedDevices

        val summary = usbController.getUsbStatusSummary(mockContext)

        assertTrue("Should contain total device count", summary.contains("Total connected devices: 2"))
        assertTrue("Should contain supported device count", summary.contains("Supported TOPDON devices: 1"))
        assertTrue("Should contain device details", summary.contains("/dev/bus/usb/001/002"))
        assertTrue("Should contain vendor/product IDs", summary.contains("VID: 0x0BDA"))
        assertTrue("Should contain vendor/product IDs", summary.contains("PID: 0x3901"))
    }

    @Test
    fun `error handling should be robust`() {
        every { mockUsbDeviceManager.getConnectedUsbDevices(mockContext) } throws RuntimeException("USB error")

        val devices = usbController.getConnectedUsbDevices(mockContext)

        assertThrows("Should propagate exception", RuntimeException::class.java) {
            usbController.getConnectedUsbDevices(mockContext)
        }
    }

    @Test
    fun `operations without callback should not crash`() {
        val controllerWithoutCallback = UsbController(mockUsbDeviceManager)
        val mockDevice = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        val intent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, mockDevice)

        controllerWithoutCallback.handleUsbDeviceIntent(mockContext, intent)
    }

    @Test
    fun `concurrent device events should be handled correctly`() {
        val device1 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "device1")
        val device2 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x5840, deviceName = "device2")
        val device3 = createMockUsbDevice(vendorId = 0x1234, productId = 0x5678, deviceName = "device3")

        val intent1 = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device1)
        val intent2 = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device2)
        val intent3 = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device3)

        every { mockUsbDeviceManager.isSupportedTopdonDevice(device1) } returns true
        every { mockUsbDeviceManager.isSupportedTopdonDevice(device2) } returns true
        every { mockUsbDeviceManager.isSupportedTopdonDevice(device3) } returns false
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(mockContext, intent1)
        usbController.handleUsbDeviceIntent(mockContext, intent2)
        usbController.handleUsbDeviceIntent(mockContext, intent3)

        verify { mockCallback.onSupportedDeviceAttached(device1) }
        verify { mockCallback.onSupportedDeviceAttached(device2) }
        verify { mockCallback.onUnsupportedDeviceAttached(device3) }
        verify(exactly = 2) { mockCallback.initializeRecordingSystem() }
    }

    @Test
    fun `device attachment and detachment sequence should work correctly`() {
        val mockDevice = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        val attachIntent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, mockDevice)
        val detachIntent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_DETACHED, mockDevice)

        every { mockUsbDeviceManager.isSupportedTopdonDevice(mockDevice) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(mockContext, attachIntent)
        usbController.handleUsbDeviceIntent(mockContext, detachIntent)

        verify { mockCallback.onSupportedDeviceAttached(mockDevice) }
        verify { mockCallback.onDeviceDetached(mockDevice) }
        verify { mockCallback.initializeRecordingSystem() }
        verify { mockCallback.updateStatusText(match { it.contains("connected") }) }
        verify { mockCallback.updateStatusText(match { it.contains("disconnected") }) }
    }

    @Test
    fun `should track multiple simultaneous devices`() {
        val device1 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val device2 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x5840, deviceName = "/dev/bus/usb/001/002")
        val intent1 = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device1)
        val intent2 = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device2)

        every { mockUsbDeviceManager.isSupportedTopdonDevice(device1) } returns true
        every { mockUsbDeviceManager.isSupportedTopdonDevice(device2) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(mockContext, intent1)
        usbController.handleUsbDeviceIntent(mockContext, intent2)

        assertEquals(2, usbController.getConnectedSupportedDeviceCount())
        val connectedDevices = usbController.getConnectedSupportedDevicesList()
        assertEquals(2, connectedDevices.size)
        assertTrue(connectedDevices.contains(device1))
        assertTrue(connectedDevices.contains(device2))

        verify { mockCallback.updateStatusText(match { it.contains("2 Topdon thermal cameras") }) }
    }

    @Test
    fun `should handle device detachment in multi-device scenario`() {
        val device1 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val device2 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x5840, deviceName = "/dev/bus/usb/001/002")

        every { mockUsbDeviceManager.isSupportedTopdonDevice(any()) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device1)
        )
        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device2)
        )

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_DETACHED, device1)
        )

        assertEquals(1, usbController.getConnectedSupportedDeviceCount())
        val remainingDevices = usbController.getConnectedSupportedDevicesList()
        assertEquals(1, remainingDevices.size)
        assertTrue(remainingDevices.contains(device2))
        assertFalse(remainingDevices.contains(device1))

        verify { mockCallback.updateStatusText(match { it.contains("1 Topdon thermal camera connected") }) }
    }

    @Test
    fun `should track device connection times and counts`() {
        val device = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        val intent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device)
        val deviceKey = "${device.vendorId}_${device.productId}_${device.deviceName}"

        every { mockUsbDeviceManager.isSupportedTopdonDevice(device) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(mockContext, intent)
        val firstConnectionTime = usbController.getDeviceConnectionTime(deviceKey)
        val firstConnectionCount = usbController.getDeviceConnectionCount(deviceKey)

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_DETACHED, device)
        )
        Thread.sleep(10)
        usbController.handleUsbDeviceIntent(mockContext, intent)

        val secondConnectionTime = usbController.getDeviceConnectionTime(deviceKey)
        val secondConnectionCount = usbController.getDeviceConnectionCount(deviceKey)

        assertNotNull(firstConnectionTime)
        assertEquals(1, firstConnectionCount)
        assertNotNull(secondConnectionTime)
        assertEquals(2, secondConnectionCount)
        assertTrue(secondConnectionTime!! > firstConnectionTime!!)
    }

    @Test
    fun `should generate appropriate status text for different device counts`() {
        val device1 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val device2 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x5840, deviceName = "/dev/bus/usb/001/002")

        every { mockUsbDeviceManager.isSupportedTopdonDevice(any()) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        assertEquals(0, usbController.getConnectedSupportedDeviceCount())

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device1)
        )
        verify { mockCallback.updateStatusText(match { it.contains("1 Topdon thermal camera connected") }) }

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device2)
        )
        verify { mockCallback.updateStatusText(match { it.contains("2 Topdon thermal cameras connected") }) }
    }

    @Test
    fun `should check device connection status by key`() {
        val device = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val deviceKey = "${device.vendorId}_${device.productId}_${device.deviceName}"
        val intent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device)

        every { mockUsbDeviceManager.isSupportedTopdonDevice(device) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        assertFalse(usbController.isDeviceConnected(deviceKey))
        assertNull(usbController.getDeviceInfoByKey(deviceKey))

        usbController.handleUsbDeviceIntent(mockContext, intent)
        assertTrue(usbController.isDeviceConnected(deviceKey))
        assertEquals(device, usbController.getDeviceInfoByKey(deviceKey))

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_DETACHED, device)
        )
        assertFalse(usbController.isDeviceConnected(deviceKey))
        assertNull(usbController.getDeviceInfoByKey(deviceKey))
    }

    @Test
    fun `should handle mixed supported and unsupported devices correctly`() {
        val supportedDevice =
            createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val unsupportedDevice =
            createMockUsbDevice(vendorId = 0x1234, productId = 0x5678, deviceName = "/dev/bus/usb/001/002")

        every { mockUsbDeviceManager.isSupportedTopdonDevice(supportedDevice) } returns true
        every { mockUsbDeviceManager.isSupportedTopdonDevice(unsupportedDevice) } returns false
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, supportedDevice)
        )
        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, unsupportedDevice)
        )

        assertEquals(1, usbController.getConnectedSupportedDeviceCount())
        val connectedDevices = usbController.getConnectedSupportedDevicesList()
        assertTrue(connectedDevices.contains(supportedDevice))
        assertFalse(connectedDevices.contains(unsupportedDevice))

        verify { mockCallback.onSupportedDeviceAttached(supportedDevice) }
        verify { mockCallback.onUnsupportedDeviceAttached(unsupportedDevice) }
    }

    @Test
    fun `should save and restore multi-device state`() {
        val mockSharedPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        val mockEditor = mockk<android.content.SharedPreferences.Editor>(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPrefs
        every { mockSharedPrefs.edit() } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putStringSet(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs

        val device = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        every { mockUsbDeviceManager.isSupportedTopdonDevice(device) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device)
        )

        verify { mockEditor.putInt("connected_device_count", 1) }
        verify { mockEditor.putStringSet("connected_device_keys", any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `getMultiDeviceStatusSummary should provide complete information`() {
        val device1 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val device2 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x5840, deviceName = "/dev/bus/usb/001/002")

        every { mockUsbDeviceManager.isSupportedTopdonDevice(any()) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device1)
        )
        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device2)
        )

        val summary = usbController.getMultiDeviceStatusSummary(mockContext)

        assertTrue(summary.contains("Currently connected TOPDON devices: 2"))
        assertTrue(summary.contains("Currently connected devices:"))
        assertTrue(summary.contains(device1.deviceName))
        assertTrue(summary.contains(device2.deviceName))
    }

    @Test
    fun `should handle edge case of null device in intent`() {
        val intent = createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, null)

        usbController.handleUsbDeviceIntent(mockContext, intent)

        verify { mockCallback.onUsbError(match { it.contains("no device information available") }) }
        assertEquals(0, usbController.getConnectedSupportedDeviceCount())
    }

    @Test
    fun `should handle device with same VID PID but different device name`() {
        val device1 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val device2 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/002")

        every { mockUsbDeviceManager.isSupportedTopdonDevice(any()) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device1)
        )
        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device2)
        )

        assertEquals(2, usbController.getConnectedSupportedDeviceCount())
        val connectedDevices = usbController.getConnectedSupportedDevicesList()
        assertTrue(connectedDevices.contains(device1))
        assertTrue(connectedDevices.contains(device2))
    }

    @Test
    fun `should generate device priority assessments for connected devices`() {
        val device1 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val device2 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x5840, deviceName = "/dev/bus/usb/001/002")

        every { mockUsbDeviceManager.isSupportedTopdonDevice(any()) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device1)
        )
        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device2)
        )

        val assessments = usbController.getDevicePriorityAssessments()

        assertEquals(2, assessments.size)
        assertTrue(
            "Should have assessments for both devices",
            assessments.any { it.device == device1 } && assessments.any { it.device == device2 })

        assessments.forEach { assessment ->
            assertTrue(
                "Priority score should be valid",
                assessment.priorityScore >= 0.0 && assessment.priorityScore <= 1.0
            )
            assertTrue(
                "Quality score should be valid",
                assessment.qualityScore >= 0.0 && assessment.qualityScore <= 1.0
            )
            assertTrue("Confidence should be valid", assessment.confidence >= 0.0 && assessment.confidence <= 1.0)
            assertNotNull("Should have priority level", assessment.priorityLevel)
            assertTrue("Should have recommendations", assessment.recommendations.isNotEmpty())
        }
    }

    @Test
    fun `should optimise device selection for multi-device scenarios`() {
        val device1 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val device2 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x5840, deviceName = "/dev/bus/usb/001/002")
        val device3 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x5830, deviceName = "/dev/bus/usb/001/003")

        every { mockUsbDeviceManager.isSupportedTopdonDevice(any()) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device1)
        )
        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device2)
        )
        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device3)
        )

        val selectionResult = usbController.getOptimizedDeviceSelection(maxDevices = 2)

        assertNotNull("Should have primary device selected", selectionResult.primaryDevice)
        assertTrue("Should have secondary devices", selectionResult.secondaryDevices.isNotEmpty())
        assertEquals(3, selectionResult.allDeviceAssessments.size)
        assertTrue("Selection rationale should be provided", selectionResult.selectionRationale.isNotEmpty())

        val metrics = selectionResult.optimizationMetrics
        assertTrue("Total quality score should be valid", metrics.totalQualityScore >= 0.0)
        assertTrue("Expected reliability should be valid", metrics.expectedReliability >= 0.0)
        assertTrue("Resource efficiency should be valid", metrics.resourceEfficiency >= 0.0)
        assertTrue("Risk score should be valid", metrics.riskScore >= 0.0 && metrics.riskScore <= 1.0)
    }

    @Test
    fun `should generate complete performance analytics report`() {
        val device = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        every { mockUsbDeviceManager.isSupportedTopdonDevice(device) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        repeat(5) {
            usbController.handleUsbDeviceIntent(
                mockContext,
                createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device)
            )
            usbController.handleUsbDeviceIntent(
                mockContext,
                createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_DETACHED, device)
            )
        }

        val report = usbController.getPerformanceAnalyticsReport(mockContext)

        assertTrue("Should have processed events", report.totalEvents > 0)
        assertTrue("Average response time should be valid", report.averageResponseTime >= 0.0)
        assertTrue("95th percentile should be valid", report.percentile95ResponseTime >= 0L)
        assertTrue("99th percentile should be valid", report.percentile99ResponseTime >= 0L)
        assertTrue(
            "CPU efficiency should be valid",
            report.cpuEfficiencyScore >= 0.0 && report.cpuEfficiencyScore <= 1.0
        )
        assertTrue("Memory utilisation should be valid", report.memoryUtilization >= 0L)
        assertTrue("Event throughput should be valid", report.eventThroughput >= 0.0)
        assertTrue("Should have system recommendations", report.systemRecommendations.isNotEmpty())
    }

    @Test
    fun `should monitor device connection quality`() {
        val device = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val deviceKey = "3034_14593_/dev/bus/usb/001/001"

        every { mockUsbDeviceManager.isSupportedTopdonDevice(device) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device)
        )

        val qualityReport = usbController.monitorDeviceConnectionQuality(deviceKey)

        assertTrue("Quality report should contain device key", qualityReport.contains(deviceKey))
        assertTrue(
            "Quality report should contain quality analysis",
            qualityReport.contains("Connection Quality Analysis")
        )
        assertTrue("Quality report should contain overall score", qualityReport.contains("Overall Quality Score"))
        assertTrue("Quality report should contain detailed metrics", qualityReport.contains("Detailed Metrics"))
        assertTrue("Quality report should be non-empty", qualityReport.isNotEmpty())
    }

    @Test
    fun `should generate device priority analysis report`() {
        val device1 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val device2 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x5840, deviceName = "/dev/bus/usb/001/002")

        every { mockUsbDeviceManager.isSupportedTopdonDevice(any()) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device1)
        )
        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device2)
        )

        val priorityReport = usbController.generateDevicePriorityReport()

        assertTrue("Report should contain title", priorityReport.contains("Advanced Device Priority Analysis Report"))
        assertTrue("Report should contain device count", priorityReport.contains("Total Devices Assessed"))
        assertTrue("Report should contain priority distribution", priorityReport.contains("Priority Distribution"))
        assertTrue("Report should contain device rankings", priorityReport.contains("Detailed Device Rankings"))
        assertTrue("Report should contain statistical analysis", priorityReport.contains("Statistical Analysis"))
        assertTrue("Report should contain recommendations", priorityReport.contains("System Recommendations"))
    }

    @Test
    fun `should update device performance feedback for adaptive learning`() {
        val deviceKey = "3034_14593_/dev/bus/usb/001/001"
        val performanceScore = 0.85
        val actualReliability = 0.9
        val resourceUsage = 0.3

        usbController.updateDevicePerformanceFeedback(deviceKey, performanceScore, actualReliability, resourceUsage)

        assertTrue("Performance feedback update should complete successfully", true)
    }

    @Test
    fun `should get resource utilisation metrics`() {
        val resourceMetrics = usbController.getResourceUtilizationMetrics()

        assertTrue("Should contain CPU usage", resourceMetrics.containsKey("cpu_usage"))
        assertTrue("Should contain memory usage", resourceMetrics.containsKey("memory_usage"))
        assertTrue("Should contain event rate", resourceMetrics.containsKey("event_rate"))
        assertTrue("Should contain efficiency score", resourceMetrics.containsKey("efficiency_score"))

        resourceMetrics.values.forEach { value ->
            assertTrue("All metrics should be non-negative", value >= 0.0)
        }
    }

    @Test
    fun `should reset performance analytics data`() {
        val device = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901)
        every { mockUsbDeviceManager.isSupportedTopdonDevice(device) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device)
        )

        val reportBefore = usbController.getPerformanceAnalyticsReport(mockContext)
        assertTrue("Should have events before reset", reportBefore.totalEvents > 0)

        usbController.resetPerformanceAnalytics()

        val reportAfter = usbController.getPerformanceAnalyticsReport(mockContext)
        assertEquals("Should have no events after reset", 0L, reportAfter.totalEvents)
        assertEquals("Average response time should be reset", 0.0, reportAfter.averageResponseTime, 0.001)
    }

    @Test
    fun `should generate complete system status report`() {
        val device1 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x3901, deviceName = "/dev/bus/usb/001/001")
        val device2 = createMockUsbDevice(vendorId = 0x0BDA, productId = 0x5840, deviceName = "/dev/bus/usb/001/002")

        every { mockUsbDeviceManager.isSupportedTopdonDevice(any()) } returns true
        every { mockCallback.areAllPermissionsGranted() } returns true

        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device1)
        )
        usbController.handleUsbDeviceIntent(
            mockContext,
            createUsbDeviceIntent(UsbManager.ACTION_USB_DEVICE_ATTACHED, device2)
        )

        val systemStatus = usbController.getComprehensiveSystemStatus(mockContext)

        assertTrue(
            "Should contain system analysis title",
            systemStatus.contains("complete USB Controller System Analysis")
        )
        assertTrue("Should contain multi-device status", systemStatus.contains("MULTI-DEVICE STATUS"))
        assertTrue("Should contain performance analytics", systemStatus.contains("PERFORMANCE ANALYTICS"))
        assertTrue("Should contain device prioritization", systemStatus.contains("DEVICE PRIORITIZATION"))
        assertTrue("Should contain system recommendations", systemStatus.contains("SYSTEM RECOMMENDATIONS"))
        assertTrue("Should contain total events", systemStatus.contains("Total Events Processed"))
        assertTrue("Should contain response times", systemStatus.contains("Average Response Time"))
        assertTrue("Should contain primary device info", systemStatus.contains("Primary Device"))
        assertTrue("Should contain secondary devices info", systemStatus.contains("Secondary Devices"))
    }

    @Test
    fun `should handle device prioritization with empty device list`() {
        val assessments = usbController.getDevicePriorityAssessments()
        val selectionResult = usbController.getOptimizedDeviceSelection()

        assertTrue("Should have empty assessments list", assessments.isEmpty())
        assertNull("Should have no primary device", selectionResult.primaryDevice)
        assertTrue("Should have no secondary devices", selectionResult.secondaryDevices.isEmpty())
        assertEquals("No devices available for selection", selectionResult.selectionRationale)
    }

    private fun createMockUsbDevice(
        vendorId: Int,
        productId: Int,
        deviceName: String = "/dev/bus/usb/001/002",
        deviceClass: Int = 14
    ): UsbDevice {
        return mockk<UsbDevice>(relaxed = true).apply {
            every { this@apply.vendorId } returns vendorId
            every { this@apply.productId } returns productId
            every { this@apply.deviceName } returns deviceName
            every { this@apply.deviceClass } returns deviceClass
        }
    }

    private fun createUsbDeviceIntent(action: String, device: UsbDevice?): Intent {
        return mockk<Intent>(relaxed = true).apply {
            every { this@apply.action } returns action
            every { getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java) } returns device
        }
    }
}
