package com.multisensor.recording.ui

import org.junit.Assert.*
import org.junit.Test

class SettingsUiStateTest {

    @Test
    fun `canSaveSettings returns true when has changes and no validation errors`() {
        val state = SettingsUiState(
            hasUnsavedChanges = true,
            validationErrors = emptyMap(),
            isLoading = false
        )

        assertTrue("[DEBUG_LOG] Should be able to save settings when valid changes exist", state.canSaveSettings)
    }

    @Test
    fun `canSaveSettings returns false when no unsaved changes`() {
        val state = SettingsUiState(
            hasUnsavedChanges = false,
            validationErrors = emptyMap(),
            isLoading = false
        )

        assertFalse("[DEBUG_LOG] Should not be able to save settings when no changes", state.canSaveSettings)
    }

    @Test
    fun `canSaveSettings returns false when validation errors exist`() {
        val state = SettingsUiState(
            hasUnsavedChanges = true,
            validationErrors = mapOf("serverIpAddress" to "Invalid IP format"),
            isLoading = false
        )

        assertFalse("[DEBUG_LOG] Should not be able to save settings with validation errors", state.canSaveSettings)
    }

    @Test
    fun `canSaveSettings returns false when loading`() {
        val state = SettingsUiState(
            hasUnsavedChanges = true,
            validationErrors = emptyMap(),
            isLoading = true
        )

        assertFalse("[DEBUG_LOG] Should not be able to save settings when loading", state.canSaveSettings)
    }

    @Test
    fun `canResetSettings returns true when not loading`() {
        val state = SettingsUiState(isLoading = false)

        assertTrue("[DEBUG_LOG] Should be able to reset settings when not loading", state.canResetSettings)
    }

    @Test
    fun `canResetSettings returns false when loading`() {
        val state = SettingsUiState(isLoading = true)

        assertFalse("[DEBUG_LOG] Should not be able to reset settings when loading", state.canResetSettings)
    }

    @Test
    fun `isNetworkConfigValid returns true with valid configuration`() {
        val state = SettingsUiState(
            serverIpAddress = "192.168.1.100",
            legacyPort = 8080,
            jsonPort = 9000
        )

        assertTrue("[DEBUG_LOG] Network config should be valid with proper values", state.isNetworkConfigValid)
    }

    @Test
    fun `isNetworkConfigValid returns false with invalid IP address`() {
        val state = SettingsUiState(
            serverIpAddress = "999.999.999.999",
            legacyPort = 8080,
            jsonPort = 9000
        )

        assertFalse("[DEBUG_LOG] Network config should be invalid with bad IP", state.isNetworkConfigValid)
    }

    @Test
    fun `isNetworkConfigValid returns false with invalid port ranges`() {
        val state = SettingsUiState(
            serverIpAddress = "192.168.1.100",
            legacyPort = 500,
            jsonPort = 9000
        )

        assertFalse("[DEBUG_LOG] Network config should be invalid with port below 1024", state.isNetworkConfigValid)
    }

    @Test
    fun `isNetworkConfigValid returns false with same ports`() {
        val state = SettingsUiState(
            serverIpAddress = "192.168.1.100",
            legacyPort = 8080,
            jsonPort = 8080
        )

        assertFalse("[DEBUG_LOG] Network config should be invalid with duplicate ports", state.isNetworkConfigValid)
    }

    @Test
    fun `isStorageConfigValid returns true with valid configuration`() {
        val state = SettingsUiState(
            maxRecordingDuration = 3600,
            storageThreshold = 0.8f
        )

        assertTrue("[DEBUG_LOG] Storage config should be valid with proper values", state.isStorageConfigValid)
    }

    @Test
    fun `isStorageConfigValid returns false with zero duration`() {
        val state = SettingsUiState(
            maxRecordingDuration = 0,
            storageThreshold = 0.8f
        )

        assertFalse("[DEBUG_LOG] Storage config should be invalid with zero duration", state.isStorageConfigValid)
    }

    @Test
    fun `isStorageConfigValid returns false with invalid threshold`() {
        val state = SettingsUiState(
            maxRecordingDuration = 3600,
            storageThreshold = 1.5f
        )

        assertFalse("[DEBUG_LOG] Storage config should be invalid with threshold above 1.0", state.isStorageConfigValid)
    }

    @Test
    fun `isDeviceConfigValid returns true with valid configuration`() {
        val state = SettingsUiState(
            shimmerSamplingRate = 512,
            thermalFrameRate = 9,
            audioSampleRate = 44100
        )

        assertTrue("[DEBUG_LOG] Device config should be valid with proper values", state.isDeviceConfigValid)
    }

