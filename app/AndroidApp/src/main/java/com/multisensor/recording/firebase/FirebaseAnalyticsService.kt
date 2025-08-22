package com.multisensor.recording.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Analytics service for tracking app usage and research events
 * Enhanced for comprehensive research workflow analytics
 */
@Singleton
class FirebaseAnalyticsService @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {

    // Research Session Events
    
    /**
     * Log recording session start
     */
    fun logRecordingSessionStart(sessionId: String, deviceCount: Int, experimentType: String? = null) {
        val bundle = Bundle().apply {
            putString("session_id", sessionId)
            putLong("device_count", deviceCount.toLong())
            if (experimentType != null) {
                putString("experiment_type", experimentType)
            }
        }
        firebaseAnalytics.logEvent("recording_session_start", bundle)
    }

    /**
     * Log recording session end
     */
    fun logRecordingSessionEnd(sessionId: String, durationMs: Long, dataSize: Long, participantCount: Int = 1) {
        val bundle = Bundle().apply {
            putString("session_id", sessionId)
            putLong("duration_ms", durationMs)
            putLong("data_size_bytes", dataSize)
            putLong("participant_count", participantCount.toLong())
        }
        firebaseAnalytics.logEvent("recording_session_end", bundle)
    }

    /**
     * Log session pause/resume
     */
    fun logSessionPause(sessionId: String, reason: String) {
        val bundle = Bundle().apply {
            putString("session_id", sessionId)
            putString("pause_reason", reason)
        }
        firebaseAnalytics.logEvent("session_paused", bundle)
    }

    fun logSessionResume(sessionId: String, pauseDurationMs: Long) {
        val bundle = Bundle().apply {
            putString("session_id", sessionId)
            putLong("pause_duration_ms", pauseDurationMs)
        }
        firebaseAnalytics.logEvent("session_resumed", bundle)
    }

    // Device and Sensor Events

    /**
     * Log GSR sensor connection
     */
    fun logGSRSensorConnected(sensorId: String, sensorType: String = "shimmer", connectionMethod: String = "bluetooth") {
        val bundle = Bundle().apply {
            putString("sensor_id", sensorId)
            putString("sensor_type", sensorType)
            putString("connection_method", connectionMethod)
        }
        firebaseAnalytics.logEvent("gsr_sensor_connected", bundle)
    }

    /**
     * Log GSR sensor disconnection
     */
    fun logGSRSensorDisconnected(sensorId: String, reason: String, dataLoss: Boolean = false) {
        val bundle = Bundle().apply {
            putString("sensor_id", sensorId)
            putString("disconnect_reason", reason)
            putLong("data_loss", if (dataLoss) 1L else 0L)
        }
        firebaseAnalytics.logEvent("gsr_sensor_disconnected", bundle)
    }

    /**
     * Log thermal camera usage
     */
    fun logThermalCameraUsed(cameraModel: String, resolution: String, frameRate: Int? = null) {
        val bundle = Bundle().apply {
            putString("camera_model", cameraModel)
            putString("resolution", resolution)
            if (frameRate != null) {
                putLong("frame_rate", frameRate.toLong())
            }
        }
        firebaseAnalytics.logEvent("thermal_camera_used", bundle)
    }

    /**
     * Log camera calibration
     */
    fun logCameraCalibration(cameraType: String, success: Boolean, errorCount: Int = 0) {
        val bundle = Bundle().apply {
            putString("camera_type", cameraType)
            putLong("success", if (success) 1L else 0L)
            putLong("error_count", errorCount.toLong())
        }
        firebaseAnalytics.logEvent("camera_calibration", bundle)
    }

    // Data Quality and Processing Events

    /**
     * Log calibration event
     */
    fun logCalibrationPerformed(calibrationType: String, success: Boolean, duration: Long? = null) {
        val bundle = Bundle().apply {
            putString("calibration_type", calibrationType)
            putLong("success", if (success) 1L else 0L)
            if (duration != null) {
                putLong("duration_ms", duration)
            }
        }
        firebaseAnalytics.logEvent("calibration_performed", bundle)
    }

    /**
     * Log data quality assessment
     */
    fun logDataQualityCheck(sessionId: String, qualityScore: Float, issues: List<String> = emptyList()) {
        val bundle = Bundle().apply {
            putString("session_id", sessionId)
            putLong("quality_score", (qualityScore * 100).toLong()) // Store as percentage
            putLong("issue_count", issues.size.toLong())
            if (issues.isNotEmpty()) {
                putString("primary_issue", issues.first())
            }
        }
        firebaseAnalytics.logEvent("data_quality_check", bundle)
    }

    /**
     * Log synchronization events
     */
    fun logSynchronizationPerformed(deviceCount: Int, success: Boolean, timeDriftMs: Long? = null) {
        val bundle = Bundle().apply {
            putLong("device_count", deviceCount.toLong())
            putLong("success", if (success) 1L else 0L)
            if (timeDriftMs != null) {
                putLong("time_drift_ms", timeDriftMs)
            }
        }
        firebaseAnalytics.logEvent("synchronization_performed", bundle)
    }

