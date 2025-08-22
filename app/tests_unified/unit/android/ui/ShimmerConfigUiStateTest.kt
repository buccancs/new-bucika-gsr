package com.multisensor.recording.ui

import org.junit.Assert.*
import org.junit.Test

class ShimmerConfigUiStateTest {

    @Test
    fun `canStartScan returns true when bluetooth enabled and has permission`() {
        val state = ShimmerConfigUiState(
            isBluetoothEnabled = true,
            hasBluetoothPermission = true,
            isScanning = false,
            isLoadingConnection = false
        )

        assertTrue("[DEBUG_LOG] Should be able to start scan when bluetooth ready", state.canStartScan)
    }

    @Test
    fun `canStartScan returns false when bluetooth disabled`() {
        val state = ShimmerConfigUiState(
            isBluetoothEnabled = false,
            hasBluetoothPermission = true,
            isScanning = false,
            isLoadingConnection = false
        )

        assertFalse("[DEBUG_LOG] Should not be able to start scan when bluetooth disabled", state.canStartScan)
    }

    @Test
    fun `canStartScan returns false when no bluetooth permission`() {
        val state = ShimmerConfigUiState(
            isBluetoothEnabled = true,
            hasBluetoothPermission = false,
            isScanning = false,
            isLoadingConnection = false
        )

        assertFalse("[DEBUG_LOG] Should not be able to start scan without permission", state.canStartScan)
    }

    @Test
    fun `canStartScan returns false when already scanning`() {
        val state = ShimmerConfigUiState(
            isBluetoothEnabled = true,
            hasBluetoothPermission = true,
            isScanning = true,
            isLoadingConnection = false
        )

        assertFalse("[DEBUG_LOG] Should not be able to start scan when already scanning", state.canStartScan)
    }

    @Test
    fun `canConnectToDevice returns true when device selected and not connected`() {
        val devices = listOf(
            ShimmerDeviceItem("Shimmer3-001", "00:11:22:33:44:55", -65),
            ShimmerDeviceItem("Shimmer3-002", "00:11:22:33:44:56", -70)
        )
        val state = ShimmerConfigUiState(
            isDeviceConnected = false,
            availableDevices = devices,
            selectedDeviceIndex = 0,
            isLoadingConnection = false
        )

        assertTrue("[DEBUG_LOG] Should be able to connect to selected device", state.canConnectToDevice)
    }

    @Test
    fun `canConnectToDevice returns false when already connected`() {
        val devices = listOf(ShimmerDeviceItem("Shimmer3-001", "00:11:22:33:44:55", -65))
        val state = ShimmerConfigUiState(
            isDeviceConnected = true,
            availableDevices = devices,
            selectedDeviceIndex = 0,
            isLoadingConnection = false
        )

        assertFalse("[DEBUG_LOG] Should not be able to connect when already connected", state.canConnectToDevice)
    }

    @Test
    fun `canConnectToDevice returns false when no device selected`() {
        val devices = listOf(ShimmerDeviceItem("Shimmer3-001", "00:11:22:33:44:55", -65))
        val state = ShimmerConfigUiState(
            isDeviceConnected = false,
            availableDevices = devices,
            selectedDeviceIndex = -1,
            isLoadingConnection = false
        )

        assertFalse("[DEBUG_LOG] Should not be able to connect when no device selected", state.canConnectToDevice)
    }

    @Test
    fun `canDisconnectDevice returns true when connected and not loading`() {
        val state = ShimmerConfigUiState(
            isDeviceConnected = true,
            isLoadingConnection = false
        )

        assertTrue("[DEBUG_LOG] Should be able to disconnect when connected", state.canDisconnectDevice)
    }

    @Test
    fun `canDisconnectDevice returns false when not connected`() {
        val state = ShimmerConfigUiState(
            isDeviceConnected = false,
            isLoadingConnection = false
        )

        assertFalse("[DEBUG_LOG] Should not be able to disconnect when not connected", state.canDisconnectDevice)
    }

    @Test
    fun `canApplyConfiguration returns true when connected with sensors enabled`() {
        val state = ShimmerConfigUiState(
            isDeviceConnected = true,
            isConfiguring = false,
            isRecording = false,
            enabledSensors = setOf("accel", "gyro", "gsr")
        )

        assertTrue("[DEBUG_LOG] Should be able to apply configuration when ready", state.canApplyConfiguration)
    }

