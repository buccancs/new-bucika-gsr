package com.multisensor.recording.firebase

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.multisensor.recording.firebase.FirebaseAuthService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.*
import java.util.Date

/**
 * Unit tests for FirebaseFirestoreService
 */
@DisplayName("Firebase Firestore Service Tests")
class FirebaseFirestoreServiceTest {

    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockAuthService: FirebaseAuthService
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocument: DocumentReference
    private lateinit var mockDocumentSnapshot: DocumentSnapshot
    private lateinit var mockQuery: Query
    private lateinit var mockQuerySnapshot: QuerySnapshot
    private lateinit var firebaseFirestoreService: FirebaseFirestoreService

    @BeforeEach
    fun setup() {
        mockFirestore = mockk(relaxed = true)
        mockAuthService = mockk(relaxed = true)
        mockCollection = mockk(relaxed = true)
        mockDocument = mockk(relaxed = true)
        mockDocumentSnapshot = mockk(relaxed = true)
        mockQuery = mockk(relaxed = true)
        mockQuerySnapshot = mockk(relaxed = true)
        firebaseFirestoreService = FirebaseFirestoreService(mockFirestore, mockAuthService)

        every { mockFirestore.collection(any()) } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocument
        every { mockCollection.document() } returns mockDocument
        every { mockDocument.id } returns "test-doc-id"
    }

    @Test
    @DisplayName("Should save recording session successfully")
    fun testSaveRecordingSessionSuccess() = runBlocking {
        // Given
        val session = FirebaseFirestoreService.RecordingSession(
            sessionId = "",
            startTime = Date(),
            deviceCount = 2,
            researcherId = "researcher-001"
        )
        val mockSetTask: Task<Void> = Tasks.forResult(null)
        every { mockDocument.set(any()) } returns mockSetTask

        // When
        val result = firebaseFirestoreService.saveRecordingSession(session)

        // Then
        assertTrue(result.isSuccess)
        verify { mockDocument.set(any()) }
    }

    @Test
    @DisplayName("Should handle save recording session failure")
    fun testSaveRecordingSessionFailure() = runBlocking {
        // Given
        val session = FirebaseFirestoreService.RecordingSession(
            sessionId = "",
            startTime = Date(),
            deviceCount = 2,
            researcherId = "researcher-001"
        )
        val exception = RuntimeException("Firestore error")
        val mockSetTask: Task<Void> = Tasks.forException(exception)
        every { mockDocument.set(any()) } returns mockSetTask

        // When
        val result = firebaseFirestoreService.saveRecordingSession(session)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    @DisplayName("Should update recording session end successfully")
    fun testUpdateRecordingSessionEndSuccess() = runBlocking {
        // Given
        val sessionId = "test-session-123"
        val endTime = Date()
        val dataFilePaths = mapOf("rgb_video" to "path/to/video.mp4")
        val totalDataSizeBytes = 1024L
        val mockUpdateTask: Task<Void> = Tasks.forResult(null)
        every { mockDocument.update(any<Map<String, Any>>()) } returns mockUpdateTask

        // When
        val result = firebaseFirestoreService.updateRecordingSessionEnd(
            sessionId, endTime, dataFilePaths, totalDataSizeBytes
        )

        // Then
        assertTrue(result.isSuccess)
        verify { mockDocument.update(any<Map<String, Any>>()) }
    }

    @Test
    @DisplayName("Should get recording session successfully")
    fun testGetRecordingSessionSuccess() = runBlocking {
        // Given
        val sessionId = "test-session-123"
        val mockGetTask: Task<DocumentSnapshot> = Tasks.forResult(mockDocumentSnapshot)
        every { mockDocument.get() } returns mockGetTask
        every { mockDocumentSnapshot.toObject(FirebaseFirestoreService.RecordingSession::class.java) } returns 
            FirebaseFirestoreService.RecordingSession(sessionId = sessionId)

        // When
        val result = firebaseFirestoreService.getRecordingSession(sessionId)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals(sessionId, result.getOrNull()?.sessionId)
    }

    @Test
    @DisplayName("Should save calibration data successfully")
    fun testSaveCalibrationDataSuccess() = runBlocking {
        // Given
        val sessionId = "test-session-123"
        val calibrationType = "camera_calibration"
        val calibrationData = mapOf("matrix" to "test-matrix", "distortion" to "test-distortion")
        val mockSetTask: Task<Void> = Tasks.forResult(null)
        every { mockDocument.set(any()) } returns mockSetTask

        // When
        val result = firebaseFirestoreService.saveCalibrationData(sessionId, calibrationType, calibrationData)

        // Then
        assertTrue(result.isSuccess)
        verify { mockDocument.set(any()) }
    }

    @Test
    @DisplayName("Should log system error successfully")
    fun testLogSystemErrorSuccess() = runBlocking {
        // Given
        val sessionId = "test-session-123"
        val errorType = "camera_initialization_error"
        val errorMessage = "Failed to initialize camera"
        val stackTrace = "com.example.CameraError at line 123"
        val mockSetTask: Task<Void> = Tasks.forResult(null)
        every { mockDocument.set(any()) } returns mockSetTask

        // When
        val result = firebaseFirestoreService.logSystemError(sessionId, errorType, errorMessage, stackTrace)

        // Then
        assertTrue(result.isSuccess)
        verify { mockDocument.set(any()) }
    }
}