package com.multisensor.recording.security

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.multisensor.recording.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {

    companion object {
        private const val TAG = "PrivacyManager"
        private const val PREFS_FILE = "privacy_preferences"
        private const val KEY_CONSENT_GIVEN = "consent_given"
        private const val KEY_CONSENT_DATE = "consent_date"
        private const val KEY_CONSENT_VERSION = "consent_version"
        private const val KEY_DATA_ANONYMIZATION = "data_anonymization_enabled"
        private const val KEY_FACE_BLURRING = "face_blurring_enabled"
        private const val KEY_METADATA_STRIPPING = "metadata_stripping_enabled"
        private const val KEY_PARTICIPANT_ID = "participant_id"
        private const val KEY_STUDY_ID = "study_id"
        private const val KEY_DATA_RETENTION_DAYS = "data_retention_days"

        // Enhanced security constants - thesis compliant
        private const val KEY_ENCRYPT_DATA_AT_REST = "encrypt_data_at_rest"
        private const val KEY_SECURE_FILE_DELETION = "secure_file_deletion"
        private const val KEY_LOG_SECURITY_EVENTS = "log_security_events"
        private const val CURRENT_CONSENT_VERSION = 1
        private const val DEFAULT_RETENTION_DAYS = 365

        // Face blurring parameters - thesis specification
        private const val FACE_BLUR_RADIUS = 25f
        private const val FACE_DETECTION_MIN_SIZE = 100

        // AES-GCM encryption parameters - thesis security requirements
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }

    // Enhanced security features
    private var faceDetector: CascadeClassifier? = null
    private val securityEvents = ConcurrentHashMap<String, SecurityEvent>()
    private val encryptionKeys = ConcurrentHashMap<String, SecretKey>()

    // Security event tracking
    data class SecurityEvent(
        val timestamp: Long,
        val eventType: String,
        val severity: SecuritySeverity,
        val description: String,
        val deviceInfo: String? = null
    )

    enum class SecuritySeverity {
        INFO, WARNING, CRITICAL
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true)  // Enhanced security for thesis compliance
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also {
                // Initialize enhanced privacy features
                initializeEnhancedPrivacyFeatures()
                logSecurityEvent("privacy_manager_init", SecuritySeverity.INFO,
                    "PrivacyManager initialized with AES-256-GCM encryption")
            }
        } catch (e: Exception) {
            logger.error("Failed to create encrypted preferences, falling back to regular preferences", e)
            logSecurityEvent("encrypted_prefs_failed", SecuritySeverity.CRITICAL,
                "Failed to initialize encrypted preferences: ${e.message}")
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    // Initialize enhanced privacy features
    private fun initializeEnhancedPrivacyFeatures() {
        try {
            // Set thesis-compliant default privacy settings
            if (!securePrefs.contains(KEY_FACE_BLURRING)) {
                securePrefs.edit()
                    .putBoolean(KEY_FACE_BLURRING, false)  // Disabled - no faces in video, only hands
                    .putBoolean(KEY_ENCRYPT_DATA_AT_REST, true)  // Default enabled for security
                    .putBoolean(KEY_SECURE_FILE_DELETION, true)  // Default enabled for privacy
                    .putBoolean(KEY_LOG_SECURITY_EVENTS, true)  // Default enabled for audit
                    .apply()
            }

            // Initialize face detection if enabled (disabled by default for hands-only video)
            if (isFaceBlurringEnabled()) {
                initializeFaceDetection()
            }

            logSecurityEvent("enhanced_features_init", SecuritySeverity.INFO,
                "Enhanced privacy features initialized")

        } catch (e: Exception) {
            logger.error("Failed to initialize enhanced privacy features", e)
        }
    }

    // Initialize OpenCV face detection for privacy protection
    private fun initializeFaceDetection() {
        try {
            // Copy face detection cascade to internal storage if needed
            val cascadeFile = File(context.filesDir, "haarcascade_frontalface_alt.xml")

            if (!cascadeFile.exists()) {
                // Copy from assets (would need to be included in app assets)
                context.assets.open("opencv/haarcascade_frontalface_alt.xml").use { inputStream ->
                    FileOutputStream(cascadeFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            // Initialize face detector
            faceDetector = CascadeClassifier(cascadeFile.absolutePath)

            if (faceDetector?.empty() == true) {
                logger.error("Failed to load face detection cascade")
                faceDetector = null
                logSecurityEvent("face_detection_failed", SecuritySeverity.WARNING,
                    "Face detection cascade failed to load")
            } else {
                logger.info("Face detection initialized successfully")
                logSecurityEvent("face_detection_init", SecuritySeverity.INFO,
                    "Face detection initialized for privacy protection")
            }

        } catch (e: Exception) {
            logger.error("Failed to initialize face detection", e)
            faceDetector = null
            logSecurityEvent("face_detection_error", SecuritySeverity.WARNING,
                "Face detection initialization error: ${e.message}")
        }
    }

    fun hasValidConsent(): Boolean {
        val consentGiven = securePrefs.getBoolean(KEY_CONSENT_GIVEN, false)
        val consentVersion = securePrefs.getInt(KEY_CONSENT_VERSION, 0)

        return consentGiven && consentVersion >= CURRENT_CONSENT_VERSION
    }

    fun recordConsent(participantId: String? = null, studyId: String? = null) {
        securePrefs.edit()
            .putBoolean(KEY_CONSENT_GIVEN, true)
            .putLong(KEY_CONSENT_DATE, System.currentTimeMillis())
            .putInt(KEY_CONSENT_VERSION, CURRENT_CONSENT_VERSION)
            .apply {
                participantId?.let { putString(KEY_PARTICIPANT_ID, it) }
                studyId?.let { putString(KEY_STUDY_ID, it) }
            }
            .apply()

        logger.info("User consent recorded for study: ${studyId ?: "unknown"}")
    }

    fun withdrawConsent(): Boolean {
        return try {
            securePrefs.edit()
                .putBoolean(KEY_CONSENT_GIVEN, false)
                .putLong("consent_withdrawn_date", System.currentTimeMillis())
                .apply()

            logger.info("User consent withdrawn - data should be deleted")
            true
        } catch (e: Exception) {
            logger.error("Failed to withdraw consent", e)
            false
        }
    }

    fun getConsentInfo(): ConsentInfo {
        return ConsentInfo(
            consentGiven = securePrefs.getBoolean(KEY_CONSENT_GIVEN, false),
            consentDate = securePrefs.getLong(KEY_CONSENT_DATE, 0),
            consentVersion = securePrefs.getInt(KEY_CONSENT_VERSION, 0),
            participantId = securePrefs.getString(KEY_PARTICIPANT_ID, null),
            studyId = securePrefs.getString(KEY_STUDY_ID, null)
        )
    }

    fun configureAnonymization(
        enableDataAnonymization: Boolean,
        enableFaceBlurring: Boolean,
        enableMetadataStripping: Boolean
    ) {
        securePrefs.edit()
            .putBoolean(KEY_DATA_ANONYMIZATION, enableDataAnonymization)
            .putBoolean(KEY_FACE_BLURRING, enableFaceBlurring)
            .putBoolean(KEY_METADATA_STRIPPING, enableMetadataStripping)
            .apply()

        logger.info("Data anonymization configured: anonymize=$enableDataAnonymization, blur=$enableFaceBlurring, strip=$enableMetadataStripping")
    }

    fun getAnonymizationSettings(): AnonymizationSettings {
        return AnonymizationSettings(
            dataAnonymizationEnabled = securePrefs.getBoolean(KEY_DATA_ANONYMIZATION, false),
            faceBlurringEnabled = securePrefs.getBoolean(KEY_FACE_BLURRING, false),  // Disabled - no faces in video, only hands
            metadataStrippingEnabled = securePrefs.getBoolean(KEY_METADATA_STRIPPING, true)
        )
    }

    // Enhanced privacy feature accessors
    fun isFaceBlurringEnabled(): Boolean {
        return securePrefs.getBoolean(KEY_FACE_BLURRING, false)  // Disabled - no faces in video, only hands
    }

    fun isDataEncryptionEnabled(): Boolean {
        return securePrefs.getBoolean(KEY_ENCRYPT_DATA_AT_REST, true)
    }

    fun isSecureFileDeletionEnabled(): Boolean {
        return securePrefs.getBoolean(KEY_SECURE_FILE_DELETION, true)
    }

    fun isSecurityLoggingEnabled(): Boolean {
        return securePrefs.getBoolean(KEY_LOG_SECURITY_EVENTS, true)
    }

    fun setDataRetentionPeriod(days: Int) {
        securePrefs.edit()
            .putInt(KEY_DATA_RETENTION_DAYS, days)
            .apply()

        logger.info("Data retention period set to $days days")
    }

    fun getDataRetentionDays(): Int {
        return securePrefs.getInt(KEY_DATA_RETENTION_DAYS, DEFAULT_RETENTION_DAYS)
    }

    fun generateAnonymousParticipantId(): String {
        val uuid = UUID.randomUUID().toString()
        val anonymousId = "ANON_${uuid.substring(0, 8).uppercase()}"

        securePrefs.edit()
            .putString(KEY_PARTICIPANT_ID, anonymousId)
            .apply()

        return anonymousId
    }

    fun getParticipantId(): String? {
        return securePrefs.getString(KEY_PARTICIPANT_ID, null)
    }

    fun shouldDeleteData(dataTimestamp: Long): Boolean {
        val retentionPeriodMs = getDataRetentionDays() * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        return (currentTime - dataTimestamp) > retentionPeriodMs
    }

    suspend fun anonymizeMetadata(metadata: Map<String, Any>): Map<String, Any> = withContext(Dispatchers.Default) {
        val anonymizedMetadata = metadata.toMutableMap()

        if (getAnonymizationSettings().metadataStrippingEnabled) {
            val keysToRemove = listOf(
                "device_id", "serial_number", "mac_address", "imei",
                "phone_number", "email", "user_name", "device_name",
                "network_ssid", "ip_address", "location", "gps"
            )

            keysToRemove.forEach { key ->
                anonymizedMetadata.remove(key)
            }

            anonymizedMetadata["participant_id"] = getParticipantId() ?: generateAnonymousParticipantId()
            anonymizedMetadata["session_id"] = "SESSION_${UUID.randomUUID().toString().substring(0, 8)}"
            anonymizedMetadata["device_type"] = android.os.Build.MODEL.replace(Regex("[0-9]+"), "X")
        }

        anonymizedMetadata.toMap()
    }

    suspend fun generatePrivacyReport(): PrivacyReport = withContext(Dispatchers.Default) {
        val consentInfo = getConsentInfo()
        val anonymizationSettings = getAnonymizationSettings()

        PrivacyReport(
            consentInfo = consentInfo,
            anonymizationSettings = anonymizationSettings,
            dataRetentionDays = getDataRetentionDays(),
            reportGeneratedAt = System.currentTimeMillis(),
            dataCollectionPurpose = "Multi-sensor physiological research data collection",
            dataTypes = listOf(
                "Video recordings (RGB)",
                "Thermal camera data",
                "Shimmer sensor data (GSR, accelerometer, etc.)",
                "Device metadata",
                "Session timestamps"
            ),
            dataProcessingBasis = "Research consent",
            dataStorageLocation = "Local device storage (encrypted)",
            thirdPartySharing = "None"
        )
    }

    suspend fun clearAllPrivacyData(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            securePrefs.edit().clear().apply()
            logger.info("All privacy data cleared successfully")
            logSecurityEvent("privacy_data_cleared", SecuritySeverity.INFO,
                "All privacy data cleared from encrypted storage")
            true
        } catch (e: Exception) {
            logger.error("Failed to clear privacy data", e)
            logSecurityEvent("privacy_clear_failed", SecuritySeverity.CRITICAL,
                "Failed to clear privacy data: ${e.message}")
            false
        }
    }

    // === ENHANCED PRIVACY FEATURES FOR THESIS COMPLIANCE ===

    /**
     * Apply face blurring to protect participant privacy in video recordings
     * Implements OpenCV-based face detection with Gaussian blur as claimed in thesis
     */
    suspend fun applyFaceBlurring(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        if (!isFaceBlurringEnabled() || faceDetector == null) {
            return@withContext bitmap
        }

        try {
            // Convert bitmap to OpenCV Mat
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)

            // Convert to grayscale for face detection
            val grayMat = Mat()
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)

            // Detect faces
            val faces = MatOfRect()
            faceDetector?.detectMultiScale(
                grayMat,
                faces,
                1.1,
                3,
                0,
                Size(FACE_DETECTION_MIN_SIZE.toDouble(), FACE_DETECTION_MIN_SIZE.toDouble()),
                Size()
            )

            // Blur detected faces
            val faceArray = faces.toArray()
            if (faceArray.isNotEmpty()) {
                for (face in faceArray) {
                    val faceRegion = Mat(mat, face)

                    // Apply Gaussian blur to face region
                    val blurredFace = Mat()
                    val kernelSize = Size(
                        (FACE_BLUR_RADIUS * 2 + 1).toDouble(),
                        (FACE_BLUR_RADIUS * 2 + 1).toDouble()
                    )
                    Imgproc.GaussianBlur(faceRegion, blurredFace, kernelSize, 0.0)

                    // Copy blurred face back to original image
                    blurredFace.copyTo(faceRegion)
                }

                logSecurityEvent("face_blurring_applied", SecuritySeverity.INFO,
                    "Applied face blurring to ${faceArray.size} detected faces")
            }

            // Convert back to bitmap
            val result = bitmap.copy(
                bitmap.config ?: when {
                    bitmap.hasAlpha() -> Bitmap.Config.ARGB_8888
                    else -> Bitmap.Config.RGB_565
                },
                true
            )
            Utils.matToBitmap(mat, result)

            result

        } catch (e: Exception) {
            logger.error("Failed to apply face blurring", e)
            logSecurityEvent("face_blurring_failed", SecuritySeverity.WARNING,
                "Face blurring failed: ${e.message}")
            bitmap
        }
    }

    /**
     * Encrypt sensitive data using AES-GCM encryption as specified in thesis
     */
    suspend fun encryptSensitiveData(data: ByteArray, keyAlias: String = "default"): EncryptedData? = withContext(Dispatchers.Default) {
        if (!isDataEncryptionEnabled()) {
            return@withContext EncryptedData(data, ByteArray(0), keyAlias, false)
        }

        try {
            // Generate or retrieve AES key
            val secretKey = getOrCreateSecretKey(keyAlias)

            // Generate random IV
            val iv = ByteArray(GCM_IV_LENGTH)
            java.security.SecureRandom().nextBytes(iv)

            // Initialize cipher
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

            // Encrypt data
            val encryptedData = cipher.doFinal(data)

            logSecurityEvent("data_encrypted", SecuritySeverity.INFO,
                "Encrypted ${data.size} bytes using AES-GCM")

            EncryptedData(encryptedData, iv, keyAlias, true)

        } catch (e: Exception) {
            logger.error("Failed to encrypt data", e)
            logSecurityEvent("encryption_failed", SecuritySeverity.CRITICAL,
                "Data encryption failed: ${e.message}")
            null
        }
    }

    /**
     * Decrypt previously encrypted data
     */
    suspend fun decryptSensitiveData(encryptedData: EncryptedData): ByteArray? = withContext(Dispatchers.Default) {
        if (!encryptedData.isEncrypted) {
            return@withContext encryptedData.data
        }

        try {
            // Retrieve secret key
            val secretKey = getOrCreateSecretKey(encryptedData.keyAlias)

            // Initialize cipher for decryption
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            // Decrypt data
            val decryptedData = cipher.doFinal(encryptedData.data)

            logSecurityEvent("data_decrypted", SecuritySeverity.INFO,
                "Decrypted ${decryptedData.size} bytes")

            decryptedData

        } catch (e: Exception) {
            logger.error("Failed to decrypt data", e)
            logSecurityEvent("decryption_failed", SecuritySeverity.CRITICAL,
                "Data decryption failed: ${e.message}")
            null
        }
    }

    /**
     * Securely delete file with multiple overwrites for privacy compliance
     */
    suspend fun secureFileDelete(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!isSecureFileDeletionEnabled()) {
            return@withContext file.delete()
        }

        try {
            if (!file.exists()) {
                return@withContext true
            }

            val fileSize = file.length()

            // Perform secure deletion with multiple passes
            FileOutputStream(file).use { fos ->
                // Pass 1: Write zeros
                val zeros = ByteArray(1024)
                var remaining = fileSize
                while (remaining > 0) {
                    val writeSize = minOf(remaining, zeros.size.toLong()).toInt()
                    fos.write(zeros, 0, writeSize)
                    remaining -= writeSize
                }
                fos.flush()

                // Pass 2: Write random data
                val random = java.security.SecureRandom()
                val randomBytes = ByteArray(1024)
                fos.channel.position(0)
                remaining = fileSize
                while (remaining > 0) {
                    val writeSize = minOf(remaining, randomBytes.size.toLong()).toInt()
                    random.nextBytes(randomBytes)
                    fos.write(randomBytes, 0, writeSize)
                    remaining -= writeSize
                }
                fos.flush()

                // Pass 3: Write zeros again
                fos.channel.position(0)
                remaining = fileSize
                while (remaining > 0) {
                    val writeSize = minOf(remaining, zeros.size.toLong()).toInt()
                    fos.write(zeros, 0, writeSize)
                    remaining -= writeSize
                }
                fos.flush()
            }

            // Finally delete the file
            val deleted = file.delete()

            if (deleted) {
                logSecurityEvent("secure_file_delete", SecuritySeverity.INFO,
                    "Securely deleted file: ${file.name} (${fileSize} bytes)")
            }

            deleted

        } catch (e: Exception) {
            logger.error("Failed to securely delete file: ${file.name}", e)
            logSecurityEvent("secure_delete_failed", SecuritySeverity.WARNING,
                "Secure file deletion failed: ${e.message}")
            false
        }
    }

    /**
     * Log security event for audit trail
     */
    fun logSecurityEvent(eventType: String, severity: SecuritySeverity, description: String) {
        if (!isSecurityLoggingEnabled()) {
            return
        }

        val event = SecurityEvent(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            severity = severity,
            description = description,
            deviceInfo = "${android.os.Build.MODEL} ${android.os.Build.VERSION.RELEASE}"
        )

        securityEvents[eventType + "_" + event.timestamp] = event

        // Log to system log based on severity
        when (severity) {
            SecuritySeverity.INFO -> Log.i(TAG, "Security Event: $description")
            SecuritySeverity.WARNING -> Log.w(TAG, "Security Warning: $description")
            SecuritySeverity.CRITICAL -> Log.e(TAG, "Security Critical: $description")
        }

        // Keep only last 1000 events
        if (securityEvents.size > 1000) {
            val oldestKey = securityEvents.keys.minOrNull()
            oldestKey?.let { securityEvents.remove(it) }
        }
    }

    /**
     * Get security events for audit
     */
    fun getSecurityEvents(since: Long = 0): List<SecurityEvent> {
        return securityEvents.values
            .filter { it.timestamp >= since }
            .sortedBy { it.timestamp }
    }

    /**
     * Get privacy compliance report with thesis metrics
     */
    suspend fun getEnhancedPrivacyReport(): EnhancedPrivacyReport = withContext(Dispatchers.Default) {
        val consentInfo = getConsentInfo()
        val anonymizationSettings = getAnonymizationSettings()

        val enabledFeatures = listOf(
            isFaceBlurringEnabled(),
            isDataEncryptionEnabled(),
            isSecureFileDeletionEnabled(),
            isSecurityLoggingEnabled(),
            anonymizationSettings.dataAnonymizationEnabled,
            anonymizationSettings.metadataStrippingEnabled
        ).count { it }

        val compliancePercentage = (enabledFeatures.toFloat() / 6) * 100

        val criticalEvents = securityEvents.values.count { it.severity == SecuritySeverity.CRITICAL }
        val warningEvents = securityEvents.values.count { it.severity == SecuritySeverity.WARNING }

        EnhancedPrivacyReport(
            consentInfo = consentInfo,
            anonymizationSettings = anonymizationSettings,
            dataRetentionDays = getDataRetentionDays(),
            reportGeneratedAt = System.currentTimeMillis(),
            compliancePercentage = compliancePercentage,
            faceBlurringEnabled = isFaceBlurringEnabled(),
            aesEncryptionEnabled = isDataEncryptionEnabled(),
            secureDeleteEnabled = isSecureFileDeletionEnabled(),
            securityLoggingEnabled = isSecurityLoggingEnabled(),
            criticalSecurityEvents = criticalEvents,
            warningSecurityEvents = warningEvents,
            thesisComplianceFeatures = listOf(
                "AES-256-GCM encryption at rest",
                "OpenCV face detection and blurring",
                "Secure multi-pass file deletion",
                "Encrypted SharedPreferences with StrongBox",
                "Comprehensive security event logging",
                "Participant data anonymization"
            )
        )
    }

    // Helper methods
    private fun getOrCreateSecretKey(keyAlias: String): SecretKey {
        encryptionKeys[keyAlias]?.let { return it }

        val keyData = securePrefs.getString("key_$keyAlias", null)

        val secretKey = if (keyData != null) {
            // Load existing key
            val keyBytes = android.util.Base64.decode(keyData, android.util.Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            // Generate new key
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(AES_KEY_SIZE)
            val newKey = keyGenerator.generateKey()

            // Store key
            val keyBytes = newKey.encoded
            val keyString = android.util.Base64.encodeToString(keyBytes, android.util.Base64.DEFAULT)
            securePrefs.edit().putString("key_$keyAlias", keyString).apply()

            newKey
        }

        encryptionKeys[keyAlias] = secretKey
        return secretKey
    }

    // Enhanced data classes
    data class EncryptedData(
        val data: ByteArray,
        val iv: ByteArray,
        val keyAlias: String,
        val isEncrypted: Boolean
    )

    data class ConsentInfo(
        val consentGiven: Boolean,
        val consentDate: Long,
        val consentVersion: Int,
        val participantId: String?,
        val studyId: String?
    )

    data class AnonymizationSettings(
        val dataAnonymizationEnabled: Boolean,
        val faceBlurringEnabled: Boolean,
        val metadataStrippingEnabled: Boolean
    )

    data class PrivacyReport(
        val consentInfo: ConsentInfo,
        val anonymizationSettings: AnonymizationSettings,
        val dataRetentionDays: Int,
        val reportGeneratedAt: Long,
        val dataCollectionPurpose: String,
        val dataTypes: List<String>,
        val dataProcessingBasis: String,
        val dataStorageLocation: String,
        val thirdPartySharing: String
    )

    data class EnhancedPrivacyReport(
        val consentInfo: ConsentInfo,
        val anonymizationSettings: AnonymizationSettings,
        val dataRetentionDays: Int,
        val reportGeneratedAt: Long,
        val compliancePercentage: Float,
        val faceBlurringEnabled: Boolean,
        val aesEncryptionEnabled: Boolean,
        val secureDeleteEnabled: Boolean,
        val securityLoggingEnabled: Boolean,
        val criticalSecurityEvents: Int,
        val warningSecurityEvents: Int,
        val thesisComplianceFeatures: List<String>
    )
}
