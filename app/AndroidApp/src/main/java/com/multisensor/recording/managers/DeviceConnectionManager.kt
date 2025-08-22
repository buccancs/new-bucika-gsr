package com.multisensor.recording.managers

import android.content.Context
import android.hardware.camera2.CameraManager
import android.view.SurfaceView
import android.view.TextureView
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.multisensor.recording.recording.CameraRecorder
import com.multisensor.recording.recording.ThermalRecorder
import com.multisensor.recording.recording.ShimmerRecorder
import com.multisensor.recording.network.JsonSocketClient
import com.multisensor.recording.network.NetworkConfiguration
import com.multisensor.recording.network.ServerConfiguration
import com.multisensor.recording.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraRecorder: CameraRecorder,
    private val thermalRecorder: ThermalRecorder,
    private val shimmerRecorder: ShimmerRecorder,
    private val jsonSocketClient: JsonSocketClient,
    private val networkConfiguration: NetworkConfiguration,
    private val logger: Logger
) {

    data class DeviceConnectionState(
        val cameraConnected: Boolean = false,
        val thermalConnected: Boolean = false,
        val shimmerConnected: Boolean = false,
        val pcConnected: Boolean = false,
        val isInitializing: Boolean = false,
        val isScanning: Boolean = false,
        val connectionError: String? = null,
        val deviceInfo: DeviceInfo = DeviceInfo()
    )

    data class DeviceInfo(
        val availableCameras: List<String> = emptyList(),
        val shimmerDevices: List<ShimmerDeviceInfo> = emptyList(),
        val pcServerAddress: String? = null,
        val thermalCameraModel: String? = null
    )

    data class ShimmerDeviceInfo(
        val macAddress: String,
        val deviceName: String,
        val connectionType: String,
        val isConnected: Boolean = false
    )

    private val _connectionState = MutableStateFlow(DeviceConnectionState())
    val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

    suspend fun initializeAllDevices(
        textureView: TextureView? = null,
        thermalSurfaceView: SurfaceView? = null
    ): Result<String> {
        return try {
            logger.info("Initializing all devices...")
            _connectionState.value = _connectionState.value.copy(isInitializing = true)

            val results = mutableListOf<String>()
            var successCount = 0
            var totalDevices = 0

            totalDevices++
            val cameraResult = initializeCamera(textureView)
            if (cameraResult.isSuccess) {
                successCount++
                results.add("Camera: OK")
            } else {
                results.add("Camera: ${cameraResult.exceptionOrNull()?.message ?: "Failed"}")
            }

            totalDevices++
            val thermalResult = initializeThermalCamera(thermalSurfaceView)
            if (thermalResult.isSuccess) {
                successCount++
                results.add("Thermal: OK")
            } else {
                results.add("Thermal: ${thermalResult.exceptionOrNull()?.message ?: "N/A"}")
            }

            totalDevices++
            val shimmerResult = initializeShimmerSensors()
            if (shimmerResult.isSuccess) {
                successCount++
                results.add("Shimmer: OK")
            } else {
                results.add("Shimmer: ${shimmerResult.exceptionOrNull()?.message ?: "N/A"}")
            }

            val summary = "Device initialization: $successCount/$totalDevices successful - ${results.joinToString(", ")}"

            _connectionState.value = _connectionState.value.copy(isInitializing = false)

            logger.info("Device initialization completed: $summary")
            Result.success(summary)

        } catch (e: Exception) {
            logger.error("Device initialization failed", e)
            _connectionState.value = _connectionState.value.copy(
                isInitializing = false,
                connectionError = "Initialization failed: ${e.message}"
            )
            Result.failure(e)
        }
    }

    private suspend fun initializeCamera(textureView: TextureView?): Result<Unit> {
        return try {
            if (textureView != null) {
                logger.info("Initializing camera...")
                val success = cameraRecorder.initialize(textureView)
                if (success) {
                    val previewSession = cameraRecorder.startSession(recordVideo = false, captureRaw = false)
                    _connectionState.value = _connectionState.value.copy(cameraConnected = true)
                    logger.info("Camera initialized successfully")
                    Result.success(Unit)
                } else {
                    Result.failure(RuntimeException("Camera initialization failed"))
                }
            } else {
                Result.failure(IllegalArgumentException("TextureView required"))
            }
        } catch (e: Exception) {
            logger.error("Camera initialization error", e)
            Result.failure(e)
        }
    }

    private suspend fun initializeThermalCamera(surfaceView: SurfaceView?): Result<Unit> {
        return try {
            logger.info("Initializing thermal camera...")
            val success = thermalRecorder.initialize(surfaceView)
            if (success) {
                val previewStarted = thermalRecorder.startPreview()
                _connectionState.value = _connectionState.value.copy(thermalConnected = success)
                logger.info("Thermal camera initialized")
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("Thermal camera not available"))
            }
        } catch (e: SecurityException) {
            logger.error("Security exception initializing thermal camera", e)
            logger.warning("Thermal camera limited functionality due to USB receiver restrictions on Android 13+")
            _connectionState.value = _connectionState.value.copy(thermalConnected = false)
            Result.failure(e)
        } catch (e: Exception) {
            logger.error("Thermal camera initialization error", e)
            Result.failure(e)
        }
    }

    private suspend fun initializeShimmerSensors(): Result<Unit> {
        return try {
            val success = shimmerRecorder.initialize()
            _connectionState.value = _connectionState.value.copy(shimmerConnected = success)
            if (success) {
                logger.info("Shimmer sensors initialized successfully")
                Result.success(Unit)
            } else {
                logger.warning("No Shimmer sensors available - may be due to missing Bluetooth permissions")
                Result.failure(RuntimeException("Shimmer sensors not available - check Bluetooth permissions"))
            }
        } catch (e: Exception) {
            logger.error("Shimmer initialization error", e)
            Result.failure(e)
        }
    }

    suspend fun connectToPC(): Result<String> {
        return try {
            logger.info("Connecting to PC server...")

            // Try to discover server automatically first
            val discoveredServer = discoverPCServer()
            val serverConfig = if (discoveredServer != null) {
                logger.info("Discovered PC server at: ${discoveredServer.getJsonAddress()}")
                networkConfiguration.updateServerConfiguration(discoveredServer)
                discoveredServer
            } else {
                logger.info("Using configured server address")
                networkConfiguration.getServerConfiguration()
            }

            jsonSocketClient.configure(serverConfig.serverIp, serverConfig.jsonPort)
            jsonSocketClient.connect()

            delay(2000)

            val isConnected = jsonSocketClient.isConnected()

            if (isConnected) {
                val address = serverConfig.getJsonAddress()
                _connectionState.value = _connectionState.value.copy(
                    pcConnected = true,
                    deviceInfo = _connectionState.value.deviceInfo.copy(pcServerAddress = address)
                )
                val message = "Connected to PC at $address"
                logger.info(message)
                Result.success(message)
            } else {
                val message = "Failed to connect to PC at ${serverConfig.getJsonAddress()}"
                logger.error(message)
                Result.failure(RuntimeException(message))
            }

        } catch (e: Exception) {
            logger.error("PC connection error", e)
            _connectionState.value = _connectionState.value.copy(
                connectionError = "PC connection failed: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun disconnectFromPC(): Result<Unit> {
        return try {
            logger.info("Disconnecting from PC server...")
            jsonSocketClient.disconnect()

            _connectionState.value = _connectionState.value.copy(
                pcConnected = false,
                deviceInfo = _connectionState.value.deviceInfo.copy(pcServerAddress = null)
            )

            logger.info("Disconnected from PC server")
            Result.success(Unit)

        } catch (e: Exception) {
            logger.error("PC disconnection error", e)
            Result.failure(e)
        }
    }

    suspend fun scanForDevices(): Result<DeviceInfo> {
        return try {
            logger.info("Scanning for devices...")
            _connectionState.value = _connectionState.value.copy(isScanning = true)

            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val availableCameras = try {
                cameraManager.cameraIdList.toList()
            } catch (e: Exception) {
                logger.warning("Camera scan failed: ${e.message}")
                emptyList()
            }

            val shimmerDevices = try {
                shimmerRecorder.scanForDevices().map { (mac, name) ->
                    ShimmerDeviceInfo(
                        macAddress = mac,
                        deviceName = name,
                        connectionType = "Bluetooth",
                        isConnected = false
                    )
                }
            } catch (e: Exception) {
                logger.warning("Shimmer scan failed: ${e.message}")
                emptyList()
            }

            val thermalModel = try {
                if (thermalRecorder.isThermalCameraAvailable()) "Topdon Camera" else null
            } catch (e: Exception) {
                logger.warning("Thermal check failed: ${e.message}")
                null
            }

            val deviceInfo = DeviceInfo(
                availableCameras = availableCameras,
                shimmerDevices = shimmerDevices,
                thermalCameraModel = thermalModel
            )

            _connectionState.value = _connectionState.value.copy(
                isScanning = false,
                deviceInfo = deviceInfo
            )

            val summary = "Found: ${availableCameras.size} cameras, ${shimmerDevices.size} Shimmer devices, thermal: ${thermalModel != null}"
            logger.info("Device scan completed: $summary")

            Result.success(deviceInfo)

        } catch (e: Exception) {
            logger.error("Device scan error", e)
            _connectionState.value = _connectionState.value.copy(
                isScanning = false,
                connectionError = "Scan failed: ${e.message}"
            )
            Result.failure(e)
        }
    }

    suspend fun connectShimmerDevice(
        macAddress: String,
        deviceName: String,
        connectionType: com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid.BT_TYPE
    ): Result<Unit> {
        return try {
            logger.info("Connecting to Shimmer device: $deviceName ($macAddress)")

            val success = shimmerRecorder.connectSingleDevice(macAddress, deviceName, connectionType)

            if (success) {
                val updatedDevices = _connectionState.value.deviceInfo.shimmerDevices.map { device ->
                    if (device.macAddress == macAddress) {
                        device.copy(isConnected = true)
                    } else device
                }

                _connectionState.value = _connectionState.value.copy(
                    shimmerConnected = true,
                    deviceInfo = _connectionState.value.deviceInfo.copy(shimmerDevices = updatedDevices)
                )

                logger.info("Successfully connected to Shimmer device: $deviceName")
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("Failed to connect to Shimmer device"))
            }

        } catch (e: Exception) {
            logger.error("Shimmer connection error", e)
            Result.failure(e)
        }
    }

    suspend fun refreshDeviceStatus(): Result<String> {
        return try {
            logger.info("Refreshing device status...")

            val cameraConnected = cameraRecorder.isConnected

            val thermalConnected = thermalRecorder.isThermalCameraAvailable()

            val shimmerStatus = shimmerRecorder.getShimmerStatus()
            val shimmerConnected = shimmerStatus.isConnected

            val pcConnected = jsonSocketClient.isConnected()

            _connectionState.value = _connectionState.value.copy(
                cameraConnected = cameraConnected,
                thermalConnected = thermalConnected,
                shimmerConnected = shimmerConnected,
                pcConnected = pcConnected
            )

            val summary = "Status: Camera=$cameraConnected, Thermal=$thermalConnected, Shimmer=$shimmerConnected, PC=$pcConnected"
            logger.info("Device status refreshed: $summary")

            Result.success(summary)

        } catch (e: Exception) {
            logger.error("Device status refresh error", e)
            Result.failure(e)
        }
    }

    fun getCurrentState(): DeviceConnectionState = _connectionState.value

    fun clearError() {
        _connectionState.value = _connectionState.value.copy(connectionError = null)
    }

    private suspend fun discoverPCServer(): ServerConfiguration? {
        return withContext(Dispatchers.IO) {
            try {
                logger.info("Discovering PC server on local network...")

                val currentConfig = networkConfiguration.getServerConfiguration()

                // Enhanced discovery with multiple network detection strategies
                val discoveredServers = mutableListOf<String>()

                // Strategy 1: Try configured IP first
                discoveredServers.add(currentConfig.serverIp)

                // Strategy 2: Auto-detect network ranges from device's current network configuration
                val networkRanges = detectCurrentNetworkRanges()
                discoveredServers.addAll(networkRanges)

                // Strategy 3: Common default network ranges
                val baseIp = currentConfig.serverIp.substringBeforeLast(".")
                discoveredServers.addAll(listOf(
                    "$baseIp.100",
                    "$baseIp.101",
                    "$baseIp.1",   // Router
                    "$baseIp.10",
                    "$baseIp.50",
                    "$baseIp.254"  // Common high IP
                ))

                // Strategy 4: Cross-subnet discovery for common enterprise/home networks
                discoveredServers.addAll(listOf(
                    "192.168.1.100", "192.168.1.101", "192.168.1.10",
                    "192.168.0.100", "192.168.0.101", "192.168.0.10",
                    "10.0.0.100", "10.0.0.101", "10.0.0.10",
                    "172.16.0.100", "172.16.0.101", "172.16.0.10"
                ))

                // Remove duplicates and test each IP
                val uniqueIps = discoveredServers.distinct()
                logger.info("Testing ${uniqueIps.size} potential server addresses...")

                for (ip in uniqueIps) {
                    try {
                        logger.debug("Trying PC server at: $ip:${currentConfig.jsonPort}")

                        // Test connection with shorter timeout for faster discovery
                        val socket = java.net.Socket()
                        val success = try {
                            socket.connect(
                                java.net.InetSocketAddress(ip, currentConfig.jsonPort),
                                1500  // 1.5 second timeout for faster discovery
                            )
                            true
                        } catch (e: Exception) {
                            false
                        } finally {
                            try { socket.close() } catch (e: Exception) { }
                        }

                        if (success) {
                            logger.info("✅ Found responsive PC server at: $ip:${currentConfig.jsonPort}")

                            // Verify it's actually our PC server by trying a quick handshake
                            if (verifyPCServerIdentity(ip, currentConfig.jsonPort)) {
                                logger.info("✅ Verified PC server identity at: $ip:${currentConfig.jsonPort}")
                                return@withContext ServerConfiguration(
                                    serverIp = ip,
                                    legacyPort = currentConfig.legacyPort,
                                    jsonPort = currentConfig.jsonPort
                                )
                            } else {
                                logger.debug("Server at $ip responded but is not our PC server")
                            }
                        }

                    } catch (e: Exception) {
                        logger.debug("No server found at $ip: ${e.message}")
                    }
                }

                logger.warning("🔍 No PC server discovered on local network")
                logger.info("💡 Discovery suggestions:")
                logger.info("  • Ensure PC server is running: python pc_server_helper.py --start")
                logger.info("  • Check firewall settings on PC")
                logger.info("  • Verify both devices are on same network")
                logger.info("  • Try manual IP configuration in Android app settings")

                null

            } catch (e: Exception) {
                logger.error("Error during PC server discovery", e)
                null
            }
        }
    }

    private fun detectCurrentNetworkRanges(): List<String> {
        return try {
            val networkRanges = mutableListOf<String>()

            // Get current device's network interfaces
            val networkInterfaces = java.net.NetworkInterface.getNetworkInterfaces()

            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()

                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val inetAddresses = networkInterface.inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress = inetAddresses.nextElement()

                    if (inetAddress is java.net.Inet4Address && !inetAddress.isLoopbackAddress) {
                        val hostAddress = inetAddress.hostAddress
                        if (hostAddress != null) {
                            // Generate potential server IPs in same subnet
                            val subnet = hostAddress.substringBeforeLast(".")
                            networkRanges.addAll(listOf(
                                "$subnet.100", "$subnet.101", "$subnet.102",
                                "$subnet.10", "$subnet.50", "$subnet.1"
                            ))
                        }
                    }
                }
            }

            logger.debug("Auto-detected network ranges: $networkRanges")
            networkRanges.distinct()

        } catch (e: Exception) {
            logger.warning("Failed to auto-detect network ranges: ${e.message}")
            emptyList()
        }
    }

    private suspend fun verifyPCServerIdentity(ip: String, port: Int): Boolean {
        return try {
            // Quick identity verification - try to connect and send a ping
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(ip, port), 2000)

            // Send a simple ping message to verify it's our server
            val output = socket.getOutputStream()
            val testMessage = """{"type":"ping","timestamp":${System.currentTimeMillis()}}"""
            val messageBytes = testMessage.toByteArray()

            // Send length header + message (matching PC server protocol)
            output.write(java.nio.ByteBuffer.allocate(4).putInt(messageBytes.size).array())
            output.write(messageBytes)
            output.flush()

            // Try to read response (with timeout)
            socket.soTimeout = 2000
            val input = socket.getInputStream()
            val lengthBytes = ByteArray(4)
            val bytesRead = input.read(lengthBytes)

            socket.close()

            // If we got a response with the expected protocol, it's likely our server
            bytesRead == 4

        } catch (e: Exception) {
            logger.debug("Server identity verification failed for $ip: ${e.message}")
            false
        }
    }

    suspend fun checkDeviceCapabilities(): Result<Map<String, Boolean>> {
        return try {
            val capabilities = mutableMapOf<String, Boolean>()

            try {
                capabilities["raw_stage3"] = cameraRecorder.isRawStage3Available()
            } catch (e: Exception) {
                capabilities["raw_stage3"] = false
            }

            try {
                capabilities["thermal_camera"] = thermalRecorder.isThermalCameraAvailable()
            } catch (e: Exception) {
                capabilities["thermal_camera"] = false
            }

            try {
                capabilities["shimmer_streaming"] = shimmerRecorder.isAnyDeviceStreaming()
                capabilities["shimmer_sd_logging"] = shimmerRecorder.isAnyDeviceSDLogging()
            } catch (e: Exception) {
                capabilities["shimmer_streaming"] = false
                capabilities["shimmer_sd_logging"] = false
            }

            logger.info("Device capabilities checked: $capabilities")
            Result.success(capabilities)

        } catch (e: Exception) {
            logger.error("Capability check error", e)
            Result.failure(e)
        }
    }

    // Wrapper methods for compatibility with other naming conventions
    suspend fun initialize(textureView: TextureView? = null, thermalSurfaceView: SurfaceView? = null): Result<String> =
        initializeAllDevices(textureView, thermalSurfaceView)

    suspend fun startSession(textureView: TextureView? = null): Result<String> =
        initializeAllDevices(textureView)

    fun isThermalCameraAvailable(): Boolean {
        return try {
            connectionState.value.thermalConnected
        } catch (e: Exception) {
            false
        }
    }

    fun isConnected(): Boolean {
        return try {
            val state = connectionState.value
            state.cameraConnected || state.thermalConnected || state.shimmerConnected
        } catch (e: Exception) {
            false
        }
    }

    fun isRawStage3Available(): Boolean {
        return try {
            connectionState.value.cameraConnected
        } catch (e: Exception) {
            false
        }
    }
}
