package com.multisensor.recording.testfixtures

import com.multisensor.recording.network.NetworkQualityMonitor.NetworkQuality

object NetworkTestFactory {

    fun createNetworkQuality(
        score: Int = 4,
        latencyMs: Long = 50L,
        bandwidthKbps: Double = 1000.0,
        timestamp: Long = System.currentTimeMillis()
    ): NetworkQuality {
        return NetworkQuality(
            score = score,
            latencyMs = latencyMs,
            bandwidthKbps = bandwidthKbps,
            timestamp = timestamp
        )
    }

    fun createPoorNetworkQuality(): NetworkQuality {
        return createNetworkQuality(
            score = 1,
            latencyMs = 500L,
            bandwidthKbps = 100.0
        )
    }

    fun createExcellentNetworkQuality(): NetworkQuality {
        return createNetworkQuality(
            score = 5,
            latencyMs = 5L,
            bandwidthKbps = 2000.0
        )
    }
}

object RecordingTestFactory {

    fun createMockRecordingStatistics(
        sessionId: String = "test-session-${System.currentTimeMillis()}",
        duration: Long = 30000L,
        videoEnabled: Boolean = true,
        audioEnabled: Boolean = false,
        thermalEnabled: Boolean = true,
        framesRecorded: Int = 1800,
        dataSize: Long = 1024 * 1024 * 100,
        averageFrameRate: Double = 60.0,
        droppedFrames: Int = 0
    ): Map<String, Any> {
        return mapOf(
            "sessionId" to sessionId,
            "duration" to duration,
            "videoEnabled" to videoEnabled,
            "audioEnabled" to audioEnabled,
            "thermalEnabled" to thermalEnabled,
            "framesRecorded" to framesRecorded,
            "dataSize" to dataSize,
            "averageFrameRate" to averageFrameRate,
            "droppedFrames" to droppedFrames,
            "timestamp" to System.currentTimeMillis()
        )
    }

    fun createLongMockRecordingStatistics(): Map<String, Any> {
        return createMockRecordingStatistics(
            duration = 300000L,
            framesRecorded = 18000,
            dataSize = 1024 * 1024 * 1024,
            averageFrameRate = 60.0
        )
    }

    fun createShortMockRecordingStatistics(): Map<String, Any> {
        return createMockRecordingStatistics(
            duration = 5000L,
            framesRecorded = 300,
            dataSize = 1024 * 1024 * 10,
            averageFrameRate = 60.0
        )
    }

    fun createProblematicMockRecordingStatistics(): Map<String, Any> {
        return createMockRecordingStatistics(
            duration = 30000L,
            framesRecorded = 1500,
            averageFrameRate = 50.0,
            droppedFrames = 300
        )
    }
}
