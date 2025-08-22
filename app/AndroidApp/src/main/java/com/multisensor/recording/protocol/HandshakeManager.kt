package com.multisensor.recording.protocol

import android.content.Context
import android.util.Log
import com.multisensor.recording.config.CommonConstants
import org.json.JSONObject
import java.net.Socket

class HandshakeManager(
    private val context: Context,
) {
    companion object {
        private const val TAG = "HandshakeManager"
        private const val HANDSHAKE_TIMEOUT_MS = 10000L
    }

    private val schemaManager = SchemaManager.getInstance(context)

    fun sendHandshake(socket: Socket): Boolean =
        try {
            val handshakeMessage = createHandshakeMessage()
            val messageJson = handshakeMessage.toString()

            Log.i(TAG, "Sending handshake: $messageJson")

            val outputStream = socket.getOutputStream()
            outputStream.write(messageJson.toByteArray())
            outputStream.write('\n'.code)
            outputStream.flush()

            Log.i(TAG, "Handshake sent successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send handshake: ${e.message}", e)
            false
        }

    fun processHandshakeAck(ackMessage: JSONObject): Boolean {
        return try {
            if (!schemaManager.validateMessage(ackMessage)) {
                Log.e(TAG, "Invalid handshake acknowledgment format")
                return false
            }

            val serverProtocolVersion = ackMessage.getInt("protocol_version")
            val serverName = ackMessage.getString("server_name")
            val serverVersion = ackMessage.getString("server_version")
            val compatible = ackMessage.getBoolean("compatible")
            val message = ackMessage.optString("message", "")

            Log.i(TAG, "Received handshake ack from $serverName v$serverVersion")
            Log.i(TAG, "Server protocol version: $serverProtocolVersion")
            Log.i(TAG, "Client protocol version: ${CommonConstants.PROTOCOL_VERSION}")

            if (!compatible) {
                Log.w(TAG, "Protocol version mismatch detected!")
                Log.w(TAG, "Server message: $message")

                if (serverProtocolVersion != CommonConstants.PROTOCOL_VERSION) {
                    Log.w(
                        TAG,
                        "Version mismatch: Android v${CommonConstants.PROTOCOL_VERSION}, Server v$serverProtocolVersion"
                    )
                    Log.w(TAG, "Consider updating both applications to the same version")
                }

                return false
            }

            Log.i(TAG, "Handshake successful - protocol versions compatible")
            if (message.isNotEmpty()) {
                Log.i(TAG, "Server message: $message")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error processing handshake acknowledgment: ${e.message}", e)
            false
        }
    }

    private fun createHandshakeMessage(): JSONObject =
        schemaManager.createMessage("handshake").apply {
            put("protocol_version", CommonConstants.PROTOCOL_VERSION)
            put("device_name", getDeviceName())
            put("app_version", CommonConstants.APP_VERSION)
            put("device_type", "android")
        }

    private fun getDeviceName(): String =
        try {
            val manufacturer = android.os.Build.MANUFACTURER
            val model = android.os.Build.MODEL
            "$manufacturer $model"
        } catch (e: Exception) {
            "Android Device"
        }

    fun areVersionsCompatible(
        clientVersion: Int,
        serverVersion: Int,
    ): Boolean {
        return clientVersion == serverVersion
    }

    fun createHandshakeAck(
        clientProtocolVersion: Int,
        compatible: Boolean,
        message: String = "",
    ): JSONObject =
        schemaManager.createMessage("handshake_ack").apply {
            put("protocol_version", CommonConstants.PROTOCOL_VERSION)
            put("server_name", "Android Recording Device")
            put("server_version", CommonConstants.APP_VERSION)
            put("compatible", compatible)
            if (message.isNotEmpty()) {
                put("message", message)
            }
        }
}