    @Test
    fun `isDeviceConfigValid returns false with zero sampling rate`() {
        val state = SettingsUiState(
            shimmerSamplingRate = 0,
            thermalFrameRate = 9,
            audioSampleRate = 44100
        )

        assertFalse(
            "[DEBUG_LOG] Device config should be invalid with zero shimmer sampling rate",
            state.isDeviceConfigValid
        )
    }

    @Test
    fun `isConfigurationValid returns true when all configs valid`() {
        val state = SettingsUiState(
            serverIpAddress = "192.168.1.100",
            legacyPort = 8080,
            jsonPort = 9000,
            maxRecordingDuration = 3600,
            storageThreshold = 0.8f,
            shimmerSamplingRate = 512,
            thermalFrameRate = 9,
            audioSampleRate = 44100
        )

        assertTrue("[DEBUG_LOG] Overall configuration should be valid", state.isConfigurationValid)
    }

    @Test
    fun `isConfigurationValid returns false when any config invalid`() {
        val state = SettingsUiState(
            serverIpAddress = "invalid.ip",
            legacyPort = 8080,
            jsonPort = 9000,
            maxRecordingDuration = 3600,
            storageThreshold = 0.8f,
            shimmerSamplingRate = 512,
            thermalFrameRate = 9,
            audioSampleRate = 44100
        )

        assertFalse(
            "[DEBUG_LOG] Overall configuration should be invalid when network config invalid",
            state.isConfigurationValid
        )
    }

    @Test
    fun `formattedStorageThreshold returns correct percentage`() {
        val state = SettingsUiState(storageThreshold = 0.85f)

        assertEquals(
            "[DEBUG_LOG] Formatted storage threshold should be correct",
            "85%",
            state.formattedStorageThreshold
        )
    }

    @Test
    fun `formattedMaxDuration returns hours for long durations`() {
        val state = SettingsUiState(maxRecordingDuration = 7200)

        assertEquals("[DEBUG_LOG] Formatted duration should show hours", "2h", state.formattedMaxDuration)
    }

    @Test
    fun `formattedMaxDuration returns minutes for medium durations`() {
        val state = SettingsUiState(maxRecordingDuration = 300)

        assertEquals("[DEBUG_LOG] Formatted duration should show minutes", "5m", state.formattedMaxDuration)
    }

    @Test
    fun `formattedMaxDuration returns seconds for short durations`() {
        val state = SettingsUiState(maxRecordingDuration = 45)

        assertEquals("[DEBUG_LOG] Formatted duration should show seconds", "45s", state.formattedMaxDuration)
    }

    @Test
    fun `isSectionExpanded returns true when section is expanded`() {
        val expandedSections = setOf(SettingsSection.NETWORK, SettingsSection.DEVICES)
        val state = SettingsUiState(expandedSections = expandedSections)

        assertTrue("[DEBUG_LOG] Network section should be expanded", state.isSectionExpanded(SettingsSection.NETWORK))
        assertTrue("[DEBUG_LOG] Devices section should be expanded", state.isSectionExpanded(SettingsSection.DEVICES))
        assertFalse(
            "[DEBUG_LOG] Recording section should not be expanded",
            state.isSectionExpanded(SettingsSection.RECORDING)
        )
    }

    @Test
    fun `getValidationError returns correct error message`() {
        val validationErrors = mapOf(
            "serverIpAddress" to "Invalid IP format",
            "legacyPort" to "Port must be between 1024-65535"
        )
        val state = SettingsUiState(validationErrors = validationErrors)

        assertEquals(
            "[DEBUG_LOG] Should return correct error for IP address",
            "Invalid IP format", state.getValidationError("serverIpAddress")
        )
        assertEquals(
            "[DEBUG_LOG] Should return correct error for port",
            "Port must be between 1024-65535", state.getValidationError("legacyPort")
        )
        assertNull(
            "[DEBUG_LOG] Should return null for field without error",
            state.getValidationError("audioSampleRate")
        )
    }

    @Test
    fun `app theme enum contains all expected values`() {
        val themes = AppTheme.values()

        assertTrue("[DEBUG_LOG] Should contain LIGHT theme", themes.contains(AppTheme.LIGHT))
        assertTrue("[DEBUG_LOG] Should contain DARK theme", themes.contains(AppTheme.DARK))
        assertTrue("[DEBUG_LOG] Should contain SYSTEM theme", themes.contains(AppTheme.SYSTEM))

        assertEquals("[DEBUG_LOG] Light theme display name should be correct", "Light", AppTheme.LIGHT.displayName)
        assertEquals("[DEBUG_LOG] Dark theme display name should be correct", "Dark", AppTheme.DARK.displayName)
        assertEquals(
            "[DEBUG_LOG] System theme display name should be correct",
            "Follow System",
            AppTheme.SYSTEM.displayName
        )
    }

