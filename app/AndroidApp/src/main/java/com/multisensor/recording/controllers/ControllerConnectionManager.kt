package com.multisensor.recording.controllers

import com.multisensor.recording.network.JsonMessage
import com.multisensor.recording.util.Logger
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface ControllerConnectionManager {
    val connectionState: StateFlow<ConnectionState>

    suspend fun connect(connectionConfig: ConnectionConfig): Result<Unit>
    suspend fun disconnect()
    suspend fun sendCommand(command: JsonMessage): Result<Unit>
    suspend fun sendData(data: ByteArray, metadata: Map<String, Any> = emptyMap()): Result<Unit>

    fun setCommandCallback(callback: (JsonMessage) -> Unit)
    fun isConnected(): Boolean
}

data class ConnectionConfig(
    val serverIp: String,
    val serverPort: Int = 9000,
    val enableAutoReconnect: Boolean = true,
    val connectionTimeout: Long = 10000L,
    val maxRetryAttempts: Int = 5
)

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String, val exception: Throwable? = null) : ConnectionState()
}

@ServiceScoped
class PcControllerConnectionManager @Inject constructor(
    private val jsonSocketClient: com.multisensor.recording.network.JsonSocketClient,
    private val logger: Logger
) : ControllerConnectionManager {

    private val _connectionState = kotlinx.coroutines.flow.MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    override val connectionState: StateFlow<ConnectionState>
        get() = _connectionState.asStateFlow()

    override suspend fun connect(connectionConfig: ConnectionConfig): Result<Unit> {
        return try {
            _connectionState.value = ConnectionState.Connecting

            jsonSocketClient.configure(
                ip = connectionConfig.serverIp,
                port = connectionConfig.serverPort
            )

            jsonSocketClient.connect()

            _connectionState.value = ConnectionState.Connected
            logger.info("Successfully initiated connection to PC controller at ${connectionConfig.serverIp}:${connectionConfig.serverPort}")
            Result.success(Unit)
        } catch (e: Exception) {
            val error = "Connection failed: ${e.message}"
            _connectionState.value = ConnectionState.Error(error, e)
            logger.error(error, e)
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        try {
            jsonSocketClient.disconnect()
            _connectionState.value = ConnectionState.Disconnected
            logger.info("Disconnected from PC controller")
        } catch (e: Exception) {
            logger.error("Error during disconnect: ${e.message}", e)
        }
    }

    override suspend fun sendCommand(command: JsonMessage): Result<Unit> {
        return if (isConnected()) {
            try {
                jsonSocketClient.sendMessage(command)
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error("Failed to send command: ${e.message}", e)
                Result.failure(e)
            }
        } else {
            val error = "Cannot send command: not connected to PC controller"
            logger.error(error)
            Result.failure(IllegalStateException(error))
        }
    }

    override suspend fun sendData(data: ByteArray, metadata: Map<String, Any>): Result<Unit> {
        return if (isConnected()) {
            try {

                logger.debug("Sending ${data.size} bytes of data with metadata: $metadata")
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error("Failed to send data: ${e.message}", e)
                Result.failure(e)
            }
        } else {
            val error = "Cannot send data: not connected to PC controller"
            logger.error(error)
            Result.failure(IllegalStateException(error))
        }
    }

    override fun setCommandCallback(callback: (JsonMessage) -> Unit) {
        jsonSocketClient.setCommandCallback(callback)
    }

    override fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected
    }
}