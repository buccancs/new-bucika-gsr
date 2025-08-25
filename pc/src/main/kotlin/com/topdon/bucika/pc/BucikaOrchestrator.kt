package com.topdon.bucika.pc

import com.topdon.bucika.pc.discovery.DiscoveryService
import com.topdon.bucika.pc.session.SessionManager
import com.topdon.bucika.pc.time.TimeSyncService
import com.topdon.bucika.pc.ui.MainController
import com.topdon.bucika.pc.websocket.WebSocketServer
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.*

private val logger = KotlinLogging.logger {}

class BucikaOrchestrator : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private lateinit var sessionManager: SessionManager
    private lateinit var timeSyncService: TimeSyncService
    private lateinit var discoveryService: DiscoveryService
    private lateinit var webSocketServer: WebSocketServer
    private lateinit var mainController: MainController
    
    override fun start(primaryStage: Stage) {
        try {
            logger.info { "Starting Bucika GSR Orchestrator v1.0.0" }
            
            // Initialize core services
            initializeServices()
            
            // Load UI
            val loader = FXMLLoader(javaClass.getResource("/fxml/main.fxml"))
            val root = loader.load<javafx.scene.Parent>()
            mainController = loader.getController()
            
            // Configure main controller with services
            mainController.initialize(sessionManager, webSocketServer, discoveryService)
            
            // Setup primary stage
            primaryStage.title = "Bucika GSR Orchestrator"
            primaryStage.scene = Scene(root, 1200.0, 800.0)
            primaryStage.show()
            
            // Start services
            startServices()
            
            logger.info { "Bucika GSR Orchestrator started successfully" }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to start orchestrator" }
            throw e
        }
    }
    
    override fun stop() {
        logger.info { "Shutting down Bucika GSR Orchestrator" }
        
        runBlocking {
            // Stop services gracefully
            webSocketServer.stop()
            discoveryService.stop()
            timeSyncService.stop()
            
            // Cancel application scope
            applicationScope.cancel()
            applicationScope.coroutineContext[Job]?.join()
        }
        
        logger.info { "Bucika GSR Orchestrator shut down complete" }
    }
    
    private fun initializeServices() {
        sessionManager = SessionManager()
        timeSyncService = TimeSyncService()
        discoveryService = DiscoveryService()
        webSocketServer = WebSocketServer(8080, sessionManager, timeSyncService)
    }
    
    private fun startServices() {
        applicationScope.launch {
            // Start time sync service
            timeSyncService.start()
            
            // Start discovery service
            discoveryService.start()
            
            // Start WebSocket server
            webSocketServer.start()
        }
    }
}

fun main(args: Array<String>) {
    Application.launch(BucikaOrchestrator::class.java, *args)
}