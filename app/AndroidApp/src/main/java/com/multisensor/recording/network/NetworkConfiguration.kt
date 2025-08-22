package com.multisensor.recording.network

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConfiguration
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME = "network_config"
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_LEGACY_PORT = "legacy_port"
        private const val KEY_JSON_PORT = "json_port"

        private const val DEFAULT_SERVER_IP = "192.168.0.100"
        private const val DEFAULT_LEGACY_PORT = 8080
        private const val DEFAULT_JSON_PORT = 9000

        fun isValidIpAddress(ip: String): Boolean {
            return try {
                val parts = ip.split(".")
                if (parts.size != 4) return false

                parts.all { part ->
                    val num = part.toIntOrNull()
                    num != null && num in 0..255
                }
            } catch (e: Exception) {
                false
            }
        }

        fun isValidPort(port: Int): Boolean = port in 1024..65535
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getServerIp(): String = sharedPreferences.getString(KEY_SERVER_IP, DEFAULT_SERVER_IP) ?: DEFAULT_SERVER_IP

    fun setServerIp(ip: String) {
        sharedPreferences.edit().apply {
            putString(KEY_SERVER_IP, ip)
            apply()
        }
    }

    fun getLegacyPort(): Int = sharedPreferences.getInt(KEY_LEGACY_PORT, DEFAULT_LEGACY_PORT)

    fun setLegacyPort(port: Int) {
        sharedPreferences.edit().apply {
            putInt(KEY_LEGACY_PORT, port)
            apply()
        }
    }

    fun getJsonPort(): Int = sharedPreferences.getInt(KEY_JSON_PORT, DEFAULT_JSON_PORT)

    fun setJsonPort(port: Int) {
        sharedPreferences.edit().apply {
            putInt(KEY_JSON_PORT, port)
            apply()
        }
    }

    fun getServerConfiguration(): ServerConfiguration =
        ServerConfiguration(
            serverIp = getServerIp(),
            legacyPort = getLegacyPort(),
            jsonPort = getJsonPort(),
        )

    fun updateServerConfiguration(config: ServerConfiguration) {
        sharedPreferences.edit().apply {
            putString(KEY_SERVER_IP, config.serverIp)
            putInt(KEY_LEGACY_PORT, config.legacyPort)
            putInt(KEY_JSON_PORT, config.jsonPort)
            apply()
        }
    }

    fun resetToDefaults() {
        sharedPreferences.edit().apply {
            putString(KEY_SERVER_IP, DEFAULT_SERVER_IP)
            putInt(KEY_LEGACY_PORT, DEFAULT_LEGACY_PORT)
            putInt(KEY_JSON_PORT, DEFAULT_JSON_PORT)
            apply()
        }
    }

    fun isCustomConfiguration(): Boolean =
        getServerIp() != DEFAULT_SERVER_IP ||
                getLegacyPort() != DEFAULT_LEGACY_PORT ||
                getJsonPort() != DEFAULT_JSON_PORT

    fun getConfigurationSummary(): String {
        val config = getServerConfiguration()
        return "NetworkConfig[IP=${config.serverIp}, Legacy=${config.legacyPort}, JSON=${config.jsonPort}]"
    }
}

data class ServerConfiguration(
    val serverIp: String,
    val legacyPort: Int,
    val jsonPort: Int,
) {
    fun getLegacyAddress(): String = "$serverIp:$legacyPort"

    fun getJsonAddress(): String = "$serverIp:$jsonPort"

    fun isValid(): Boolean {
        return NetworkConfiguration.isValidIpAddress(serverIp) &&
                NetworkConfiguration.isValidPort(legacyPort) &&
                NetworkConfiguration.isValidPort(jsonPort)
    }
}