    @Test
    fun `log level enum contains all expected values with correct priorities`() {
        val levels = LogLevel.values()

        assertTrue("[DEBUG_LOG] Should contain VERBOSE level", levels.contains(LogLevel.VERBOSE))
        assertTrue("[DEBUG_LOG] Should contain DEBUG level", levels.contains(LogLevel.DEBUG))
        assertTrue("[DEBUG_LOG] Should contain INFO level", levels.contains(LogLevel.INFO))
        assertTrue("[DEBUG_LOG] Should contain WARN level", levels.contains(LogLevel.WARN))
        assertTrue("[DEBUG_LOG] Should contain ERROR level", levels.contains(LogLevel.ERROR))

        assertTrue(
            "[DEBUG_LOG] VERBOSE should have lower priority than DEBUG",
            LogLevel.VERBOSE.priority < LogLevel.DEBUG.priority
        )
        assertTrue(
            "[DEBUG_LOG] DEBUG should have lower priority than INFO",
            LogLevel.DEBUG.priority < LogLevel.INFO.priority
        )
        assertTrue(
            "[DEBUG_LOG] INFO should have lower priority than WARN",
            LogLevel.INFO.priority < LogLevel.WARN.priority
        )
        assertTrue(
            "[DEBUG_LOG] WARN should have lower priority than ERROR",
            LogLevel.WARN.priority < LogLevel.ERROR.priority
        )
    }

    @Test
    fun `backup frequency enum contains all expected values with correct intervals`() {
        val frequencies = BackupFrequency.values()

        assertTrue("[DEBUG_LOG] Should contain DAILY frequency", frequencies.contains(BackupFrequency.DAILY))
        assertTrue("[DEBUG_LOG] Should contain WEEKLY frequency", frequencies.contains(BackupFrequency.WEEKLY))
        assertTrue("[DEBUG_LOG] Should contain MONTHLY frequency", frequencies.contains(BackupFrequency.MONTHLY))
        assertTrue("[DEBUG_LOG] Should contain MANUAL frequency", frequencies.contains(BackupFrequency.MANUAL))

        assertEquals("[DEBUG_LOG] Daily should be 24 hours", 24, BackupFrequency.DAILY.intervalHours)
        assertEquals("[DEBUG_LOG] Weekly should be 168 hours", 168, BackupFrequency.WEEKLY.intervalHours)
        assertEquals("[DEBUG_LOG] Monthly should be 720 hours", 720, BackupFrequency.MONTHLY.intervalHours)
        assertEquals("[DEBUG_LOG] Manual should be 0 hours", 0, BackupFrequency.MANUAL.intervalHours)
    }

    @Test
    fun `settings section enum contains all expected sections`() {
        val sections = SettingsSection.values()

        assertTrue("[DEBUG_LOG] Should contain RECORDING section", sections.contains(SettingsSection.RECORDING))
        assertTrue("[DEBUG_LOG] Should contain STORAGE section", sections.contains(SettingsSection.STORAGE))
        assertTrue("[DEBUG_LOG] Should contain NETWORK section", sections.contains(SettingsSection.NETWORK))
        assertTrue("[DEBUG_LOG] Should contain DEVICES section", sections.contains(SettingsSection.DEVICES))
        assertTrue("[DEBUG_LOG] Should contain INTERFACE section", sections.contains(SettingsSection.INTERFACE))
        assertTrue("[DEBUG_LOG] Should contain PRIVACY section", sections.contains(SettingsSection.PRIVACY))
        assertTrue("[DEBUG_LOG] Should contain NOTIFICATIONS section", sections.contains(SettingsSection.NOTIFICATIONS))
        assertTrue("[DEBUG_LOG] Should contain ADVANCED section", sections.contains(SettingsSection.ADVANCED))
        assertTrue("[DEBUG_LOG] Should contain CALIBRATION section", sections.contains(SettingsSection.CALIBRATION))
        assertTrue("[DEBUG_LOG] Should contain BACKUP section", sections.contains(SettingsSection.BACKUP))
    }

