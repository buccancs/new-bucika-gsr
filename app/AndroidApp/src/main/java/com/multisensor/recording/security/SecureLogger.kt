package com.multisensor.recording.security

import android.content.Context
import com.multisensor.recording.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val baseLogger: Logger,
    private val securityUtils: SecurityUtils
) {

    companion object {
        private val SENSITIVE_PATTERNS = mapOf<Regex, (MatchResult) -> String>(
            Regex("(?i)(token|password|secret|key|auth)\\s*[:=]\\s*[\"']?([^\\s\"']{8,})[\"']?") to { matchResult ->
                "${matchResult.groupValues[1]}=***"
            },

            Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})") to { _ -> "**:**:**:**:**:**" },

            Regex("\\b(?!127\\.0\\.0\\.1|localhost)(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b") to { _ -> "***.***.***.***" },

            Regex("\\+?[1-9]\\d{1,14}\\b") to { _ -> "***-***-****" },

            Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b") to { _ -> "***@***.***" },

            Regex("[A-Za-z0-9+/]{20,}={0,2}") to { _ -> "***[base64]***" },

            Regex("\\b[0-9A-Fa-f]{32,}\\b") to { _ -> "***[hex]***" },

            Regex("\\bserial[\"'\\s]*[:=][\"'\\s]*([A-Za-z0-9]{2})([A-Za-z0-9]+)([A-Za-z0-9]{2})[\"'\\s]*") to { matchResult ->
                "serial=${matchResult.groupValues[1]}***${matchResult.groupValues[3]}"
            },

            Regex("at\\s+(com\\.multisensor\\.recording\\.security\\.[^\\s]+)\\([^)]+\\)") to { matchResult ->
                "at ${matchResult.groupValues[1].substringBeforeLast('.')}.**(***)"
            }
        )
    }

    fun verbose(message: String, throwable: Throwable? = null) {
        baseLogger.verbose(sanitizeMessage(message), sanitizeThrowable(throwable))
    }

    fun debug(message: String, throwable: Throwable? = null) {
        baseLogger.debug(sanitizeMessage(message), sanitizeThrowable(throwable))
    }

    fun info(message: String, throwable: Throwable? = null) {
        baseLogger.info(sanitizeMessage(message), sanitizeThrowable(throwable))
    }

    fun warning(message: String, throwable: Throwable? = null) {
        baseLogger.warning(sanitizeMessage(message), sanitizeThrowable(throwable))
    }

    fun error(message: String, throwable: Throwable? = null) {
        baseLogger.error(sanitizeMessage(message), sanitizeThrowable(throwable))
    }

    fun logSecurityEvent(event: SecurityEvent, details: String = "") {
        val sanitizedDetails = sanitizeMessage(details)
        val logMessage = "SECURITY EVENT: ${event.name} - $sanitizedDetails"

        when (event.severity) {
            SecurityEventSeverity.LOW -> info(logMessage)
            SecurityEventSeverity.MEDIUM -> warning(logMessage)
            SecurityEventSeverity.HIGH -> error(logMessage)
            SecurityEventSeverity.CRITICAL -> error(logMessage)
        }
    }

    fun logAuthEvent(event: AuthEvent, remoteAddress: String? = null, success: Boolean = false) {
        val sanitizedAddress = remoteAddress?.let { sanitizeMessage(it) } ?: "unknown"
        val status = if (success) "SUCCESS" else "FAILURE"
        val logMessage = "AUTH EVENT: ${event.name} from $sanitizedAddress - $status"

        if (success) {
            info(logMessage)
        } else {
            warning(logMessage)
        }
    }

    private fun sanitizeMessage(message: String): String {
        var sanitized = securityUtils.sanitizeForLogging(message)

        SENSITIVE_PATTERNS.forEach { (pattern, replacement) ->
            sanitized = pattern.replace(sanitized, replacement)
        }

        return sanitized
    }

    private fun sanitizeThrowable(throwable: Throwable?): Throwable? {
        if (throwable == null) return null

        if (throwable.message?.contains("password", ignoreCase = true) == true ||
            throwable.message?.contains("token", ignoreCase = true) == true ||
            throwable.message?.contains("secret", ignoreCase = true) == true) {

            val sanitizedMessage = sanitizeMessage(throwable.message ?: "")
            return RuntimeException(sanitizedMessage, null)
        }

        return throwable
    }

    fun logNetworkEvent(event: NetworkEvent, remoteAddress: String? = null, dataSize: Long = 0) {
        val sanitizedAddress = remoteAddress?.let { sanitizeMessage(it) } ?: "unknown"
        val logMessage = "NETWORK EVENT: ${event.name} with $sanitizedAddress, ${dataSize} bytes"

        when (event.severity) {
            NetworkEventSeverity.INFO -> info(logMessage)
            NetworkEventSeverity.WARNING -> warning(logMessage)
            NetworkEventSeverity.ERROR -> error(logMessage)
        }
    }

    fun logFileEvent(event: FileEvent, filePath: String, success: Boolean = true) {
        val fileName = filePath.substringAfterLast('/')
        val status = if (success) "SUCCESS" else "FAILURE"
        val logMessage = "FILE EVENT: ${event.name} - $fileName - $status"

        if (success) {
            debug(logMessage)
        } else {
            warning(logMessage)
        }
    }

    fun getCurrentLogFilePath(): String? = baseLogger.getCurrentLogFilePath()
    fun getLogStatistics(): Logger.LogStatistics = baseLogger.getLogStatistics()
    fun logSystemInfo() = baseLogger.logSystemInfo()
    fun cleanup() = baseLogger.cleanup()
}

enum class SecurityEvent(val severity: SecurityEventSeverity) {
    ENCRYPTION_KEY_GENERATED(SecurityEventSeverity.LOW),
    AUTHENTICATION_TOKEN_GENERATED(SecurityEventSeverity.MEDIUM),
    INVALID_AUTHENTICATION_ATTEMPT(SecurityEventSeverity.HIGH),
    ENCRYPTION_FAILURE(SecurityEventSeverity.HIGH),
    CERTIFICATE_VALIDATION_FAILED(SecurityEventSeverity.CRITICAL),
    UNAUTHORIZED_ACCESS_ATTEMPT(SecurityEventSeverity.CRITICAL)
}

enum class SecurityEventSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class AuthEvent {
    AUTHENTICATION_STARTED,
    AUTHENTICATION_SUCCESS,
    AUTHENTICATION_FAILURE,
    TOKEN_VALIDATION_FAILED,
    CONNECTION_AUTHENTICATED,
    CONNECTION_UNAUTHENTICATED
}

enum class NetworkEvent(val severity: NetworkEventSeverity) {
    CONNECTION_ESTABLISHED(NetworkEventSeverity.INFO),
    CONNECTION_LOST(NetworkEventSeverity.WARNING),
    TLS_HANDSHAKE_SUCCESS(NetworkEventSeverity.INFO),
    TLS_HANDSHAKE_FAILED(NetworkEventSeverity.ERROR),
    DATA_TRANSMITTED(NetworkEventSeverity.INFO),
    DATA_RECEIVED(NetworkEventSeverity.INFO),
    CERTIFICATE_PINNING_FAILED(NetworkEventSeverity.ERROR)
}

enum class NetworkEventSeverity {
    INFO, WARNING, ERROR
}

enum class FileEvent {
    FILE_ENCRYPTED,
    FILE_DECRYPTED,
    FILE_DELETED,
    FILE_CREATED,
    FILE_ACCESS_DENIED,
    FILE_CORRUPTION_DETECTED
}
