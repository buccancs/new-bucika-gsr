# TC001 Troubleshooting Guide

## Overview

This comprehensive troubleshooting guide addresses common issues, diagnostic procedures, and solutions for the Topdon TC001 thermal imaging integration within the BucikaGSR system. This guide provides systematic approaches to identifying and resolving hardware connectivity, software configuration, and performance issues.

## Table of Contents

1. [Quick Diagnostics](#quick-diagnostics)
2. [USB Connection Issues](#usb-connection-issues)
3. [Thermal Imaging Problems](#thermal-imaging-problems)
4. [Temperature Accuracy Issues](#temperature-accuracy-issues)
5. [Performance Problems](#performance-problems)
6. [Data Recording Issues](#data-recording-issues)
7. [UI and Display Issues](#ui-and-display-issues)
8. [Advanced Diagnostics](#advanced-diagnostics)
9. [Error Code Reference](#error-code-reference)
10. [Support and Resources](#support-and-resources)

## Quick Diagnostics

### System Health Check

#### Basic System Verification
```kotlin
// Quick system diagnostics
class TC001SystemDiagnostics {
    
    fun runQuickDiagnostics(context: Context): DiagnosticResult {
        val results = DiagnosticResult()
        
        // Check USB host support
        results.usbHostSupport = checkUSBHostSupport(context)
        
        // Check OpenCV status
        results.openCVStatus = checkOpenCVStatus()
        
        // Check thermal camera connectivity
        results.thermalCameraConnected = checkThermalCameraConnection(context)
        
        // Check storage availability
        results.storageAvailable = checkStorageAvailability(context)
        
        // Check permissions
        results.permissions = checkRequiredPermissions(context)
        
        return results
    }
    
    private fun checkUSBHostSupport(context: Context): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
    }
    
    private fun checkOpenCVStatus(): Boolean {
        return try {
            OpenCVLoader.initDebug()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun checkThermalCameraConnection(context: Context): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return findTC001Device(usbManager) != null
    }
    
    private fun findTC001Device(usbManager: UsbManager): UsbDevice? {
        return usbManager.deviceList.values.find { device ->
            device.vendorId == TC001_VENDOR_ID && device.productId == TC001_PRODUCT_ID
        }
    }
}
```

#### Diagnostic Results Display
```kotlin
// Display diagnostic results to user
fun showDiagnosticResults(context: Context, results: DiagnosticResult) {
    val diagnosticText = buildString {
        appendLine("=== TC001 System Diagnostics ===")
        appendLine()
        appendLine("USB Host Support: ${if (results.usbHostSupport) "✓ OK" else "✗ FAILED"}")
        appendLine("OpenCV Status: ${if (results.openCVStatus) "✓ OK" else "✗ FAILED"}")
        appendLine("Thermal Camera: ${if (results.thermalCameraConnected) "✓ Connected" else "✗ Disconnected"}")
        appendLine("Storage Available: ${if (results.storageAvailable) "✓ OK" else "✗ INSUFFICIENT"}")
        appendLine("Permissions: ${if (results.permissions) "✓ Granted" else "✗ MISSING"}")
        
        if (!results.allGood()) {
            appendLine()
            appendLine("Issues Detected:")
            if (!results.usbHostSupport) appendLine("- Enable USB Host support in device settings")
            if (!results.openCVStatus) appendLine("- OpenCV library not loaded properly")
            if (!results.thermalCameraConnected) appendLine("- TC001 camera not connected or not recognized")
            if (!results.storageAvailable) appendLine("- Insufficient storage space")
            if (!results.permissions) appendLine("- Missing USB or storage permissions")
        }
    }
    
    Log.d("TC001Diagnostics", diagnosticText)
    
    // Show results in debug dialog if in debug mode
    if (BuildConfig.DEBUG) {
        AlertDialog.Builder(context)
            .setTitle("TC001 Diagnostics")
            .setMessage(diagnosticText)
            .setPositiveButton("OK", null)
            .show()
    }
}
```

## USB Connection Issues

### Issue: TC001 Device Not Detected

#### Symptoms
- No USB devices found in device enumeration
- App shows "Device not connected"
- No thermal imaging feed available

#### Diagnostic Steps
```kotlin
fun diagnoseUSBDetection(context: Context) {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    val deviceList = usbManager.deviceList
    
    Log.d("USB_DEBUG", "=== USB Device Detection Diagnostics ===")
    Log.d("USB_DEBUG", "Total USB devices found: ${deviceList.size}")
    
    if (deviceList.isEmpty()) {
        Log.w("USB_DEBUG", "No USB devices detected!")
        Log.w("USB_DEBUG", "Check:")
        Log.w("USB_DEBUG", "1. USB OTG cable is properly connected")
        Log.w("USB_DEBUG", "2. TC001 device is powered on")
        Log.w("USB_DEBUG", "3. Android device supports USB host mode")
        return
    }
    
    // List all detected USB devices
    deviceList.values.forEachIndexed { index, device ->
        Log.d("USB_DEBUG", "Device ${index + 1}:")
        Log.d("USB_DEBUG", "  Name: ${device.deviceName}")
        Log.d("USB_DEBUG", "  Vendor ID: 0x${Integer.toHexString(device.vendorId)}")
        Log.d("USB_DEBUG", "  Product ID: 0x${Integer.toHexString(device.productId)}")
        Log.d("USB_DEBUG", "  Class: ${device.deviceClass}")
        Log.d("USB_DEBUG", "  Subclass: ${device.deviceSubclass}")
        Log.d("USB_DEBUG", "  Protocol: ${device.deviceProtocol}")
        Log.d("USB_DEBUG", "  Interfaces: ${device.interfaceCount}")
        Log.d("USB_DEBUG", "  Has Permission: ${usbManager.hasPermission(device)}")
        
        // Check if this might be TC001 with different IDs
        if (device.deviceClass == USB_CLASS_VIDEO || 
            device.deviceName.contains("TC001", ignoreCase = true) ||
            device.vendorId == TC001_VENDOR_ID) {
            Log.i("USB_DEBUG", "  *** Potential TC001 device detected ***")
        }
    }
    
    // Check for TC001 specifically
    val tc001Device = findTC001Device(usbManager)
    if (tc001Device == null) {
        Log.w("USB_DEBUG", "TC001 device not found in device list")
        Log.w("USB_DEBUG", "Expected: VID=0x${Integer.toHexString(TC001_VENDOR_ID)}, PID=0x${Integer.toHexString(TC001_PRODUCT_ID)}")
    } else {
        Log.i("USB_DEBUG", "TC001 device found successfully")
    }
}
```

#### Solutions

**Solution 1: Check Physical Connections**
```kotlin
fun checkPhysicalConnections(): List<String> {
    return listOf(
        "1. Verify USB OTG cable is not damaged",
        "2. Ensure TC001 device is powered on (check LED indicators)", 
        "3. Try different USB ports/cables",
        "4. Confirm Android device supports USB host mode",
        "5. Check TC001 power supply if using external power"
    )
}
```

**Solution 2: Update Device Filter**
```xml
<!-- If TC001 uses different vendor/product IDs -->
<resources>
    <!-- Original TC001 configuration -->
    <usb-device vendor-id="1234" product-id="5678" />
    
    <!-- Alternative configurations for different TC001 variants -->
    <usb-device vendor-id="1234" product-id="5679" />
    <usb-device vendor-id="1234" product-id="567A" />
    
    <!-- Generic thermal camera class -->
    <usb-device class="14" />
</resources>
```

**Solution 3: Runtime Device Discovery**
```kotlin
class FlexibleTC001Detector {
    
    fun findTC001DeviceFlexible(usbManager: UsbManager): UsbDevice? {
        return usbManager.deviceList.values.find { device ->
            // Check known TC001 IDs
            if (device.vendorId == TC001_VENDOR_ID && 
                device.productId in listOf(TC001_PRODUCT_ID, TC001_PRODUCT_ID_ALT)) {
                return@find true
            }
            
            // Check device class (Video/UVC devices)
            if (device.deviceClass == USB_CLASS_VIDEO) {
                Log.d("TC001", "Found UVC device: ${device.deviceName}")
                return@find true
            }
            
            // Check device name patterns
            val deviceName = device.deviceName?.lowercase() ?: ""
            if (deviceName.contains("tc001") || 
                deviceName.contains("topdon") ||
                deviceName.contains("thermal")) {
                Log.d("TC001", "Found potential TC001 by name: ${device.deviceName}")
                return@find true
            }
            
            false
        }
    }
}
```

### Issue: USB Permission Not Granted

#### Symptoms
- Device detected but connection fails
- "Permission denied" error messages
- USB permission dialog not appearing

#### Diagnostic Steps
```kotlin
fun diagnoseUSBPermissions(context: Context, device: UsbDevice) {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    
    Log.d("USB_PERM", "=== USB Permission Diagnostics ===")
    Log.d("USB_PERM", "Device: ${device.deviceName}")
    Log.d("USB_PERM", "Has Permission: ${usbManager.hasPermission(device)}")
    
    if (!usbManager.hasPermission(device)) {
        Log.w("USB_PERM", "Permission not granted for device")
        
        // Check if permission request is pending
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        Log.i("USB_PERM", "Requesting USB permission...")
        usbManager.requestPermission(device, pendingIntent)
    }
}
```

#### Solutions

**Solution 1: Proper Permission Handling**
```kotlin
class USBPermissionHandler {
    private val ACTION_USB_PERMISSION = "com.topdon.tc001.USB_PERMISSION"
    
    fun requestUSBPermission(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        
        if (usbManager.hasPermission(device)) {
            // Permission already granted
            connectDevice(device)
            return
        }
        
        // Register broadcast receiver for permission result
        val permissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    
                    if (granted && device != null) {
                        Log.i("USB_PERM", "USB permission granted")
                        connectDevice(device)
                    } else {
                        Log.w("USB_PERM", "USB permission denied by user")
                        showPermissionDeniedMessage()
                    }
                    
                    context.unregisterReceiver(this)
                }
            }
        }
        
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(permissionReceiver, filter)
        
        // Request permission
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }
    
    private fun showPermissionDeniedMessage() {
        // Show user-friendly message explaining why permission is needed
        Toast.makeText(context, 
            "USB permission is required for thermal camera access", 
            Toast.LENGTH_LONG).show()
    }
}
```

**Solution 2: Auto-Permission Grant (Rooted Devices)**
```kotlin
fun autoGrantUSBPermission(device: UsbDevice) {
    try {
        // Only for rooted devices or system apps
        val command = "pm grant com.topdon.tc001 android.permission.USB_PERMISSION"
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        process.waitFor()
        
        Log.i("USB_PERM", "Attempted auto-grant USB permission")
    } catch (e: Exception) {
        Log.w("USB_PERM", "Auto-grant failed, device may not be rooted", e)
    }
}
```

## Thermal Imaging Problems

### Issue: No Thermal Image Display

#### Symptoms
- Black screen in thermal view
- No thermal frames received
- Frame callback not triggered

#### Diagnostic Steps
```kotlin
fun diagnoseThermalImaging(thermalCamera: IRUVCTC) {
    Log.d("THERMAL", "=== Thermal Imaging Diagnostics ===")
    
    // Check connection status
    val isConnected = thermalCamera.isConnected()
    Log.d("THERMAL", "Camera Connected: $isConnected")
    
    if (!isConnected) {
        Log.e("THERMAL", "Camera not connected - cannot start imaging")
        return
    }
    
    // Check streaming status
    val isStreaming = thermalCamera.isStreaming()
    Log.d("THERMAL", "Streaming Active: $isStreaming")
    
    if (!isStreaming) {
        Log.w("THERMAL", "Streaming not active - attempting to start")
        val streamStarted = thermalCamera.startThermalStream()
        Log.d("THERMAL", "Stream Start Result: $streamStarted")
    }
    
    // Monitor frame callbacks for 10 seconds
    var frameCount = 0
    val frameMonitor = object : IFrameCallback {
        override fun onThermalFrame(bitmap: Bitmap?, tempData: ByteArray?, timestamp: Long) {
            frameCount++
            Log.d("THERMAL", "Frame received: $frameCount at $timestamp")
            
            if (bitmap != null) {
                Log.d("THERMAL", "Bitmap size: ${bitmap.width}x${bitmap.height}")
            } else {
                Log.w("THERMAL", "Bitmap is null")
            }
            
            if (tempData != null) {
                Log.d("THERMAL", "Temperature data size: ${tempData.size} bytes")
            } else {
                Log.w("THERMAL", "Temperature data is null")
            }
        }
        
        override fun onError(errorMessage: String, errorCode: Int) {
            Log.e("THERMAL", "Frame error: $errorMessage (Code: $errorCode)")
        }
    }
    
    thermalCamera.setFrameCallback(frameMonitor)
    
    // Wait and report results
    Handler().postDelayed({
        Log.d("THERMAL", "Frame monitoring complete: $frameCount frames in 10 seconds")
        if (frameCount == 0) {
            Log.e("THERMAL", "No frames received - check camera connection and configuration")
        } else if (frameCount < 200) { // Less than 20 FPS
            Log.w("THERMAL", "Low frame rate detected: ${frameCount / 10.0} FPS")
        }
    }, 10000)
}
```

#### Solutions

**Solution 1: Restart Thermal Stream**
```kotlin
fun restartThermalStream(thermalCamera: IRUVCTC): Boolean {
    try {
        Log.i("THERMAL", "Restarting thermal stream...")
        
        // Stop existing stream
        thermalCamera.stopThermalStream()
        Thread.sleep(1000) // Wait for cleanup
        
        // Start new stream
        val success = thermalCamera.startThermalStream()
        
        if (success) {
            Log.i("THERMAL", "Thermal stream restarted successfully")
        } else {
            Log.e("THERMAL", "Failed to restart thermal stream")
        }
        
        return success
    } catch (e: Exception) {
        Log.e("THERMAL", "Exception during stream restart", e)
        return false
    }
}
```

**Solution 2: Reinitialize Camera Connection**
```kotlin
fun reinitializeCameraConnection(context: Context, device: UsbDevice): IRUVCTC? {
    return try {
        Log.i("THERMAL", "Reinitializing camera connection...")
        
        val frameCallback = object : IFrameCallback {
            override fun onThermalFrame(bitmap: Bitmap?, tempData: ByteArray?, timestamp: Long) {
                // Handle thermal frames
                handleThermalFrame(bitmap, tempData, timestamp)
            }
            
            override fun onError(errorMessage: String, errorCode: Int) {
                Log.e("THERMAL", "Frame error: $errorMessage")
            }
        }
        
        val thermalCamera = IRUVCTC(context, frameCallback)
        
        if (thermalCamera.connectUSBDevice(device)) {
            Thread.sleep(2000) // Wait for connection
            
            if (thermalCamera.startThermalStream()) {
                Log.i("THERMAL", "Camera reinitialized successfully")
                thermalCamera
            } else {
                Log.e("THERMAL", "Failed to start thermal stream after reconnection")
                null
            }
        } else {
            Log.e("THERMAL", "Failed to reconnect USB device")
            null
        }
    } catch (e: Exception) {
        Log.e("THERMAL", "Exception during camera reinitialization", e)
        null
    }
}
```

### Issue: Thermal Image Distorted or Corrupted

#### Symptoms  
- Thermal image shows random colors/patterns
- Temperature readings are extreme values
- Visual artifacts or corruption in thermal display

#### Diagnostic Steps
```kotlin
fun diagnoseThermalImageCorruption(tempData: ByteArray?) {
    Log.d("THERMAL_DATA", "=== Thermal Data Corruption Diagnostics ===")
    
    if (tempData == null) {
        Log.e("THERMAL_DATA", "Temperature data is null")
        return
    }
    
    Log.d("THERMAL_DATA", "Temperature data size: ${tempData.size} bytes")
    Log.d("THERMAL_DATA", "Expected size: ${256 * 192 * 2} bytes")
    
    if (tempData.size != 256 * 192 * 2) {
        Log.e("THERMAL_DATA", "Temperature data size mismatch!")
        return
    }
    
    // Analyze temperature data
    val temperatures = mutableListOf<Float>()
    for (i in tempData.indices step 2) {
        if (i + 1 < tempData.size) {
            val rawValue = (tempData[i + 1].toInt() shl 8) or (tempData[i].toInt() and 0xFF)
            val temperature = (rawValue / 64.0f) - 273.15f
            temperatures.add(temperature)
        }
    }
    
    if (temperatures.isEmpty()) {
        Log.e("THERMAL_DATA", "No temperature values extracted")
        return
    }
    
    val minTemp = temperatures.minOrNull() ?: 0f
    val maxTemp = temperatures.maxOrNull() ?: 0f
    val avgTemp = temperatures.average().toFloat()
    
    Log.d("THERMAL_DATA", "Temperature range: ${minTemp}°C to ${maxTemp}°C")
    Log.d("THERMAL_DATA", "Average temperature: ${avgTemp}°C")
    
    // Check for corruption indicators
    if (minTemp < -100f || maxTemp > 200f) {
        Log.w("THERMAL_DATA", "Extreme temperature values detected - possible corruption")
    }
    
    if (maxTemp - minTemp > 300f) {
        Log.w("THERMAL_DATA", "Unusually large temperature range - possible corruption")
    }
    
    // Check for data patterns that indicate corruption
    val firstTenValues = temperatures.take(10)
    if (firstTenValues.all { it == firstTenValues[0] }) {
        Log.w("THERMAL_DATA", "First 10 temperature values are identical - possible stuck data")
    }
}
```

#### Solutions

**Solution 1: Data Validation and Filtering**
```kotlin
class ThermalDataValidator {
    
    fun validateAndCleanThermalData(tempData: ByteArray): ByteArray? {
        if (tempData.size != 256 * 192 * 2) {
            Log.e("VALIDATOR", "Invalid data size: ${tempData.size}")
            return null
        }
        
        val cleanedData = tempData.copyOf()
        var corruptedPixels = 0
        
        for (i in cleanedData.indices step 2) {
            if (i + 1 < cleanedData.size) {
                val rawValue = (cleanedData[i + 1].toInt() shl 8) or (cleanedData[i].toInt() and 0xFF)
                val temperature = (rawValue / 64.0f) - 273.15f
                
                // Check if temperature is within reasonable range
                if (temperature < -50f || temperature > 150f) {
                    // Replace with interpolated value from neighbors
                    val replacement = interpolateTemperatureValue(cleanedData, i, 256, 192)
                    cleanedData[i] = (replacement and 0xFF).toByte()
                    cleanedData[i + 1] = ((replacement shr 8) and 0xFF).toByte()
                    corruptedPixels++
                }
            }
        }
        
        if (corruptedPixels > 0) {
            Log.w("VALIDATOR", "Cleaned $corruptedPixels corrupted pixels")
        }
        
        return cleanedData
    }
    
    private fun interpolateTemperatureValue(data: ByteArray, pixelIndex: Int, width: Int, height: Int): Int {
        // Simple interpolation using neighbor pixels
        val pixelNumber = pixelIndex / 2
        val x = pixelNumber % width
        val y = pixelNumber / width
        
        val neighbors = mutableListOf<Int>()
        
        // Check 4-connected neighbors
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                
                val nx = x + dx
                val ny = y + dy
                
                if (nx in 0 until width && ny in 0 until height) {
                    val neighborIndex = (ny * width + nx) * 2
                    if (neighborIndex + 1 < data.size) {
                        val neighborValue = (data[neighborIndex + 1].toInt() shl 8) or 
                                          (data[neighborIndex].toInt() and 0xFF)
                        neighbors.add(neighborValue)
                    }
                }
            }
        }
        
        // Return average of valid neighbors, or default value if no neighbors
        return if (neighbors.isNotEmpty()) {
            neighbors.average().toInt()
        } else {
            ((25.0f + 273.15f) * 64.0f).toInt() // Default to 25°C
        }
    }
}
```

**Solution 2: Hardware Reset**
```kotlin
fun resetTC001Hardware(thermalCamera: IRUVCTC, context: Context): Boolean {
    return try {
        Log.i("THERMAL", "Performing hardware reset...")
        
        // Stop thermal stream
        thermalCamera.stopThermalStream()
        
        // Disconnect USB device
        thermalCamera.disconnectUSBDevice()
        
        // Wait for device reset
        Thread.sleep(5000)
        
        // Find and reconnect device
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val device = findTC001Device(usbManager)
        
        if (device != null && usbManager.hasPermission(device)) {
            val reconnected = thermalCamera.connectUSBDevice(device)
            if (reconnected) {
                Thread.sleep(2000)
                return thermalCamera.startThermalStream()
            }
        }
        
        false
    } catch (e: Exception) {
        Log.e("THERMAL", "Hardware reset failed", e)
        false
    }
}
```

## Temperature Accuracy Issues

### Issue: Inaccurate Temperature Readings

#### Symptoms
- Temperature readings consistently too high or too low
- Temperature values don't match expected measurements
- Large discrepancies between different measurement points

#### Diagnostic Steps
```kotlin
fun diagnoseTemperatureAccuracy() {
    Log.d("TEMP_ACCURACY", "=== Temperature Accuracy Diagnostics ===")
    
    // Check current thermal parameters
    val config = ThermalSettings.loadThermalConfig(context)
    Log.d("TEMP_ACCURACY", "Current settings:")
    Log.d("TEMP_ACCURACY", "  Emissivity: ${config.emissivity}")
    Log.d("TEMP_ACCURACY", "  Ambient Temperature: ${config.ambientTemperature}°C")
    Log.d("TEMP_ACCURACY", "  Distance: ${config.distance}m")
    Log.d("TEMP_ACCURACY", "  Humidity: ${config.humidity}%")
    
    // Validate parameter ranges
    if (config.emissivity < 0.1f || config.emissivity > 1.0f) {
        Log.w("TEMP_ACCURACY", "Emissivity out of valid range: ${config.emissivity}")
    }
    
    if (kotlin.math.abs(config.ambientTemperature) > 100f) {
        Log.w("TEMP_ACCURACY", "Ambient temperature seems extreme: ${config.ambientTemperature}°C")
    }
    
    if (config.distance <= 0f || config.distance > 100f) {
        Log.w("TEMP_ACCURACY", "Distance setting unusual: ${config.distance}m")
    }
    
    // Test with known temperature source
    performCalibrationTest()
}

fun performCalibrationTest() {
    Log.d("TEMP_ACCURACY", "=== Calibration Test ===")
    Log.d("TEMP_ACCURACY", "Point thermal camera at a known temperature source:")
    Log.d("TEMP_ACCURACY", "1. Room temperature object (~25°C)")
    Log.d("TEMP_ACCURACY", "2. Hot coffee cup (~60-70°C)")
    Log.d("TEMP_ACCURACY", "3. Ice water (~0°C)")
    Log.d("TEMP_ACCURACY", "Compare readings with reference thermometer")
}
```

#### Solutions

**Solution 1: Recalibrate Thermal Parameters**
```kotlin
class ThermalCalibrationManager {
    
    fun calibrateThermalParameters(context: Context) {
        // Guide user through calibration process
        showCalibrationWizard(context)
    }
    
    private fun showCalibrationWizard(context: Context) {
        val calibrationSteps = listOf(
            CalibrationStep("Step 1: Set Emissivity", 
                "Set emissivity based on target material:\n" +
                "• Human skin: 0.98\n" +
                "• Water: 0.96\n" +
                "• Metal (oxidized): 0.80\n" +
                "• Plastic: 0.90-0.95\n" +
                "• Wood: 0.85-0.90"),
            
            CalibrationStep("Step 2: Measure Ambient Temperature",
                "Use a reference thermometer to measure ambient temperature accurately"),
            
            CalibrationStep("Step 3: Set Distance",
                "Measure the distance from camera to target object"),
            
            CalibrationStep("Step 4: Verify Calibration",
                "Point camera at known temperature source and verify reading")
        )
        
        showCalibrationDialog(context, calibrationSteps)
    }
    
    fun applyTemperatureCorrection(rawTemp: Float, correctionFactor: Float): Float {
        // Apply linear correction based on calibration
        return rawTemp * correctionFactor
    }
    
    fun calculateEmissivityCorrection(material: MaterialType): Float {
        return when (material) {
            MaterialType.HUMAN_SKIN -> 0.98f
            MaterialType.METAL_PAINTED -> 0.90f
            MaterialType.METAL_OXIDIZED -> 0.80f
            MaterialType.PLASTIC -> 0.92f
            MaterialType.WOOD -> 0.87f
            MaterialType.WATER -> 0.96f
            MaterialType.GLASS -> 0.85f
            MaterialType.FABRIC -> 0.90f
            else -> 0.95f // Default
        }
    }
}
```

**Solution 2: Advanced Temperature Correction**
```kotlin
class AdvancedTemperatureCorrection {
    
    data class CorrectionParameters(
        val emissivity: Float,
        val ambientTemp: Float,
        val distance: Float,
        val humidity: Float,
        val atmosphericTransmission: Float = 1.0f
    )
    
    fun correctTemperatureReading(rawTemp: Float, params: CorrectionParameters): Float {
        // Apply atmospheric correction for distance
        val atmosphericCorrection = calculateAtmosphericCorrection(params.distance, params.humidity)
        
        // Apply emissivity correction
        val emissivityCorrection = calculateEmissivityCorrection(rawTemp, params.emissivity, params.ambientTemp)
        
        // Combine corrections
        var correctedTemp = rawTemp
        correctedTemp = applyAtmosphericCorrection(correctedTemp, atmosphericCorrection)
        correctedTemp = applyEmissivityCorrection(correctedTemp, emissivityCorrection, params.ambientTemp)
        
        return correctedTemp
    }
    
    private fun calculateAtmosphericCorrection(distance: Float, humidity: Float): Float {
        // Simplified atmospheric transmission model
        val transmissionLoss = 0.02f * distance * (humidity / 100f)
        return 1.0f - transmissionLoss.coerceAtMost(0.2f)
    }
    
    private fun calculateEmissivityCorrection(temp: Float, emissivity: Float, ambientTemp: Float): Float {
        // Stefan-Boltzmann law correction
        val tempK = temp + 273.15f
        val ambientK = ambientTemp + 273.15f
        
        val corrected = kotlin.math.pow(
            (kotlin.math.pow(tempK, 4.0) - (1 - emissivity) * kotlin.math.pow(ambientK, 4.0)) / emissivity,
            0.25
        ).toFloat()
        
        return corrected - 273.15f
    }
    
    private fun applyAtmosphericCorrection(temp: Float, transmission: Float): Float {
        return temp / transmission
    }
    
    private fun applyEmissivityCorrection(temp: Float, correctedTemp: Float, ambientTemp: Float): Float {
        return correctedTemp
    }
}
```

## Performance Problems

### Issue: Poor Frame Rate

#### Symptoms
- Thermal image updates slowly
- Frame rate below 20 FPS
- UI becomes unresponsive during thermal imaging

#### Diagnostic Steps
```kotlin
class PerformanceDiagnostics {
    
    private var frameCount = 0
    private var lastFrameTime = 0L
    private val frameTimes = mutableListOf<Long>()
    
    fun monitorFrameRate(durationMs: Long = 10000) {
        frameCount = 0
        frameTimes.clear()
        lastFrameTime = System.currentTimeMillis()
        
        Log.d("PERFORMANCE", "Starting frame rate monitoring for ${durationMs}ms")
        
        Handler().postDelayed({
            val totalTime = System.currentTimeMillis() - lastFrameTime + durationMs
            val averageFPS = (frameCount * 1000f) / totalTime
            
            Log.d("PERFORMANCE", "Frame rate monitoring results:")
            Log.d("PERFORMANCE", "  Total frames: $frameCount")
            Log.d("PERFORMANCE", "  Duration: ${totalTime}ms")
            Log.d("PERFORMANCE", "  Average FPS: $averageFPS")
            
            if (frameTimes.isNotEmpty()) {
                val avgFrameTime = frameTimes.average()
                val minFrameTime = frameTimes.minOrNull() ?: 0
                val maxFrameTime = frameTimes.maxOrNull() ?: 0
                
                Log.d("PERFORMANCE", "  Frame timing:")
                Log.d("PERFORMANCE", "    Average: ${avgFrameTime}ms")
                Log.d("PERFORMANCE", "    Min: ${minFrameTime}ms")
                Log.d("PERFORMANCE", "    Max: ${maxFrameTime}ms")
            }
            
            if (averageFPS < 15f) {
                Log.w("PERFORMANCE", "LOW FRAME RATE DETECTED!")
                suggestPerformanceImprovements()
            }
        }, durationMs)
    }
    
    fun onFrameReceived() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        
        if (lastFrameTime > 0) {
            val frameTime = currentTime - lastFrameTime
            frameTimes.add(frameTime)
        }
        
        lastFrameTime = currentTime
    }
    
    private fun suggestPerformanceImprovements() {
        Log.i("PERFORMANCE", "Performance improvement suggestions:")
        Log.i("PERFORMANCE", "1. Reduce thermal processing complexity")
        Log.i("PERFORMANCE", "2. Use background threads for image processing")
        Log.i("PERFORMANCE", "3. Reduce display resolution/scale")
        Log.i("PERFORMANCE", "4. Close unnecessary apps")
        Log.i("PERFORMANCE", "5. Check device thermal throttling")
    }
    
    fun measureMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        Log.d("PERFORMANCE", "Memory usage:")
        Log.d("PERFORMANCE", "  Used: ${usedMemory / 1024 / 1024}MB")
        Log.d("PERFORMANCE", "  Free: ${freeMemory / 1024 / 1024}MB")
        Log.d("PERFORMANCE", "  Total: ${totalMemory / 1024 / 1024}MB")
        Log.d("PERFORMANCE", "  Max: ${maxMemory / 1024 / 1024}MB")
        
        val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100f
        Log.d("PERFORMANCE", "  Usage: ${memoryUsagePercent.toInt()}%")
        
        if (memoryUsagePercent > 80f) {
            Log.w("PERFORMANCE", "HIGH MEMORY USAGE DETECTED!")
            Log.w("PERFORMANCE", "Consider reducing image cache size or forcing garbage collection")
        }
    }
}
```

#### Solutions

**Solution 1: Optimize Thermal Processing**
```kotlin
class OptimizedThermalProcessor {
    
    private val processingExecutor = Executors.newSingleThreadExecutor()
    private val bitmapPool = BitmapPool(maxSize = 10)
    
    fun processFrameOptimized(tempData: ByteArray, callback: (Bitmap) -> Unit) {
        processingExecutor.execute {
            val bitmap = createOptimizedThermalBitmap(tempData)
            
            // Update UI on main thread
            Handler(Looper.getMainLooper()).post {
                callback(bitmap)
            }
        }
    }
    
    private fun createOptimizedThermalBitmap(tempData: ByteArray): Bitmap {
        // Get recycled bitmap from pool or create new one
        val bitmap = bitmapPool.getBitmap() ?: Bitmap.createBitmap(256, 192, Bitmap.Config.RGB_565)
        
        // Use more efficient bitmap configuration
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = false // Disable antialiasing for performance
        }
        
        // Process temperature data with reduced precision for speed
        processTemperatureDataFast(tempData, canvas, paint)
        
        return bitmap
    }
    
    private fun processTemperatureDataFast(tempData: ByteArray, canvas: Canvas, paint: Paint) {
        // Process every 4th pixel for speed (can interpolate others if needed)
        val skipPixels = 2
        val width = 256
        val height = 192
        
        for (y in 0 until height step skipPixels) {
            for (x in 0 until width step skipPixels) {
                val index = (y * width + x) * 2
                if (index + 1 < tempData.size) {
                    val rawValue = (tempData[index + 1].toInt() shl 8) or (tempData[index].toInt() and 0xFF)
                    val temp = (rawValue / 64.0f) - 273.15f
                    
                    val color = temperatureToColorFast(temp)
                    paint.color = color
                    
                    // Draw larger pixels to compensate for skipping
                    canvas.drawRect(
                        x.toFloat(), y.toFloat(),
                        (x + skipPixels).toFloat(), (y + skipPixels).toFloat(),
                        paint
                    )
                }
            }
        }
    }
    
    private fun temperatureToColorFast(temp: Float): Int {
        // Fast color mapping using lookup table
        val tempIndex = ((temp + 20f) * 2f).toInt().coerceIn(0, 199) // -20°C to 80°C range
        return ColorLookupTable.IRON_BOW[tempIndex]
    }
}

class BitmapPool(private val maxSize: Int) {
    private val pool = mutableListOf<Bitmap>()
    
    fun getBitmap(): Bitmap? {
        return if (pool.isNotEmpty()) {
            pool.removeAt(pool.size - 1)
        } else null
    }
    
    fun returnBitmap(bitmap: Bitmap) {
        if (pool.size < maxSize && !bitmap.isRecycled) {
            pool.add(bitmap)
        }
    }
}
```

**Solution 2: Threading and Background Processing**
```kotlin
class ThermalThreadManager {
    
    private val thermalProcessingThread = HandlerThread("ThermalProcessing").apply { start() }
    private val processingHandler = Handler(thermalProcessingThread.looper)
    private val uiHandler = Handler(Looper.getMainLooper())
    
    fun processFrameInBackground(tempData: ByteArray, timestamp: Long, callback: (Bitmap, ByteArray) -> Unit) {
        processingHandler.post {
            // Heavy processing on background thread
            val processedBitmap = processTemperatureData(tempData)
            val filteredData = applyTemperatureFiltering(tempData)
            
            // Switch to UI thread for callback
            uiHandler.post {
                callback(processedBitmap, filteredData)
            }
        }
    }
    
    private fun processTemperatureData(tempData: ByteArray): Bitmap {
        // CPU-intensive thermal processing
        return OpencvTools.convertThermalToBitmap(tempData, 256, 192)
    }
    
    private fun applyTemperatureFiltering(tempData: ByteArray): ByteArray {
        // Apply noise reduction and smoothing
        return ThermalDataValidator().validateAndCleanThermalData(tempData) ?: tempData
    }
    
    fun shutdown() {
        thermalProcessingThread.quitSafely()
    }
}
```

## Data Recording Issues

### Issue: Recording Failures

#### Symptoms
- Recording does not start
- Recording stops unexpectedly
- Recorded files are corrupted or incomplete

#### Diagnostic Steps
```kotlin
class RecordingDiagnostics {
    
    fun diagnoseRecordingIssues(context: Context) {
        Log.d("RECORDING", "=== Recording Diagnostics ===")
        
        // Check storage availability
        val storageInfo = checkStorageSpace(context)
        Log.d("RECORDING", "Storage available: ${storageInfo.availableBytes / 1024 / 1024}MB")
        
        if (storageInfo.availableBytes < 100 * 1024 * 1024) { // Less than 100MB
            Log.w("RECORDING", "Low storage space available")
        }
        
        // Check write permissions
        val hasWritePermission = context.checkSelfPermission(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d("RECORDING", "Write permission granted: $hasWritePermission")
        
        // Check recording directory
        val recordingDir = File(context.filesDir, "thermal_recordings")
        if (!recordingDir.exists()) {
            Log.d("RECORDING", "Creating recording directory: ${recordingDir.absolutePath}")
            val created = recordingDir.mkdirs()
            Log.d("RECORDING", "Directory creation success: $created")
        }
        
        // Test file write
        testFileWrite(recordingDir)
    }
    
    private fun testFileWrite(directory: File) {
        try {
            val testFile = File(directory, "test_write.tmp")
            testFile.writeText("Test recording functionality")
            
            val canRead = testFile.canRead()
            val fileSize = testFile.length()
            
            Log.d("RECORDING", "Test file write successful")
            Log.d("RECORDING", "Can read test file: $canRead")
            Log.d("RECORDING", "Test file size: $fileSize bytes")
            
            testFile.delete()
        } catch (e: Exception) {
            Log.e("RECORDING", "Test file write failed", e)
        }
    }
    
    private fun checkStorageSpace(context: Context): StatFs {
        val path = context.filesDir.absolutePath
        return StatFs(path)
    }
}
```

#### Solutions

**Solution 1: Robust Recording Manager**
```kotlin
class RobustThermalRecorder {
    
    private var isRecording = false
    private var recordingFile: File? = null
    private var fileWriter: BufferedWriter? = null
    private val recordingQueue = LinkedBlockingQueue<RecordingFrame>()
    private var recordingThread: Thread? = null
    
    fun startRecording(context: Context, sessionId: String): Boolean {
        return try {
            if (isRecording) {
                Log.w("RECORDER", "Recording already in progress")
                return false
            }
            
            // Check prerequisites
            if (!checkRecordingPrerequisites(context)) {
                return false
            }
            
            // Create recording file
            recordingFile = createRecordingFile(context, sessionId)
            fileWriter = recordingFile?.bufferedWriter()
            
            // Write CSV header
            fileWriter?.write(createCSVHeader())
            fileWriter?.newLine()
            fileWriter?.flush()
            
            // Start background recording thread
            startRecordingThread()
            
            isRecording = true
            Log.i("RECORDER", "Recording started: ${recordingFile?.absolutePath}")
            
            true
        } catch (e: Exception) {
            Log.e("RECORDER", "Failed to start recording", e)
            cleanup()
            false
        }
    }
    
    fun recordFrame(timestamp: Long, tempData: ByteArray, metadata: ThermalMetadata?) {
        if (!isRecording) return
        
        val frame = RecordingFrame(timestamp, tempData, metadata)
        
        // Add to queue for background processing
        if (!recordingQueue.offer(frame)) {
            Log.w("RECORDER", "Recording queue full, dropping frame")
        }
    }
    
    fun stopRecording(): String? {
        if (!isRecording) {
            Log.w("RECORDER", "No recording in progress")
            return null
        }
        
        isRecording = false
        
        // Wait for queue to empty
        while (!recordingQueue.isEmpty()) {
            Thread.sleep(100)
        }
        
        // Stop recording thread
        recordingThread?.interrupt()
        
        // Close file
        fileWriter?.close()
        
        val filePath = recordingFile?.absolutePath
        Log.i("RECORDER", "Recording stopped: $filePath")
        
        cleanup()
        return filePath
    }
    
    private fun startRecordingThread() {
        recordingThread = Thread {
            try {
                while (isRecording || !recordingQueue.isEmpty()) {
                    val frame = recordingQueue.poll(1, TimeUnit.SECONDS)
                    if (frame != null) {
                        writeFrameToFile(frame)
                    }
                }
            } catch (e: InterruptedException) {
                Log.d("RECORDER", "Recording thread interrupted")
            } catch (e: Exception) {
                Log.e("RECORDER", "Error in recording thread", e)
            }
        }
        recordingThread?.start()
    }
    
    private fun writeFrameToFile(frame: RecordingFrame) {
        try {
            // Calculate frame statistics
            val stats = calculateFrameStatistics(frame.temperatureData)
            
            // Write CSV row
            val csvRow = "${frame.timestamp},${stats.minTemp},${stats.maxTemp},${stats.avgTemp}"
            fileWriter?.write(csvRow)
            fileWriter?.newLine()
            
            // Flush periodically
            if (frame.timestamp % 1000 == 0L) {
                fileWriter?.flush()
            }
        } catch (e: Exception) {
            Log.e("RECORDER", "Error writing frame to file", e)
        }
    }
    
    private fun checkRecordingPrerequisites(context: Context): Boolean {
        // Check storage space
        val statFs = StatFs(context.filesDir.absolutePath)
        val availableBytes = statFs.availableBytes
        
        if (availableBytes < 50 * 1024 * 1024) { // 50MB minimum
            Log.e("RECORDER", "Insufficient storage space: ${availableBytes / 1024 / 1024}MB")
            return false
        }
        
        // Check write permissions
        val hasPermission = context.checkSelfPermission(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            Log.e("RECORDER", "Write permission not granted")
            return false
        }
        
        return true
    }
    
    private fun cleanup() {
        fileWriter?.close()
        fileWriter = null
        recordingFile = null
        recordingQueue.clear()
        recordingThread = null
    }
}
```

## Error Code Reference

### TC001 Error Codes

#### USB Connection Errors (1000-1099)
- **1001**: `ERROR_USB_PERMISSION_DENIED` - USB permission not granted by user
- **1002**: `ERROR_USB_DEVICE_NOT_FOUND` - TC001 device not connected
- **1003**: `ERROR_USB_CONNECTION_FAILED` - Failed to establish USB connection
- **1004**: `ERROR_USB_COMMUNICATION_TIMEOUT` - USB communication timeout
- **1005**: `ERROR_USB_DEVICE_DISCONNECTED` - Device disconnected during operation

#### Thermal Imaging Errors (2000-2099)
- **2001**: `ERROR_THERMAL_STREAM_FAILED` - Failed to start thermal stream
- **2002**: `ERROR_THERMAL_CALIBRATION_INVALID` - Invalid calibration data
- **2003**: `ERROR_THERMAL_TEMPERATURE_OUT_OF_RANGE` - Temperature reading out of sensor range
- **2004**: `ERROR_THERMAL_FRAME_CORRUPTED` - Corrupted thermal frame data
- **2005**: `ERROR_THERMAL_PROCESSING_FAILED` - Image processing failed

#### File I/O Errors (3000-3099)
- **3001**: `ERROR_FILE_WRITE_FAILED` - Failed to write thermal data file
- **3002**: `ERROR_FILE_READ_FAILED` - Failed to read thermal data file
- **3003**: `ERROR_FILE_STORAGE_FULL` - Insufficient storage space
- **3004**: `ERROR_FILE_PERMISSION_DENIED` - File system permission denied
- **3005**: `ERROR_FILE_CORRUPTED` - Data file corrupted or invalid format

#### System Errors (4000-4099)
- **4001**: `ERROR_SYSTEM_LOW_MEMORY` - Insufficient system memory
- **4002**: `ERROR_SYSTEM_THERMAL_THROTTLING` - Device thermal throttling active
- **4003**: `ERROR_SYSTEM_OPENCV_NOT_LOADED` - OpenCV library not loaded
- **4004**: `ERROR_SYSTEM_UNSUPPORTED_DEVICE` - Android device not supported

### Error Handling Implementation

```kotlin
class TC001ErrorHandler {
    
    fun handleError(errorCode: Int, context: Context) {
        when (errorCode) {
            1001 -> handleUSBPermissionDenied(context)
            1002 -> handleDeviceNotFound(context)
            2001 -> handleThermalStreamFailed(context)
            2004 -> handleFrameCorrupted(context)
            3001 -> handleFileWriteFailed(context)
            3003 -> handleStorageFull(context)
            4001 -> handleLowMemory(context)
            else -> handleUnknownError(errorCode, context)
        }
    }
    
    private fun handleUSBPermissionDenied(context: Context) {
        Toast.makeText(context, 
            "USB permission required for thermal camera access", 
            Toast.LENGTH_LONG).show()
        // Redirect to USB permission request
    }
    
    private fun handleDeviceNotFound(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("TC001 Camera Not Found")
            .setMessage("Please connect the TC001 thermal camera and try again.")
            .setPositiveButton("Retry") { _, _ -> retryDeviceConnection() }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun handleStorageFull(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Storage Full")
            .setMessage("Insufficient storage space for recording. Please free up space and try again.")
            .setPositiveButton("OK", null)
            .show()
    }
}
```

## Support and Resources

### Getting Additional Help

#### Enable Debug Logging
```kotlin
// Enable comprehensive debug logging
ThermalLogger.isDebugEnabled = true

// Enable system logging
Log.d("TC001_SUPPORT", "=== Support Information ===")
Log.d("TC001_SUPPORT", "App Version: ${BuildConfig.VERSION_NAME}")
Log.d("TC001_SUPPORT", "Android Version: ${Build.VERSION.RELEASE}")
Log.d("TC001_SUPPORT", "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
Log.d("TC001_SUPPORT", "USB Host Support: ${context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)}")
```

#### Generate Diagnostic Report
```kotlin
fun generateDiagnosticReport(context: Context): String {
    val report = StringBuilder()
    
    report.appendLine("TC001 Diagnostic Report")
    report.appendLine("Generated: ${Date()}")
    report.appendLine("="

50)
    report.appendLine()
    
    // System information
    report.appendLine("System Information:")
    report.appendLine("  App Version: ${BuildConfig.VERSION_NAME}")
    report.appendLine("  Android Version: ${Build.VERSION.RELEASE}")
    report.appendLine("  Device: ${Build.MANUFACTURER} ${Build.MODEL}")
    report.appendLine("  API Level: ${Build.VERSION.SDK_INT}")
    report.appendLine()
    
    // USB information
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    report.appendLine("USB Information:")
    report.appendLine("  USB Host Support: ${context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)}")
    report.appendLine("  Connected Devices: ${usbManager.deviceList.size}")
    
    usbManager.deviceList.values.forEach { device ->
        report.appendLine("    Device: ${device.deviceName}")
        report.appendLine("    VID: 0x${Integer.toHexString(device.vendorId)}")
        report.appendLine("    PID: 0x${Integer.toHexString(device.productId)}")
    }
    
    return report.toString()
}
```

This comprehensive troubleshooting guide provides systematic approaches to diagnosing and resolving issues with the TC001 thermal imaging integration, helping developers and users maintain optimal system performance.