    @Test
    fun `video resolution enum contains all expected resolutions`() {
        val resolutions = VideoResolution.values()

        assertTrue("[DEBUG_LOG] Should contain UHD_4K resolution", resolutions.contains(VideoResolution.UHD_4K))
        assertTrue("[DEBUG_LOG] Should contain FULL_HD resolution", resolutions.contains(VideoResolution.FULL_HD))
        assertTrue("[DEBUG_LOG] Should contain HD resolution", resolutions.contains(VideoResolution.HD))
        assertTrue("[DEBUG_LOG] Should contain STANDARD resolution", resolutions.contains(VideoResolution.STANDARD))
        assertTrue("[DEBUG_LOG] Should contain LOW resolution", resolutions.contains(VideoResolution.LOW))

        assertEquals("[DEBUG_LOG] 4K resolution value should be correct", "3840x2160", VideoResolution.UHD_4K.value)
        assertEquals(
            "[DEBUG_LOG] Full HD resolution value should be correct",
            "1920x1080",
            VideoResolution.FULL_HD.value
        )
        assertEquals("[DEBUG_LOG] HD resolution value should be correct", "1280x720", VideoResolution.HD.value)
    }

    @Test
    fun `video quality enum contains all expected qualities`() {
        val qualities = VideoQuality.values()

        assertTrue("[DEBUG_LOG] Should contain ULTRA_HIGH quality", qualities.contains(VideoQuality.ULTRA_HIGH))
        assertTrue("[DEBUG_LOG] Should contain HIGH quality", qualities.contains(VideoQuality.HIGH))
        assertTrue("[DEBUG_LOG] Should contain MEDIUM quality", qualities.contains(VideoQuality.MEDIUM))
        assertTrue("[DEBUG_LOG] Should contain LOW quality", qualities.contains(VideoQuality.LOW))
        assertTrue("[DEBUG_LOG] Should contain VERY_LOW quality", qualities.contains(VideoQuality.VERY_LOW))

        assertEquals(
            "[DEBUG_LOG] Ultra high quality value should be correct",
            "ultra_high",
            VideoQuality.ULTRA_HIGH.value
        )
        assertEquals("[DEBUG_LOG] High quality value should be correct", "high", VideoQuality.HIGH.value)
        assertEquals("[DEBUG_LOG] Medium quality value should be correct", "medium", VideoQuality.MEDIUM.value)
    }

    @Test
    fun `storage location enum contains all expected locations`() {
        val locations = StorageLocation.values()

        assertTrue("[DEBUG_LOG] Should contain INTERNAL location", locations.contains(StorageLocation.INTERNAL))
        assertTrue("[DEBUG_LOG] Should contain EXTERNAL location", locations.contains(StorageLocation.EXTERNAL))
        assertTrue("[DEBUG_LOG] Should contain PRIVATE location", locations.contains(StorageLocation.PRIVATE))
        assertTrue("[DEBUG_LOG] Should contain DOWNLOADS location", locations.contains(StorageLocation.DOWNLOADS))

        assertEquals("[DEBUG_LOG] Internal storage value should be correct", "internal", StorageLocation.INTERNAL.value)
        assertEquals("[DEBUG_LOG] External storage value should be correct", "external", StorageLocation.EXTERNAL.value)
        assertEquals("[DEBUG_LOG] Private storage value should be correct", "private", StorageLocation.PRIVATE.value)
        assertEquals(
            "[DEBUG_LOG] Downloads storage value should be correct",
            "downloads",
            StorageLocation.DOWNLOADS.value
        )
    }

    @Test
    fun `IP address validation works correctly`() {
        val validState = SettingsUiState(
            serverIpAddress = "192.168.1.1",
            legacyPort = 8080,
            jsonPort = 9000
        )
        assertTrue("[DEBUG_LOG] Valid IP should pass validation", validState.isNetworkConfigValid)

        val edgeCaseState = SettingsUiState(
            serverIpAddress = "0.0.0.0",
            legacyPort = 8080,
            jsonPort = 9000
        )
        assertTrue("[DEBUG_LOG] Edge case valid IP should pass validation", edgeCaseState.isNetworkConfigValid)

        val invalidStates = listOf(
            SettingsUiState(serverIpAddress = "256.1.1.1", legacyPort = 8080, jsonPort = 9000),
            SettingsUiState(serverIpAddress = "192.168.1", legacyPort = 8080, jsonPort = 9000),
            SettingsUiState(serverIpAddress = "192.168.1.1.1", legacyPort = 8080, jsonPort = 9000),
            SettingsUiState(serverIpAddress = "not.an.ip.address", legacyPort = 8080, jsonPort = 9000)
        )

        invalidStates.forEach { state ->
            assertFalse(
                "[DEBUG_LOG] Invalid IP ${state.serverIpAddress} should fail validation",
                state.isNetworkConfigValid
            )
        }
    }
}
