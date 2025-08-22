package com.multisensor.recording.network

import android.annotation.SuppressLint
import android.os.Build
import com.multisensor.recording.security.SecurityUtils
import com.multisensor.recording.util.Logger
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

@Singleton
class SecureJsonSocketClient
@Inject
constructor(
    private val logger: Logger,
    private val securityUtils: SecurityUtils,
) {
    private var sslSocket: SSLSocket? = null
    private var inputStream: BufferedInputStream? = null
    private var outputStream: BufferedOutputStream? = null
    private var connectionScope: CoroutineScope? = null
    private var isConnected = false
    private var shouldReconnect = true
    private var isAuthenticated = false

    private var serverIp: String = "192.168.0.100"
    private var serverPort: Int = 9000
    private var authToken: String? = null

    private var commandCallback: ((JsonMessage) -> Unit)? = null
    private var authCallback: ((Boolean) -> Unit)? = null

    companion object {
        private const val RECONNECT_DELAY_MS = 5000L
        private const val CONNECTION_TIMEOUT_MS = 10000
        private const val LENGTH_HEADER_SIZE = 4
        private const val AUTH_TIMEOUT_MS = 30000L
    }

    fun configure(
        ip: String,
        port: Int = 9000,
        authToken: String? = null,
    ) {
        serverIp = ip
        serverPort = port
        this.authToken = authToken
        logger.info("SecureJsonSocketClient configured for $ip:$port with TLS enabled")
    }

    fun setCommandCallback(callback: (JsonMessage) -> Unit) {
        commandCallback = callback
    }

    fun setAuthCallback(callback: (Boolean) -> Unit) {
        authCallback = callback
    }

    fun connect() {
        if (isConnected) {
            logger.warning("SecureJsonSocketClient already connected")
            return
        }

        shouldReconnect = true
        isAuthenticated = false
        connectionScope = CoroutineScope(Dispatchers.IO + Job())

        connectionScope?.launch {
            connectWithRetry()
        }

        logger.info("SecureJsonSocketClient connection started with TLS encryption")
    }

    fun disconnect() {
        logger.info("Disconnecting SecureJsonSocketClient...")
        shouldReconnect = false
        isAuthenticated = false

        try {
            outputStream?.close()
            inputStream?.close()
            sslSocket?.close()

            connectionScope?.cancel()

            isConnected = false
            sslSocket = null
            inputStream = null
            outputStream = null
            connectionScope = null

            logger.info("SecureJsonSocketClient disconnected successfully")
        } catch (e: Exception) {
            logger.error("Error disconnecting SecureJsonSocketClient", e)
        }
    }

    fun sendMessage(message: JsonMessage) {
        if (!isConnected) {
            logger.warning("Cannot send message - not connected to server")
            return
        }

        if (!isAuthenticated && message !is AuthenticateMessage) {
            logger.warning("Cannot send message - not authenticated. Only authentication messages allowed.")
            return
        }

        connectionScope?.launch {
            try {
                val jsonString = JsonMessage.toJson(message)
                val sanitizedJson = securityUtils.sanitizeForLogging(jsonString)

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

                logger.debug("Sent secure message: ${message.type} (${jsonBytes.size} bytes)")
            } catch (e: IOException) {
                logger.error("Error sending secure message", e)
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

    fun sendAuthenticateMessage(token: String) {
        if (!securityUtils.validateAuthToken(token)) {
            logger.error("Invalid authentication token format")
            authCallback?.invoke(false)
            return
        }

        val authMessage = AuthenticateMessage(token = token)
        sendMessage(authMessage)

        connectionScope?.launch {
            delay(AUTH_TIMEOUT_MS)
            if (!isAuthenticated) {
                logger.error("Authentication timeout")
                authCallback?.invoke(false)
                handleConnectionError()
            }
        }
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
                logger.info("Attempting secure connection to $serverIp:$serverPort...")

                val sslContext = securityUtils.createSecureSSLContext()
                if (sslContext == null) {
                    logger.error("Failed to create SSL context")
                    handleConnectionError()
                    continue
                }

                val socketFactory = sslContext.socketFactory
                sslSocket = socketFactory.createSocket() as SSLSocket

                sslSocket?.apply {
                    soTimeout = CONNECTION_TIMEOUT_MS
                    enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
                    val supportedCiphers = supportedCipherSuites
                    enabledCipherSuites = supportedCiphers?.filter { cipher ->
                        cipher.contains("AES") && cipher.contains("GCM") ||
                        cipher.contains("CHACHA20")
                    }?.toTypedArray() ?: supportedCiphers

                    connect(InetSocketAddress(serverIp, serverPort), CONNECTION_TIMEOUT_MS)
                    startHandshake()
                }

                inputStream = BufferedInputStream(sslSocket?.inputStream)
                outputStream = BufferedOutputStream(sslSocket?.outputStream)
                isConnected = true

                logger.info("Secure connection established to PC server at $serverIp:$serverPort")
                logger.info("SSL Session: ${sslSocket?.session?.protocol} with ${sslSocket?.session?.cipherSuite}")

                sendHelloMessage(
                    deviceId = android.os.Build.MODEL + "_" + getDeviceSerial().takeLast(4),
                    capabilities = listOf("rgb_video", "thermal", "shimmer"),
                )

                authToken?.let { token ->
                    sendAuthenticateMessage(token)
                }

                startMessageListener()
            } catch (e: Exception) {
                logger.error("Secure connection failed: ${e.message}")
                handleConnectionError()

                if (shouldReconnect) {
                    logger.info("Retrying secure connection in ${RECONNECT_DELAY_MS}ms...")
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
                        throw IOException("Secure connection closed while reading length header")
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
                        throw IOException("Secure connection closed while reading message payload")
                    }
                    bytesRead += read
                }

                val jsonString = String(messageBytes, Charsets.UTF_8)
                val sanitizedJson = securityUtils.sanitizeForLogging(jsonString)
                val message = JsonMessage.fromJson(jsonString)

                if (message != null) {
                    logger.debug("Received secure message: ${message.type}")

                    if (message is AuthResponseMessage) {
                        handleAuthenticationResponse(message)
                    } else {
                        commandCallback?.invoke(message)
                    }
                } else {
                    logger.warning("Failed to parse secure JSON message")
                }
            }
        } catch (e: IOException) {
            logger.error("Error in secure message listener", e)
            handleConnectionError()
        }
    }

    private fun handleAuthenticationResponse(message: AuthResponseMessage) {
        if (message.success) {
            isAuthenticated = true
            logger.info("Authentication successful")
            authCallback?.invoke(true)
        } else {
            isAuthenticated = false
            logger.error("Authentication failed: ${message.message}")
            authCallback?.invoke(false)
            connectionScope?.launch {
                handleConnectionError()
            }
        }
    }

    private suspend fun handleConnectionError() {
        if (!isConnected) return

        logger.warning("Secure connection error detected, attempting to reconnect...")

        try {
            outputStream?.close()
            inputStream?.close()
            sslSocket?.close()
        } catch (e: Exception) {
            logger.debug("Error closing secure connection resources", e)
        }

        isConnected = false
        isAuthenticated = false
        sslSocket = null
        inputStream = null
        outputStream = null

        if (shouldReconnect) {
            delay(RECONNECT_DELAY_MS)
            connectWithRetry()
        }
    }

    fun isConnected(): Boolean = isConnected

    fun isAuthenticated(): Boolean = isAuthenticated

    fun getConnectionInfo(): String =
        if (isConnected) {
            val protocol = sslSocket?.session?.protocol ?: "Unknown"
            val cipher = sslSocket?.session?.cipherSuite ?: "Unknown"
            "Secure connection to $serverIp:$serverPort ($protocol, $cipher)"
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

data class AuthenticateMessage(
    val token: String
) : JsonMessage() {
    override val type: String = "authenticate"

    override fun toJsonObject(): JSONObject =
        JSONObject().apply {
            put("type", type)
            put("token", token)
        }

    companion object {
        fun fromJson(json: JSONObject): AuthenticateMessage =
            AuthenticateMessage(
                token = json.getString("token")
            )
    }
}

data class AuthResponseMessage(
    val success: Boolean,
    val message: String? = null
) : JsonMessage() {
    override val type: String = "auth_response"

    override fun toJsonObject(): JSONObject =
        JSONObject().apply {
            put("type", type)
            put("success", success)
            message?.let { put("message", it) }
        }

    companion object {
        fun fromJson(json: JSONObject): AuthResponseMessage =
            AuthResponseMessage(
                success = json.getBoolean("success"),
                message = if (json.has("message")) json.getString("message") else null
            )
    }
}