    @Test
    fun `canApplyConfiguration returns false when no sensors enabled`() {
        val state = ShimmerConfigUiState(
            isDeviceConnected = true,
            isConfiguring = false,
            isRecording = false,
            enabledSensors = emptySet()
        )

        assertFalse(
            "[DEBUG_LOG] Should not be able to apply configuration without sensors",
            state.canApplyConfiguration
        )
    }

    @Test
    fun `canStartRecording returns true when configured and ready`() {
        val state = ShimmerConfigUiState(
            isDeviceConnected = true,
            isRecording = false,
            isConfiguring = false,
            enabledSensors = setOf("gsr", "ppg")
        )

        assertTrue("[DEBUG_LOG] Should be able to start recording when configured", state.canStartRecording)
    }

    @Test
    fun `canStartRecording returns false when already recording`() {
        val state = ShimmerConfigUiState(
            isDeviceConnected = true,
            isRecording = true,
            isConfiguring = false,
            enabledSensors = setOf("gsr")
        )

        assertFalse("[DEBUG_LOG] Should not be able to start recording when already recording", state.canStartRecording)
    }

    @Test
    fun `canStopRecording returns true when recording`() {
        val state = ShimmerConfigUiState(isRecording = true)

        assertTrue("[DEBUG_LOG] Should be able to stop recording when recording", state.canStopRecording)
    }

    @Test
    fun `canStopRecording returns false when not recording`() {
        val state = ShimmerConfigUiState(isRecording = false)

        assertFalse("[DEBUG_LOG] Should not be able to stop recording when not recording", state.canStopRecording)
    }

    @Test
    fun `connectionHealthStatus returns BLUETOOTH_DISABLED when bluetooth off`() {
        val state = ShimmerConfigUiState(isBluetoothEnabled = false)

        assertEquals(
            "[DEBUG_LOG] Should return BLUETOOTH_DISABLED",
            ConnectionHealthStatus.BLUETOOTH_DISABLED, state.connectionHealthStatus
        )
    }

    @Test
    fun `connectionHealthStatus returns NO_PERMISSION when no bluetooth permission`() {
        val state = ShimmerConfigUiState(
            isBluetoothEnabled = true,
            hasBluetoothPermission = false
        )

        assertEquals(
            "[DEBUG_LOG] Should return NO_PERMISSION",
            ConnectionHealthStatus.NO_PERMISSION, state.connectionHealthStatus
        )
    }

    @Test
    fun `connectionHealthStatus returns CONNECTING when loading connection`() {
        val state = ShimmerConfigUiState(
            isBluetoothEnabled = true,
            hasBluetoothPermission = true,
            isLoadingConnection = true
        )

        assertEquals(
            "[DEBUG_LOG] Should return CONNECTING when loading",
            ConnectionHealthStatus.CONNECTING, state.connectionHealthStatus
        )
    }

    @Test
    fun `connectionHealthStatus returns EXCELLENT when connected with strong signal`() {
        val state = ShimmerConfigUiState(
            isBluetoothEnabled = true,
            hasBluetoothPermission = true,
            isDeviceConnected = true,
            signalStrength = -60
        )

        assertEquals(
            "[DEBUG_LOG] Should return EXCELLENT for strong signal",
            ConnectionHealthStatus.EXCELLENT, state.connectionHealthStatus
        )
    }

    @Test
    fun `connectionHealthStatus returns GOOD when connected with medium signal`() {
        val state = ShimmerConfigUiState(
            isBluetoothEnabled = true,
            hasBluetoothPermission = true,
            isDeviceConnected = true,
            signalStrength = -75
        )

        assertEquals(
            "[DEBUG_LOG] Should return GOOD for medium signal",
            ConnectionHealthStatus.GOOD, state.connectionHealthStatus
        )
    }

    @Test
    fun `connectionHealthStatus returns POOR when connected with weak signal`() {
        val state = ShimmerConfigUiState(
            isBluetoothEnabled = true,
            hasBluetoothPermission = true,
            isDeviceConnected = true,
            signalStrength = -90
        )

        assertEquals(
            "[DEBUG_LOG] Should return POOR for weak signal",
            ConnectionHealthStatus.POOR, state.connectionHealthStatus
        )
    }

    @Test
    fun `connectionHealthStatus returns DISCONNECTED when not connected`() {
        val state = ShimmerConfigUiState(
            isBluetoothEnabled = true,
            hasBluetoothPermission = true,
            isDeviceConnected = false
        )

        assertEquals(
            "[DEBUG_LOG] Should return DISCONNECTED when not connected",
            ConnectionHealthStatus.DISCONNECTED, state.connectionHealthStatus
        )
    }

