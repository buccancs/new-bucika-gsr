package com.multisensor.recording.network

import android.annotation.SuppressLint
import android.os.Build
import com.multisensor.recording.util.Logger
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonSocketClient
@Inject
constructor(
    private val logger: Logger,
) {
    private var socket: Socket? = null
    private var inputStream: BufferedInputStream? = null
    private var outputStream: BufferedOutputStream? = null
    private var connectionScope: CoroutineScope? = null
    private var isConnected = false
    private var shouldReconnect = true

    private var serverIp: String = ""
    private var serverPort: Int = 9000

    private var commandCallback: ((JsonMessage) -> Unit)? = null

    companion object {
        private const val RECONNECT_DELAY_MS = 5000L
        private const val CONNECTION_TIMEOUT_MS = 10000
        private const val LENGTH_HEADER_SIZE = 4
    }

    fun configure(
        ip: String,
        port: Int = 9000,
    ) {
        serverIp = ip
        serverPort = port
        logger.info("JsonSocketClient configured for $ip:$port")
    }

    fun setCommandCallback(callback: (JsonMessage) -> Unit) {
        commandCallback = callback
    }

    fun connect() {
        if (isConnected) {
            logger.warning("JsonSocketClient already connected")
            return
        }

        shouldReconnect = true
        connectionScope = CoroutineScope(Dispatchers.IO + Job())

        connectionScope?.launch {
            connectWithRetry()
        }

        logger.info("JsonSocketClient connection started")
    }

    fun disconnect() {
        logger.info("Disconnecting JsonSocketClient...")
        shouldReconnect = false

        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()

            connectionScope?.cancel()

            isConnected = false
            socket = null
            inputStream = null
            outputStream = null
            connectionScope = null

            logger.info("JsonSocketClient disconnected successfully")
        } catch (e: Exception) {
            logger.error("Error disconnecting JsonSocketClient", e)
        }
    }

    fun sendMessage(message: JsonMessage) {
        if (!isConnected) {
            logger.warning("Cannot send message - not connected to server")
            return
        }

        connectionScope?.launch {
            try {
                val jsonString = JsonMessage.toJson(message)
                val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

                val lengthHeader =
                    ByteBuffer
                        .allocate(LENGTH_HEADER_SIZE)
                        .order(ByteOrder.BIG_ENDIAN)
                        .putInt(jsonBytes.size)
                        .array()

                outputStream?.write(lengthHeader)
                outputStream?.write(jsonBytes)
                outputStream?.flush()

                logger.debug("Sent message: ${message.type} (${jsonBytes.size} bytes)")
            } catch (e: IOException) {
                logger.error("Error sending message", e)
                handleConnectionError()
            }
        }
    }

    fun sendHelloMessage(
        deviceId: String,
        capabilities: List<String>,
    ) {
        val helloMessage =
            HelloMessage(
                device_id = deviceId,
                capabilities = capabilities,
            )
        sendMessage(helloMessage)
    }

    fun sendAck(
        commandType: String,
        success: Boolean,
        errorMessage: String? = null,
    ) {
        val ackMessage =
            AckMessage(
                cmd = commandType,
                status = if (success) "ok" else "error",
                message = errorMessage,
            )
        sendMessage(ackMessage)
    }

    fun sendStatusUpdate(
        battery: Int?,
        storage: String?,
        temperature: Double?,
        recording: Boolean,
    ) {
        val statusMessage =
            StatusMessage(
                battery = battery,
                storage = storage,
                temperature = temperature,
                recording = recording,
                connected = true,
            )
        sendMessage(statusMessage)
    }

    private suspend fun connectWithRetry() {
        while (shouldReconnect && !isConnected) {
            try {
                logger.info("Attempting to connect to $serverIp:$serverPort...")

                socket =
                    Socket().apply {
                        soTimeout = CONNECTION_TIMEOUT_MS
                        connect(java.net.InetSocketAddress(serverIp, serverPort), CONNECTION_TIMEOUT_MS)
                    }

                inputStream = BufferedInputStream(socket?.getInputStream())
                outputStream = BufferedOutputStream(socket?.getOutputStream())
                isConnected = true

                logger.info("Connected to PC server at $serverIp:$serverPort")

                sendHelloMessage(
                    deviceId =
                        android.os.Build.MODEL + "_" +
                                getDeviceSerial().takeLast(4),
                    capabilities = listOf("rgb_video", "thermal", "shimmer"),
                )

                startMessageListener()
            } catch (e: Exception) {
                logger.error("Connection failed: ${e.message}")
                handleConnectionError()

                if (shouldReconnect) {
                    logger.info("Retrying connection in ${RECONNECT_DELAY_MS}ms...")
                    delay(RECONNECT_DELAY_MS)
                }
            }
        }
    }

    private suspend fun startMessageListener() {
        try {
            while (isConnected && shouldReconnect) {
                val lengthHeader = ByteArray(LENGTH_HEADER_SIZE)
                var bytesRead = 0

                while (bytesRead < LENGTH_HEADER_SIZE) {
                    val read = inputStream?.read(lengthHeader, bytesRead, LENGTH_HEADER_SIZE - bytesRead) ?: -1
                    if (read == -1) {
                        throw IOException("Connection closed while reading length header")
                    }
                    bytesRead += read
                }

                val messageLength =
                    ByteBuffer
                        .wrap(lengthHeader)
                        .order(ByteOrder.BIG_ENDIAN)
                        .int

                if (messageLength <= 0 || messageLength > 1024 * 1024) {
                    throw IOException("Invalid message length: $messageLength")
                }

                val messageBytes = ByteArray(messageLength)
                bytesRead = 0

                while (bytesRead < messageLength) {
                    val read = inputStream?.read(messageBytes, bytesRead, messageLength - bytesRead) ?: -1
                    if (read == -1) {
                        throw IOException("Connection closed while reading message payload")
                    }
                    bytesRead += read
                }

                val jsonString = String(messageBytes, Charsets.UTF_8)
                val message = JsonMessage.fromJson(jsonString)

                if (message != null) {
                    logger.debug("Received message: ${message.type}")
                    commandCallback?.invoke(message)
                } else {
                    logger.warning("Failed to parse JSON message: $jsonString")
                }
            }
        } catch (e: IOException) {
            logger.error("Error in message listener", e)
            handleConnectionError()
        }
    }

    private suspend fun handleConnectionError() {
        if (!isConnected) return

        logger.warning("Connection error detected, attempting to reconnect...")

        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            logger.debug("Error closing connection resources", e)
        }

        isConnected = false
        socket = null
        inputStream = null
        outputStream = null

        if (shouldReconnect) {
            delay(RECONNECT_DELAY_MS)
            connectWithRetry()
        }
    }

    fun isConnected(): Boolean = isConnected

    fun getConnectionInfo(): String =
        if (isConnected) {
            "Connected to $serverIp:$serverPort"
        } else {
            "Disconnected"
        }
}

@SuppressLint("HardwareIds")
private fun getDeviceSerial(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            Build.getSerial()
        } catch (e: SecurityException) {
            "UNKNOWN"
        }
    } else {
        @Suppress("DEPRECATION")
        Build.SERIAL
    }
}
