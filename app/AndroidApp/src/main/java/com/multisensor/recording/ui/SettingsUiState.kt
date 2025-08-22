package com.multisensor.recording.ui

data class SettingsUiState(
    val videoResolution: String = "1920x1080",
    val frameRate: Int = 30,
    val videoQuality: String = "high",
    val audioEnabled: Boolean = true,
    val audioSampleRate: Int = 44100,

    val storageLocation: String = "internal",
    val maxRecordingDuration: Int = 3600,
    val autoDeleteOldFiles: Boolean = false,
    val storageThreshold: Float = 0.9f,

    val serverIpAddress: String = "192.168.1.100",
    val legacyPort: Int = 8080,
    val jsonPort: Int = 9000,
    val autoConnectToServer: Boolean = true,
    val connectionTimeout: Int = 30,

    val enableShimmer: Boolean = true,
    val enableThermalCamera: Boolean = true,
    val enableWebcam: Boolean = true,
    val shimmerSamplingRate: Int = 512,
    val thermalFrameRate: Int = 9,

    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "en",
    val showDebugInfo: Boolean = false,
    val enableHapticFeedback: Boolean = true,
    val keepScreenOn: Boolean = true,

    val enableAnalytics: Boolean = false,
    val enableCrashReporting: Boolean = true,
    val shareUsageData: Boolean = false,

    val enableNotifications: Boolean = true,
    val notifyOnRecordingStart: Boolean = true,
    val notifyOnRecordingComplete: Boolean = true,
    val notifyOnErrors: Boolean = true,
    val notificationSound: Boolean = true,

    val enableDeveloperMode: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO,
    val enablePerformanceMonitoring: Boolean = false,
    val maxLogFileSize: Int = 10,

    val autoCalibration: Boolean = true,
    val calibrationInterval: Int = 24,
    val calibrationQualityThreshold: Double = 0.95,

    val enableAutoBackup: Boolean = false,
    val backupLocation: String = "cloud",
    val backupFrequency: BackupFrequency = BackupFrequency.WEEKLY,
    val includeRawData: Boolean = false,

    val isLoading: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val expandedSections: Set<SettingsSection> = emptySet(),

    val errorMessage: String? = null,
    val showErrorDialog: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
) {

    val canSaveSettings: Boolean
        get() = hasUnsavedChanges &&
                validationErrors.isEmpty() &&
                !isLoading

    val canResetSettings: Boolean
        get() = !isLoading

    val isNetworkConfigValid: Boolean
        get() = isValidIpAddress(serverIpAddress) &&
                legacyPort in 1024..65535 &&
                jsonPort in 1024..65535 &&
                legacyPort != jsonPort

    val isStorageConfigValid: Boolean
        get() = maxRecordingDuration > 0 &&
                storageThreshold in 0.1f..1.0f

    val isDeviceConfigValid: Boolean
        get() = shimmerSamplingRate > 0 &&
                thermalFrameRate > 0 &&
                audioSampleRate > 0

    val isConfigurationValid: Boolean
        get() = isNetworkConfigValid &&
                isStorageConfigValid &&
                isDeviceConfigValid

    val formattedStorageThreshold: String
        get() = "${(storageThreshold * 100).toInt()}%"

    val formattedMaxDuration: String
        get() = when {
            maxRecordingDuration >= 3600 -> "${maxRecordingDuration / 3600}h"
            maxRecordingDuration >= 60 -> "${maxRecordingDuration / 60}m"
            else -> "${maxRecordingDuration}s"
        }

    fun isSectionExpanded(section: SettingsSection): Boolean {
        return expandedSections.contains(section)
    }

    fun getValidationError(field: String): String? {
        return validationErrors[field]
    }
}

enum class AppTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow System")
}

enum class LogLevel(val displayName: String, val priority: Int) {
    VERBOSE("Verbose", 2),
    DEBUG("Debug", 3),
    INFO("Info", 4),
    WARN("Warning", 5),
    ERROR("Error", 6)
}

enum class BackupFrequency(val displayName: String, val intervalHours: Int) {
    DAILY("Daily", 24),
    WEEKLY("Weekly", 168),
    MONTHLY("Monthly", 720),
    MANUAL("Manual Only", 0)
}

enum class SettingsSection(val displayName: String) {
    RECORDING("Recording"),
    STORAGE("Storage"),
    NETWORK("Network"),
    DEVICES("Devices"),
    INTERFACE("Interface"),
    PRIVACY("Privacy"),
    NOTIFICATIONS("Notifications"),
    ADVANCED("Advanced"),
    CALIBRATION("Calibration"),
    BACKUP("Backup & Sync")
}

enum class VideoResolution(val displayName: String, val value: String) {
    UHD_4K("4K Ultra HD (3840x2160)", "3840x2160"),
    FULL_HD("Full HD (1920x1080)", "1920x1080"),
    HD("HD (1280x720)", "1280x720"),
    STANDARD("Standard (854x480)", "854x480"),
    LOW("Low (640x360)", "640x360")
}

enum class VideoQuality(val displayName: String, val value: String) {
    ULTRA_HIGH("Ultra High Quality", "ultra_high"),
    HIGH("High Quality", "high"),
    MEDIUM("Medium Quality", "medium"),
    LOW("Low Quality", "low"),
    VERY_LOW("Very Low Quality", "very_low")
}

enum class StorageLocation(val displayName: String, val value: String) {
    INTERNAL("Internal Storage", "internal"),
    EXTERNAL("External Storage (SD Card)", "external"),
    PRIVATE("App Private Directory", "private"),
    DOWNLOADS("Downloads Folder", "downloads")
}

private fun isValidIpAddress(ip: String): Boolean {
    val parts = ip.split(".")
    if (parts.size != 4) return false

    return parts.all { part ->
        try {
            val num = part.toInt()
            num in 0..255
        } catch (e: NumberFormatException) {
            false
        }
    }
}
