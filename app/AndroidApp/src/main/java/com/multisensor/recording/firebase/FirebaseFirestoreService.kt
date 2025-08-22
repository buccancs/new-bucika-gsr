package com.multisensor.recording.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date

/**
 * Firebase Firestore service for storing research data and metadata
 * Enhanced with authentication and researcher profile management
 */
@Singleton
class FirebaseFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authService: FirebaseAuthService
) {

    /**
     * Data class for recording session metadata
     */
    data class RecordingSession(
        val sessionId: String = "",
        val startTime: Date = Date(),
        val endTime: Date? = null,
        val deviceCount: Int = 0,
        val gsrSensorIds: List<String> = emptyList(),
        val thermalCameraModel: String = "",
        val rgbCameraResolution: String = "",
        val thermalResolution: String = "",
        val calibrationData: Map<String, Any> = emptyMap(),
        val dataFilePaths: Map<String, String> = emptyMap(),
        val totalDataSizeBytes: Long = 0,
        val researcherId: String = "",
        val participantId: String = "",
        val experimentType: String = "",
        val notes: String = "",
        val collaborators: List<String> = emptyList(), // List of researcher IDs with access
        val projectId: String? = null,
        val consentApproved: Boolean = false,
        val dataQualityScore: Float? = null,
        val syncAccuracy: Long? = null // Time drift in milliseconds
    )

    /**
     * Data class for research project
     */
    data class ResearchProject(
        val projectId: String = "",
        val title: String = "",
        val description: String = "",
        val principalInvestigator: String = "",
        val collaborators: List<String> = emptyList(),
        val createdAt: Date = Date(),
        val status: String = "active", // active, completed, archived
        val experimentType: String = "",
        val sessionIds: List<String> = emptyList(),
        val institution: String = "",
        val ethicsApprovalNumber: String? = null
    )

    // Researcher Profile Management

    /**
     * Create or update researcher profile
     */
    suspend fun saveResearcherProfile(profile: ResearcherProfile): Result<Unit> {
        return try {
            firestore.collection("researcher_profiles").document(profile.uid)
                .set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get researcher profile
     */
    suspend fun getResearcherProfile(uid: String): Result<ResearcherProfile?> {
        return try {
            val doc = firestore.collection("researcher_profiles").document(uid).get().await()
            val profile = doc.toObject(ResearcherProfile::class.java)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update researcher last active time
     */
    suspend fun updateResearcherLastActive(uid: String): Result<Unit> {
        return try {
            firestore.collection("researcher_profiles").document(uid)
                .update("lastActiveAt", System.currentTimeMillis()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Recording Session Management

    /**
     * Save recording session metadata
     */
    suspend fun saveRecordingSession(session: RecordingSession): Result<String> {
        return try {
            val currentUserId = authService.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val docRef = firestore.collection("recording_sessions").document()
            val sessionWithId = session.copy(
                sessionId = docRef.id,
                researcherId = currentUserId
            )
            docRef.set(sessionWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update recording session with end time and final data
     */
    suspend fun updateRecordingSessionEnd(
        sessionId: String,
        endTime: Date,
        dataFilePaths: Map<String, String>,
        totalDataSizeBytes: Long,
        dataQualityScore: Float? = null
    ): Result<Unit> {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "endTime" to endTime,
                "dataFilePaths" to dataFilePaths,
                "totalDataSizeBytes" to totalDataSizeBytes
            )
            
            if (dataQualityScore != null) {
                updateData["dataQualityScore"] = dataQualityScore
            }
            
            firestore.collection("recording_sessions").document(sessionId)
                .update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recording session by ID
     */
    suspend fun getRecordingSession(sessionId: String): Result<RecordingSession?> {
        return try {
            val doc = firestore.collection("recording_sessions").document(sessionId).get().await()
            val session = doc.toObject(RecordingSession::class.java)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recording sessions for current researcher
     */
    suspend fun getRecordingSessionsForCurrentResearcher(): Result<List<RecordingSession>> {
        return try {
            val currentUserId = authService.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = firestore.collection("recording_sessions")
                .whereEqualTo("researcherId", currentUserId)
                .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            
            val sessions = snapshot.documents.mapNotNull { it.toObject(RecordingSession::class.java) }
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recording sessions accessible to current researcher (owned or collaborated)
     */
    suspend fun getAccessibleRecordingSessions(): Result<List<RecordingSession>> {
        return try {
            val currentUserId = authService.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Get sessions where user is owner or collaborator
            val ownedSessions = firestore.collection("recording_sessions")
                .whereEqualTo("researcherId", currentUserId)
                .get().await()
            
            val collaboratedSessions = firestore.collection("recording_sessions")
                .whereArrayContains("collaborators", currentUserId)
                .get().await()
            
            val allSessions = (ownedSessions.documents + collaboratedSessions.documents)
                .distinctBy { it.id }
                .mapNotNull { it.toObject(RecordingSession::class.java) }
                .sortedByDescending { it.startTime }
            
            Result.success(allSessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add collaborator to recording session
     */
    suspend fun addCollaboratorToSession(sessionId: String, collaboratorEmail: String): Result<Unit> {
        return try {
            // First find the collaborator's UID by email
            val userQuery = firestore.collection("researcher_profiles")
                .whereEqualTo("email", collaboratorEmail)
                .get().await()
            
            if (userQuery.documents.isEmpty()) {
                return Result.failure(Exception("Researcher with email $collaboratorEmail not found"))
            }
            
            val collaboratorUid = userQuery.documents.first().id
            
            // Add to collaborators array
            firestore.collection("recording_sessions").document(sessionId)
                .update("collaborators", com.google.firebase.firestore.FieldValue.arrayUnion(collaboratorUid))
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Research Project Management

    /**
     * Create research project
     */
    suspend fun createResearchProject(project: ResearchProject): Result<String> {
        return try {
            val currentUserId = authService.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val docRef = firestore.collection("research_projects").document()
            val projectWithId = project.copy(
                projectId = docRef.id,
                principalInvestigator = currentUserId
            )
            docRef.set(projectWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get research projects for current researcher
     */
    suspend fun getResearchProjectsForCurrentResearcher(): Result<List<ResearchProject>> {
        return try {
            val currentUserId = authService.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val snapshot = firestore.collection("research_projects")
                .whereEqualTo("principalInvestigator", currentUserId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            
            val projects = snapshot.documents.mapNotNull { it.toObject(ResearchProject::class.java) }
            Result.success(projects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add session to project
     */
    suspend fun addSessionToProject(projectId: String, sessionId: String): Result<Unit> {
        return try {
            firestore.collection("research_projects").document(projectId)
                .update("sessionIds", com.google.firebase.firestore.FieldValue.arrayUnion(sessionId))
                .await()
            
            // Also update the session with project ID
            firestore.collection("recording_sessions").document(sessionId)
                .update("projectId", projectId).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Calibration and Technical Data

    /**
     * Save calibration data
     */
    suspend fun saveCalibrationData(
        sessionId: String,
        calibrationType: String,
        calibrationData: Map<String, Any>
    ): Result<Unit> {
        return try {
            val currentUserId = authService.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))
            
            firestore.collection("calibration_data").document()
                .set(
                    mapOf(
                        "sessionId" to sessionId,
                        "researcherId" to currentUserId,
                        "calibrationType" to calibrationType,
                        "calibrationData" to calibrationData,
                        "timestamp" to Date()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Log system error for research debugging
     */
    suspend fun logSystemError(
        sessionId: String?,
        errorType: String,
        errorMessage: String,
        stackTrace: String
    ): Result<Unit> {
        return try {
            val currentUserId = authService.getCurrentUserId()
            
            firestore.collection("system_errors").document()
                .set(
                    mapOf(
                        "sessionId" to sessionId,
                        "researcherId" to currentUserId,
                        "errorType" to errorType,
                        "errorMessage" to errorMessage,
                        "stackTrace" to stackTrace,
                        "timestamp" to Date()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Data Export Management

    /**
     * Create data export request
     */
    suspend fun createDataExportRequest(
        sessionIds: List<String>,
        exportFormat: String,
        includeMetadata: Boolean = true
    ): Result<String> {
        return try {
            val currentUserId = authService.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val docRef = firestore.collection("data_export_requests").document()
            val exportRequest = mapOf(
                "requestId" to docRef.id,
                "requesterId" to currentUserId,
                "sessionIds" to sessionIds,
                "exportFormat" to exportFormat,
                "includeMetadata" to includeMetadata,
                "status" to "pending",
                "requestedAt" to Date()
            )
            
            docRef.set(exportRequest).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Analytics and Insights

    /**
     * Save research insight
     */
    suspend fun saveResearchInsight(
        insightType: String,
        data: Map<String, Any>,
        timeframe: String
    ): Result<Unit> {
        return try {
            firestore.collection("research_insights").document()
                .set(
                    mapOf(
                        "insightType" to insightType,
                        "data" to data,
                        "timeframe" to timeframe,
                        "generatedAt" to Date()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get usage statistics for current researcher
     */
    suspend fun getUsageStatistics(): Result<Map<String, Any>> {
        return try {
            val currentUserId = authService.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val sessions = firestore.collection("recording_sessions")
                .whereEqualTo("researcherId", currentUserId)
                .get().await()
            
            val totalSessions = sessions.size()
            val totalDataSize = sessions.documents.sumOf { 
                it.getLong("totalDataSizeBytes") ?: 0L 
            }
            val avgSessionDuration = sessions.documents.mapNotNull { doc ->
                val start = doc.getDate("startTime")
                val end = doc.getDate("endTime")
                if (start != null && end != null) {
                    end.time - start.time
                } else null
            }.average()
            
            val stats = mapOf(
                "totalSessions" to totalSessions,
                "totalDataSizeBytes" to totalDataSize,
                "averageSessionDurationMs" to avgSessionDuration.toLong(),
                "lastUpdated" to Date()
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}