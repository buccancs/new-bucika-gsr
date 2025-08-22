package com.multisensor.recording

/**
 * Test constants for consistent testing across the application.
 * Provides common values, mock data, and test configurations.
 */
object TestConstants {
    
    // Test timeouts
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val LONG_TIMEOUT_MS = 10000L
    const val SHORT_TIMEOUT_MS = 1000L
    
    // Test data
    const val TEST_SESSION_ID = "test_session_123"
    const val TEST_USER_ID = "test_user_456"
    const val TEST_EMAIL = "test@example.com"
    const val TEST_PASSWORD = "test_password"
    const val TEST_FILENAME = "test_file.mp4"
    const val TEST_FILE_SIZE = 1024L
    
    // Mock network responses
    const val MOCK_SERVER_URL = "http://localhost:8080"
    const val MOCK_WEBSOCKET_URL = "ws://localhost:8080/websocket"
    
    // Test device configurations
    const val TEST_CAMERA_WIDTH = 1920
    const val TEST_CAMERA_HEIGHT = 1080
    const val TEST_CAMERA_FPS = 30
    
    // Shimmer test data
    const val TEST_SHIMMER_MAC = "00:11:22:33:44:55"
    const val TEST_GSR_VALUE = 2.5
    const val TEST_SAMPLING_RATE = 51.2
    
    // File system test paths
    const val TEST_BASE_DIR = "/test/recordings"
    const val TEST_SESSION_DIR = "$TEST_BASE_DIR/sessions"
    const val TEST_LOGS_DIR = "$TEST_BASE_DIR/logs"
    
    // Error messages
    const val ERROR_NETWORK_UNAVAILABLE = "Network unavailable"
    const val ERROR_PERMISSION_DENIED = "Permission denied"
    const val ERROR_DEVICE_NOT_FOUND = "Device not found"
    
    // Test collections
    val TEST_PERMISSIONS = listOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    val TEST_FILE_EXTENSIONS = listOf(".mp4", ".dng", ".json", ".log")
    
    // Mock data generators
    fun generateMockSessionData(sessionId: String = TEST_SESSION_ID) = mapOf(
        "id" to sessionId,
        "timestamp" to System.currentTimeMillis(),
        "duration" to 30000L,
        "file_count" to 5
    )
    
    fun generateMockGsrData(count: Int = 10) = (1..count).map {
        mapOf(
            "timestamp" to System.currentTimeMillis() + it * 1000,
            "value" to TEST_GSR_VALUE + (it * 0.1),
            "quality" to if (it % 2 == 0) "good" else "poor"
        )
    }
}