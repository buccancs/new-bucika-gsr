package com.multisensor.recording.security

import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

interface PrivacyInterface {
    val privacySettings: StateFlow<PrivacySettings>
    val consentStatus: StateFlow<ConsentStatus>

    suspend fun hasValidConsent(): Boolean
    suspend fun recordConsent(consentData: ConsentData): Result<Unit>
    suspend fun withdrawConsent(): Result<Unit>
    suspend fun updateConsentVersion(newVersion: Int): Result<Unit>

    suspend fun enableDataAnonymization(enabled: Boolean): Result<Unit>
    suspend fun enableFaceBlurring(enabled: Boolean): Result<Unit>
    suspend fun enableMetadataStripping(enabled: Boolean): Result<Unit>
    suspend fun setDataRetentionPeriod(days: Int): Result<Unit>

    suspend fun anonymizeImage(bitmap: Bitmap): Result<Bitmap>
    suspend fun stripMetadata(filePath: String): Result<Unit>
    suspend fun isDataRetentionExpired(dataTimestamp: Long): Boolean

    fun getPrivacySettings(): PrivacySettings
    fun getConsentInfo(): ConsentInfo
}

data class PrivacySettings(
    val dataAnonymizationEnabled: Boolean = true,
    val faceBlurringEnabled: Boolean = true,
    val metadataStrippingEnabled: Boolean = true,
    val dataRetentionDays: Int = 365
)

data class ConsentData(
    val participantId: String? = null,
    val studyId: String? = null,
    val consentText: String = "",
    val consentVersion: Int = 1
)

data class ConsentInfo(
    val consentGiven: Boolean,
    val consentDate: Long,
    val consentVersion: Int,
    val participantId: String?,
    val studyId: String?
)

sealed class ConsentStatus {
    object Valid : ConsentStatus()
    object Missing : ConsentStatus()
    object Expired : ConsentStatus()
    object Withdrawn : ConsentStatus()
    data class UpdateRequired(val currentVersion: Int, val requiredVersion: Int) : ConsentStatus()
}

class PrivacyManagerAdapter(
    private val privacyManager: PrivacyManager
) : PrivacyInterface {

    private val _privacySettings = kotlinx.coroutines.flow.MutableStateFlow(PrivacySettings())
    private val _consentStatus = kotlinx.coroutines.flow.MutableStateFlow<ConsentStatus>(ConsentStatus.Missing)

    override val privacySettings: StateFlow<PrivacySettings> = _privacySettings
    override val consentStatus: StateFlow<ConsentStatus> = _consentStatus

    init {
        updatePrivacySettingsFromManager()
        updateConsentStatusFromManager()
    }

    override suspend fun hasValidConsent(): Boolean {
        val isValid = privacyManager.hasValidConsent()
        updateConsentStatusFromManager()
        return isValid
    }

    override suspend fun recordConsent(consentData: ConsentData): Result<Unit> {
        return try {
            privacyManager.recordConsent(consentData.participantId, consentData.studyId)
            updateConsentStatusFromManager()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun withdrawConsent(): Result<Unit> {
        return try {
            val success = privacyManager.withdrawConsent()
            if (success) {
                _consentStatus.value = ConsentStatus.Withdrawn
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to withdraw consent"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateConsentVersion(newVersion: Int): Result<Unit> {

        return Result.failure(UnsupportedOperationException("Consent version update not yet implemented"))
    }

    override suspend fun enableDataAnonymization(enabled: Boolean): Result<Unit> {
        return try {

            val settings = privacyManager.getAnonymizationSettings()
            privacyManager.configureAnonymization(
                enableDataAnonymization = enabled,
                enableFaceBlurring = settings.faceBlurringEnabled,
                enableMetadataStripping = settings.metadataStrippingEnabled
            )
            updatePrivacySettingsFromManager()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enableFaceBlurring(enabled: Boolean): Result<Unit> {
        return try {
            val settings = privacyManager.getAnonymizationSettings()
            privacyManager.configureAnonymization(
                enableDataAnonymization = settings.dataAnonymizationEnabled,
                enableFaceBlurring = enabled,
                enableMetadataStripping = settings.metadataStrippingEnabled
            )
            updatePrivacySettingsFromManager()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enableMetadataStripping(enabled: Boolean): Result<Unit> {
        return try {
            val settings = privacyManager.getAnonymizationSettings()
            privacyManager.configureAnonymization(
                enableDataAnonymization = settings.dataAnonymizationEnabled,
                enableFaceBlurring = settings.faceBlurringEnabled,
                enableMetadataStripping = enabled
            )
            updatePrivacySettingsFromManager()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setDataRetentionPeriod(days: Int): Result<Unit> {
        return try {
            privacyManager.setDataRetentionPeriod(days)
            updatePrivacySettingsFromManager()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun anonymizeImage(bitmap: Bitmap): Result<Bitmap> {
        return try {

            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stripMetadata(filePath: String): Result<Unit> {
        return try {

            privacyManager.anonymizeMetadata(emptyMap())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isDataRetentionExpired(dataTimestamp: Long): Boolean {
        return privacyManager.shouldDeleteData(dataTimestamp)
    }

    override fun getPrivacySettings(): PrivacySettings {
        val anonymizationSettings = privacyManager.getAnonymizationSettings()
        return PrivacySettings(
            dataAnonymizationEnabled = anonymizationSettings.dataAnonymizationEnabled,
            faceBlurringEnabled = anonymizationSettings.faceBlurringEnabled,
            metadataStrippingEnabled = anonymizationSettings.metadataStrippingEnabled,
            dataRetentionDays = privacyManager.getDataRetentionDays()
        )
    }

    override fun getConsentInfo(): ConsentInfo {
        val managerInfo = privacyManager.getConsentInfo()
        return ConsentInfo(
            consentGiven = managerInfo.consentGiven,
            consentDate = managerInfo.consentDate,
            consentVersion = managerInfo.consentVersion,
            participantId = managerInfo.participantId,
            studyId = managerInfo.studyId
        )
    }

    private fun updatePrivacySettingsFromManager() {
        _privacySettings.value = getPrivacySettings()
    }

    private fun updateConsentStatusFromManager() {
        val consentInfo = privacyManager.getConsentInfo()
        _consentStatus.value = when {
            !consentInfo.consentGiven -> ConsentStatus.Missing
            privacyManager.hasValidConsent() -> ConsentStatus.Valid
            else -> ConsentStatus.Expired
        }
    }
}

interface DataCleanupService {
    suspend fun deleteExpiredData(): Result<Int>
    suspend fun deleteAllUserData(): Result<Unit>
    suspend fun deleteSessionData(sessionId: String): Result<Unit>
    suspend fun schedulePeriodicCleanup()
    suspend fun cancelPeriodicCleanup()
}

interface PrivacyUiService {
    suspend fun showConsentDialog(): Result<ConsentData?>
    suspend fun showConsentUpdateDialog(currentVersion: Int, newVersion: Int): Result<Boolean>
    suspend fun showDataRetentionNotification(expiringDataCount: Int)
    suspend fun showConsentWithdrawnConfirmation()
}