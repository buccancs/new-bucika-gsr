package com.topdon.bucika.pc.discovery

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Discovery service for automatic device registration via mDNS/UDP
 */
class DiscoveryService {
    
    private var jmdns: JmDNS? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false
    
    // Discovered devices
    private val discoveredDevices = ConcurrentHashMap<String, DiscoveredDevice>()
    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()
    
    // Service announcement
    private var serviceInfo: ServiceInfo? = null
    
    suspend fun start() {
        try {
            val localhost = InetAddress.getLocalHost()
            jmdns = JmDNS.create(localhost)
            
            // Announce our orchestrator service
            announceOrchestratorService()
            
            // Start discovery listener
            startDiscoveryListener()
            
            isRunning = true
            logger.info { "Discovery service started on ${localhost.hostAddress}" }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to start discovery service" }
            throw e
        }
    }
    
    suspend fun stop() {
        isRunning = false
        
        try {
            serviceInfo?.let { jmdns?.unregisterService(it) }
            jmdns?.close()
        } catch (e: Exception) {
            logger.warn(e) { "Error closing mDNS service" }
        }
        
        serviceScope.cancel()
        serviceScope.coroutineContext[Job]?.join()
        
        logger.info { "Discovery service stopped" }
    }
    
    private fun announceOrchestratorService() {
        try {
            val props = mapOf(
                "version" to "1.0",
                "role" to "orchestrator",
                "capabilities" to "session-management,time-sync,data-ingest"
            )
            
            serviceInfo = ServiceInfo.create(
                "_bucika-gsr._tcp.local.",
                "BucikaOrchestrator",
                8080, // WebSocket port
                0, // weight
                0, // priority
                props
            )
            
            jmdns?.registerService(serviceInfo)
            logger.info { "Announced orchestrator service via mDNS" }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to announce orchestrator service" }
        }
    }
    
    private fun startDiscoveryListener() {
        serviceScope.launch {
            try {
                jmdns?.addServiceListener("_bucika-gsr._tcp.local.", object : javax.jmdns.ServiceListener {
                    override fun serviceAdded(event: javax.jmdns.ServiceEvent) {
                        logger.debug { "Service discovered: ${event.name}" }
                        // Request service info resolution
                        jmdns?.requestServiceInfo(event.type, event.name, 1000)
                    }
                    
                    override fun serviceRemoved(event: javax.jmdns.ServiceEvent) {
                        logger.info { "Service removed: ${event.name}" }
                        removeDevice(event.name)
                    }
                    
                    override fun serviceResolved(event: javax.jmdns.ServiceEvent) {
                        logger.info { "Service resolved: ${event.name}" }
                        val info = event.info
                        if (info != null && info.name != "BucikaOrchestrator") {
                            addDiscoveredDevice(info)
                        }
                    }
                })
                
                // Periodic device cleanup
                periodicCleanup()
                
            } catch (e: Exception) {
                logger.error(e) { "Error in discovery listener" }
            }
        }
    }
    
    private fun addDiscoveredDevice(serviceInfo: ServiceInfo) {
        try {
            val deviceId = serviceInfo.name
            val capabilities = serviceInfo.getPropertyString("capabilities")?.split(",") ?: emptyList()
            val version = serviceInfo.getPropertyString("version") ?: "unknown"
            val deviceName = serviceInfo.getPropertyString("deviceName") ?: deviceId
            
            val device = DiscoveredDevice(
                deviceId = deviceId,
                deviceName = deviceName,
                ipAddress = serviceInfo.inetAddresses.firstOrNull()?.hostAddress ?: "unknown",
                port = serviceInfo.port,
                capabilities = capabilities,
                version = version,
                lastSeen = System.currentTimeMillis(),
                batteryLevel = serviceInfo.getPropertyString("battery")?.toIntOrNull() ?: -1
            )
            
            discoveredDevices[deviceId] = device
            updateDevicesList()
            
            logger.info { 
                "Discovered device: $deviceName ($deviceId) at ${device.ipAddress}:${device.port} " +
                "with capabilities: ${capabilities.joinToString()}"
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Error adding discovered device: ${serviceInfo.name}" }
        }
    }
    
    private fun removeDevice(deviceName: String) {
        discoveredDevices.remove(deviceName)
        updateDevicesList()
        logger.info { "Removed device: $deviceName" }
    }
    
    private fun updateDevicesList() {
        _devices.value = discoveredDevices.values.toList().sortedBy { it.deviceName }
    }
    
    private suspend fun periodicCleanup() {
        while (isRunning) {
            delay(30_000) // 30 seconds
            
            val currentTime = System.currentTimeMillis()
            val staleThreshold = 60_000L // 1 minute
            
            // Remove devices not seen for more than threshold
            val staleDevices = discoveredDevices.filter { (_, device) ->
                currentTime - device.lastSeen > staleThreshold
            }
            
            staleDevices.forEach { (deviceId, _) ->
                discoveredDevices.remove(deviceId)
                logger.info { "Removed stale device: $deviceId" }
            }
            
            if (staleDevices.isNotEmpty()) {
                updateDevicesList()
            }
        }
    }
    
    fun getDevice(deviceId: String): DiscoveredDevice? = discoveredDevices[deviceId]
    
    fun getAllDevices(): List<DiscoveredDevice> = discoveredDevices.values.toList()
    
    fun refreshDiscovery() {
        serviceScope.launch {
            try {
                // Re-scan for services
                jmdns?.list("_bucika-gsr._tcp.local.")
                logger.debug { "Manual discovery refresh initiated" }
            } catch (e: Exception) {
                logger.error(e) { "Error during manual discovery refresh" }
            }
        }
    }
}

data class DiscoveredDevice(
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val port: Int,
    val capabilities: List<String>,
    val version: String,
    val lastSeen: Long,
    val batteryLevel: Int
) {
    fun hasCapability(capability: String): Boolean = capabilities.contains(capability)
    fun isGSRLeader(): Boolean = hasCapability("GSR_LEADER")
    fun hasVideo(): Boolean = hasCapability("RGB") || hasCapability("THERMAL")
}