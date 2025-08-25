package com.topdon.bucika.pc.demo

import com.topdon.bucika.pc.discovery.DiscoveryService
import com.topdon.bucika.pc.session.SessionManager
import com.topdon.bucika.pc.time.TimeSyncService
import com.topdon.bucika.pc.websocket.WebSocketServer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*

private val logger = KotlinLogging.logger {}

/**
 * Console demo application to demonstrate core orchestrator functionality
 * without JavaFX dependencies
 */
class ConsoleDemoOrchestrator {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private lateinit var sessionManager: SessionManager
    private lateinit var timeSyncService: TimeSyncService
    private lateinit var discoveryService: DiscoveryService
    private lateinit var webSocketServer: WebSocketServer
    
    suspend fun start() {
        try {
            logger.info { "Starting Bucika GSR Console Demo v1.0.0" }
            
            // Initialize core services
            initializeServices()
            
            // Start services
            startServices()
            
            // Demo workflow
            runDemoWorkflow()
            
            logger.info { "Bucika GSR Console Demo started successfully" }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to start console demo" }
            throw e
        }
    }
    
    fun stop() {
        logger.info { "Shutting down Bucika GSR Console Demo" }
        
        runBlocking {
            // Stop services gracefully
            webSocketServer.stop()
            discoveryService.stop()
            timeSyncService.stop()
            
            // Cancel application scope
            applicationScope.cancel()
            applicationScope.coroutineContext[Job]?.join()
        }
        
        logger.info { "Bucika GSR Console Demo shut down complete" }
    }
    
    private fun initializeServices() {
        sessionManager = SessionManager()
        timeSyncService = TimeSyncService()
        discoveryService = DiscoveryService()
        webSocketServer = WebSocketServer(8080, sessionManager, timeSyncService)
    }
    
    private suspend fun startServices() {
        // Start time sync service
        timeSyncService.start()
        logger.info { "Time sync service started on port 9123" }
        
        // Start discovery service
        discoveryService.start()
        logger.info { "Discovery service started (mDNS)" }
        
        // Start WebSocket server
        webSocketServer.start()
        logger.info { "WebSocket server started on port 8080" }
    }
    
    private suspend fun runDemoWorkflow() {
        logger.info { "Running demo workflow..." }
        
        // Create a demo session
        val session = sessionManager.createSession()
        logger.info { "Created demo session: ${session.name}" }
        
        // Monitor services for 60 seconds
        applicationScope.launch {
            repeat(12) { i ->
                delay(5000) // 5 seconds
                
                val connectedDevices = webSocketServer.getConnectedDevices()
                val discoveredDevices = discoveryService.getAllDevices()
                val timeSyncStats = timeSyncService.getAllClientStats()
                
                logger.info { 
                    "Status Update ${i + 1}/12: " +
                    "Connected: ${connectedDevices.size}, " +
                    "Discovered: ${discoveredDevices.size}, " +
                    "Time Sync Clients: ${timeSyncStats.size}"
                }
                
                if (connectedDevices.isNotEmpty()) {
                    connectedDevices.forEach { device ->
                        logger.info { 
                            "  Device: ${device.deviceName} (${device.deviceId}) " +
                            "Battery: ${device.batteryLevel}% " +
                            "Capabilities: ${device.capabilities.joinToString()}"
                        }
                    }
                }
                
                if (timeSyncStats.isNotEmpty()) {
                    timeSyncStats.forEach { stats ->
                        logger.info {
                            "  Time Sync ${stats.clientId}: " +
                            "Latest Offset: ${String.format("%.2f", stats.latestOffsetMs)}ms, " +
                            "Median: ${String.format("%.2f", stats.medianOffsetMs)}ms, " +
                            "P95: ${String.format("%.2f", stats.p95OffsetMs)}ms"
                        }
                    }
                }
            }
        }
        
        logger.info { "Demo monitoring complete. Services will continue running..." }
    }
}

suspend fun main() {
    val demo = ConsoleDemoOrchestrator()
    
    try {
        demo.start()
        
        // Keep running until interrupted
        withContext(Dispatchers.IO) {
            println("\n=== Bucika GSR Console Demo Running ===")
            println("Services are running and ready to accept connections.")
            println("Connect Android clients to ws://localhost:8080")
            println("Time sync available on UDP port 9123")
            println("Press Ctrl+C to stop...")
            
            // Wait for interrupt signal
            Runtime.getRuntime().addShutdownHook(Thread {
                demo.stop()
            })
            
            // Keep alive
            while (true) {
                delay(1000)
            }
        }
        
    } catch (e: Exception) {
        println("Demo failed: ${e.message}")
        demo.stop()
    }
}