    @Test
    fun `selectedDevice returns correct device when valid index`() {
        val devices = listOf(
            ShimmerDeviceItem("Shimmer3-001", "00:11:22:33:44:55", -65),
            ShimmerDeviceItem("Shimmer3-002", "00:11:22:33:44:56", -70)
        )
        val state = ShimmerConfigUiState(
            availableDevices = devices,
            selectedDeviceIndex = 1
        )

        val selectedDevice = state.selectedDevice
        assertNotNull("[DEBUG_LOG] Selected device should not be null", selectedDevice)
        assertEquals("[DEBUG_LOG] Should return second device", "Shimmer3-002", selectedDevice?.name)
        assertEquals("[DEBUG_LOG] MAC address should match", "00:11:22:33:44:56", selectedDevice?.macAddress)
    }

    @Test
    fun `selectedDevice returns null when invalid index`() {
        val devices = listOf(ShimmerDeviceItem("Shimmer3-001", "00:11:22:33:44:55", -65))
        val state = ShimmerConfigUiState(
            availableDevices = devices,
            selectedDeviceIndex = 5
        )

        assertNull("[DEBUG_LOG] Selected device should be null for invalid index", state.selectedDevice)
    }

    @Test
    fun `selectedDevice returns null when no devices available`() {
        val state = ShimmerConfigUiState(
            availableDevices = emptyList(),
            selectedDeviceIndex = 0
        )

        assertNull("[DEBUG_LOG] Selected device should be null when no devices", state.selectedDevice)
    }

    @Test
    fun `shimmer device item data class works correctly`() {
        val device = ShimmerDeviceItem(
            name = "Shimmer3-ABC123",
            macAddress = "00:11:22:33:44:55",
            rssi = -65,
            isConnectable = true,
            deviceType = "Shimmer3"
        )

        assertEquals("[DEBUG_LOG] Device name should match", "Shimmer3-ABC123", device.name)
        assertEquals("[DEBUG_LOG] MAC address should match", "00:11:22:33:44:55", device.macAddress)
        assertEquals("[DEBUG_LOG] RSSI should match", -65, device.rssi)
        assertTrue("[DEBUG_LOG] Should be connectable", device.isConnectable)
        assertEquals("[DEBUG_LOG] Device type should match", "Shimmer3", device.deviceType)
    }

    @Test
    fun `shimmer sensor enum values are correct`() {
        assertEquals("[DEBUG_LOG] GSR key should be correct", "gsr", ShimmerSensor.GSR.key)
        assertEquals(
            "[DEBUG_LOG] GSR display name should be correct",
            "Galvanic Skin Response",
            ShimmerSensor.GSR.displayName
        )

        assertEquals("[DEBUG_LOG] Accelerometer key should be correct", "accel", ShimmerSensor.ACCELEROMETER.key)
        assertEquals("[DEBUG_LOG] PPG key should be correct", "ppg", ShimmerSensor.PPG.key)
        assertEquals("[DEBUG_LOG] ECG key should be correct", "ecg", ShimmerSensor.ECG.key)
    }

    @Test
    fun `connection health status enum contains all expected values`() {
        val statuses = ConnectionHealthStatus.values()

        assertTrue(
            "[DEBUG_LOG] Should contain BLUETOOTH_DISABLED",
            statuses.contains(ConnectionHealthStatus.BLUETOOTH_DISABLED)
        )
        assertTrue("[DEBUG_LOG] Should contain NO_PERMISSION", statuses.contains(ConnectionHealthStatus.NO_PERMISSION))
        assertTrue("[DEBUG_LOG] Should contain DISCONNECTED", statuses.contains(ConnectionHealthStatus.DISCONNECTED))
        assertTrue("[DEBUG_LOG] Should contain CONNECTING", statuses.contains(ConnectionHealthStatus.CONNECTING))
        assertTrue("[DEBUG_LOG] Should contain POOR", statuses.contains(ConnectionHealthStatus.POOR))
        assertTrue("[DEBUG_LOG] Should contain GOOD", statuses.contains(ConnectionHealthStatus.GOOD))
        assertTrue("[DEBUG_LOG] Should contain EXCELLENT", statuses.contains(ConnectionHealthStatus.EXCELLENT))
    }
}