    // Data Export and Analysis Events

    /**
     * Log data export
     */
    fun logDataExport(format: String, fileSizeBytes: Long, sessionCount: Int = 1, exportType: String = "manual") {
        val bundle = Bundle().apply {
            putString("export_format", format)
            putLong("file_size_bytes", fileSizeBytes)
            putLong("session_count", sessionCount.toLong())
            putString("export_type", exportType)
        }
        firebaseAnalytics.logEvent("data_export", bundle)
    }

    /**
     * Log analysis performed
     */
    fun logAnalysisPerformed(analysisType: String, sessionId: String, processingTime: Long) {
        val bundle = Bundle().apply {
            putString("analysis_type", analysisType)
            putString("session_id", sessionId)
            putLong("processing_time_ms", processingTime)
        }
        firebaseAnalytics.logEvent("analysis_performed", bundle)
    }

    // User and Research Context Events

    /**
     * Log user authentication
     */
    fun logUserAuthentication(method: String, researcherType: String) {
        val bundle = Bundle().apply {
            putString("auth_method", method)
            putString("researcher_type", researcherType)
        }
        firebaseAnalytics.logEvent("user_authentication", bundle)
    }

    /**
     * Log research project creation
     */
    fun logResearchProjectCreated(projectType: String, collaboratorCount: Int) {
        val bundle = Bundle().apply {
            putString("project_type", projectType)
            putLong("collaborator_count", collaboratorCount.toLong())
        }
        firebaseAnalytics.logEvent("research_project_created", bundle)
    }

    /**
     * Log participant consent
     */
    fun logParticipantConsent(consentType: String, granted: Boolean) {
        val bundle = Bundle().apply {
            putString("consent_type", consentType)
            putLong("granted", if (granted) 1L else 0L)
        }
        firebaseAnalytics.logEvent("participant_consent", bundle)
    }

    // Error and Performance Events

    /**
     * Log system errors
     */
    fun logSystemError(errorType: String, errorMessage: String, severity: String = "medium") {
        val bundle = Bundle().apply {
            putString("error_type", errorType)
            putString("error_message", errorMessage.take(100)) // Limit message length
            putString("severity", severity)
        }
        firebaseAnalytics.logEvent("system_error", bundle)
    }

    /**
     * Log performance metrics
     */
    fun logPerformanceMetric(metricName: String, value: Long, unit: String) {
        val bundle = Bundle().apply {
            putString("metric_name", metricName)
            putLong("metric_value", value)
            putString("metric_unit", unit)
        }
        firebaseAnalytics.logEvent("performance_metric", bundle)
    }

    /**
     * Log battery usage for long sessions
     */
    fun logBatteryUsage(sessionId: String, batteryLevel: Int, duration: Long) {
        val bundle = Bundle().apply {
            putString("session_id", sessionId)
            putLong("battery_level", batteryLevel.toLong())
            putLong("session_duration_ms", duration)
        }
        firebaseAnalytics.logEvent("battery_usage", bundle)
    }

    // Cloud Storage Events

    /**
     * Log cloud upload
     */
    fun logCloudUpload(fileType: String, fileSizeBytes: Long, success: Boolean, uploadTime: Long? = null) {
        val bundle = Bundle().apply {
            putString("file_type", fileType)
            putLong("file_size_bytes", fileSizeBytes)
            putLong("success", if (success) 1L else 0L)
            if (uploadTime != null) {
                putLong("upload_time_ms", uploadTime)
            }
        }
        firebaseAnalytics.logEvent("cloud_upload", bundle)
    }

    /**
     * Log cloud download
     */
    fun logCloudDownload(fileType: String, fileSizeBytes: Long, success: Boolean) {
        val bundle = Bundle().apply {
            putString("file_type", fileType)
            putLong("file_size_bytes", fileSizeBytes)
            putLong("success", if (success) 1L else 0L)
        }
        firebaseAnalytics.logEvent("cloud_download", bundle)
    }

    // Research Workflow Events

    /**
     * Log experiment workflow step
     */
    fun logWorkflowStep(step: String, sessionId: String, duration: Long? = null) {
        val bundle = Bundle().apply {
            putString("step_name", step)
            putString("session_id", sessionId)
            if (duration != null) {
                putLong("step_duration_ms", duration)
            }
        }
        firebaseAnalytics.logEvent("workflow_step", bundle)
    }

    // User Properties for Research Context

    /**
     * Set user properties for research context
     */
    fun setUserProperty(property: String, value: String) {
        firebaseAnalytics.setUserProperty(property, value)
    }

    /**
     * Set researcher type
     */
    fun setResearcherType(researcherType: String) {
        setUserProperty("researcher_type", researcherType)
    }

    /**
     * Set institution
     */
    fun setInstitution(institution: String) {
        setUserProperty("institution", institution)
    }

    /**
     * Set research area
     */
    fun setResearchArea(area: String) {
        setUserProperty("research_area", area)
    }

    /**
     * Set app version for analytics segmentation
     */
    fun setAppVersion(version: String) {
        setUserProperty("app_version", version)
    }
}