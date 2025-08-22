package com.multisensor.recording.recording

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BluetoothDiagnosticTest {
    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

    private lateinit var context: Context
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    @Test
    fun testBluetoothDeviceDiscovery() {
        println("[DEBUG_LOG] === Bluetooth Device Discovery Diagnostic ===")

        val bluetoothAvailable = bluetoothAdapter != null
        val bluetoothEnabled = bluetoothAdapter?.isEnabled == true

        println("[DEBUG_LOG] Bluetooth adapter available: $bluetoothAvailable")
        println("[DEBUG_LOG] Bluetooth enabled: $bluetoothEnabled")

        assert(bluetoothAvailable) { "Bluetooth adapter should be available" }
        assert(bluetoothEnabled) { "Bluetooth should be enabled for device discovery" }

        if (bluetoothEnabled) {
            try {
                val pairedDevices = bluetoothAdapter.bondedDevices
                val deviceCount = pairedDevices?.size ?: 0

                println("[DEBUG_LOG] Total paired devices: $deviceCount")

                val deviceInfo = mutableListOf<String>()
                pairedDevices?.forEachIndexed { index, device ->
                    val info =
                        "Device $index: Name='${device.name}', Address='${device.address}', Type=${device.type}, BondState=${device.bondState}"
                    println("[DEBUG_LOG] $info")
                    deviceInfo.add(info)

                    val isShimmerDevice =
                        device.name?.contains("Shimmer", ignoreCase = true) == true ||
                                device.name?.contains("RN42", ignoreCase = true) == true
                    println("[DEBUG_LOG]   Matches Shimmer criteria: $isShimmerDevice")
                    println("[DEBUG_LOG]   ---")
                }

                val shimmerDevices =
                    pairedDevices
                        ?.filter { device ->
                            device.name?.contains("Shimmer", ignoreCase = true) == true ||
                                    device.name?.contains("RN42", ignoreCase = true) == true
                        }?.map { it.address } ?: emptyList()

                val shimmerCount = shimmerDevices.size
                println("[DEBUG_LOG] Filtered Shimmer devices: $shimmerCount")
                shimmerDevices.forEach { address ->
                    println("[DEBUG_LOG]   Shimmer device: $address")
                }

                val summary = "Found $deviceCount total paired devices, $shimmerCount Shimmer devices"
                println("[DEBUG_LOG] Summary: $summary")

                if (shimmerCount == 0) {
                    val guidance =
                        "No Shimmer devices found. Check: 1) Device is paired with PIN 1234, 2) Device name contains 'Shimmer' or 'RN42', 3) Device is properly bonded"
                    println("[DEBUG_LOG] Guidance: $guidance")

                    println("[DEBUG_LOG] This is expected if no Shimmer device is properly paired")
                }
            } catch (e: SecurityException) {
                val errorMsg = "Security exception accessing Bluetooth devices: ${e.message}"
                println("[DEBUG_LOG] $errorMsg")
                throw AssertionError(errorMsg, e)
            } catch (e: Exception) {
                val errorMsg = "Exception accessing Bluetooth devices: ${e.message}"
                println("[DEBUG_LOG] $errorMsg")
                throw AssertionError(errorMsg, e)
            }
        } else {
            val errorMsg = "Bluetooth is not enabled - cannot discover devices"
            println("[DEBUG_LOG] $errorMsg")
            throw AssertionError(errorMsg)
        }

        println("[DEBUG_LOG] === End Diagnostic ===")
    }
}
