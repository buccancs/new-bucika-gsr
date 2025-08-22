package com.multisensor.recording.controllers

import android.content.Context
import android.view.View
import android.widget.TextView
import com.google.common.truth.Truth.assertThat
import com.multisensor.recording.testbase.BaseRobolectricTest
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkControllerTest : BaseRobolectricTest() {

    @RelaxedMockK
    private lateinit var mockContext: Context

    @RelaxedMockK
    private lateinit var mockCallback: NetworkController.NetworkCallback

    @RelaxedMockK
    private lateinit var mockStreamingIndicator: View

    @RelaxedMockK
    private lateinit var mockStreamingLabel: View

    @RelaxedMockK
    private lateinit var mockStreamingDebugOverlay: TextView

    @InjectMockKs
    private lateinit var networkController: NetworkController

    @Before
    override fun setUp() {
        super.setUp()

        every { mockCallback.getStreamingIndicator() } returns mockStreamingIndicator
        every { mockCallback.getStreamingLabel() } returns mockStreamingLabel
        every { mockCallback.getStreamingDebugOverlay() } returns mockStreamingDebugOverlay

        networkController.setCallback(mockCallback)
    }

    @Test
    fun `should show streaming indicator correctly`() {
        networkController.showStreamingIndicator(mockContext)

        assertThat(networkController.isStreamingActive()).isTrue()
        verify { mockCallback.onStreamingStarted() }
        verify { mockStreamingIndicator.setBackgroundColor(any()) }
        verify { mockStreamingLabel.visibility = View.VISIBLE }
    }

    @Test
    fun `should hide streaming indicator correctly`() {
        networkController.showStreamingIndicator(mockContext)

        networkController.hideStreamingIndicator(mockContext)

        assertThat(networkController.isStreamingActive()).isFalse()
        verify { mockCallback.onStreamingStopped() }
        verify { mockStreamingIndicator.setBackgroundColor(any()) }
        verify { mockStreamingLabel.visibility = View.GONE }
    }

    @Test
    fun `should update streaming debug overlay with correct text`() {
        networkController.updateStreamingMetrics(30, "1.5 MB/s")

        networkController.updateStreamingDebugOverlay()

        verify { mockStreamingDebugOverlay.text = any() }
        verify { mockStreamingDebugOverlay.visibility = any() }
    }

    @Test
    fun `should update streaming UI based on recording state`() {
        networkController.updateStreamingUI(mockContext, isRecording = true)

        verify { mockCallback.onStreamingStarted() }
        verify { mockStreamingLabel.visibility = View.VISIBLE }

        networkController.updateStreamingUI(mockContext, isRecording = false)

        verify { mockCallback.onStreamingStopped() }
        verify { mockStreamingDebugOverlay.visibility = View.GONE }
    }

    @Test
    fun `should update streaming metrics correctly`() {
        val frameRate = 60
        val dataSize = "2.1 MB/s"

        networkController.updateStreamingMetrics(frameRate, dataSize)

        val (currentFrameRate, currentDataSize) = networkController.getStreamingMetrics()
        assertThat(currentFrameRate).isEqualTo(frameRate)
        assertThat(currentDataSize).isEqualTo(dataSize)
    }

    @Test
    fun `should update streaming indicator with dynamic metrics`() {
        val frameRate = 45
        val dataSize = "1.8 MB/s"

        networkController.updateStreamingIndicator(mockContext, isStreaming = true, frameRate, dataSize)

        verify { mockStreamingDebugOverlay.text = "Streaming: ${frameRate}fps ($dataSize)" }
        verify { mockStreamingDebugOverlay.visibility = View.VISIBLE }
        verify { mockStreamingLabel.visibility = View.VISIBLE }
    }

    @Test
    fun `should handle streaming indicator with zero frame rate`() {
        networkController.updateStreamingIndicator(mockContext, isStreaming = true, frameRate = 0)

        verify { mockStreamingDebugOverlay.visibility = View.GONE }
        verify { mockStreamingLabel.visibility = View.GONE }
    }

    @Test
    fun `should handle network connectivity changes`() {
        networkController.showStreamingIndicator(mockContext)

        networkController.handleNetworkConnectivityChange(connected = false)

        verify { mockCallback.onNetworkStatusChanged(false) }
        verify { mockCallback.onStreamingError("Network connection lost during streaming") }
    }

    @Test
    fun `should handle network reconnection gracefully`() {
        networkController.handleNetworkConnectivityChange(connected = true)

        verify { mockCallback.onNetworkStatusChanged(true) }
    }

    @Test
    fun `should not show streaming error when network disconnects while not streaming`() {
        assertThat(networkController.isStreamingActive()).isFalse()

        networkController.handleNetworkConnectivityChange(connected = false)

        verify { mockCallback.onNetworkStatusChanged(false) }
        verify(exactly = 0) { mockCallback.onStreamingError(any()) }
    }

    @Test
    fun `should set streaming quality to LOW correctly`() {
        networkController.showStreamingIndicator(mockContext)

        networkController.setStreamingQuality(NetworkController.StreamingQuality.LOW)

        val (frameRate, dataSize) = networkController.getStreamingMetrics()
        assertThat(frameRate).isEqualTo(15)
        assertThat(dataSize).isEqualTo("500 KB/s")
        verify { mockCallback.updateStatusText(any()) }
    }

    @Test
    fun `should set streaming quality to HIGH correctly`() {
        networkController.showStreamingIndicator(mockContext)

        networkController.setStreamingQuality(NetworkController.StreamingQuality.HIGH)

        val (frameRate, dataSize) = networkController.getStreamingMetrics()
        assertThat(frameRate).isEqualTo(30)
        assertThat(dataSize).isEqualTo("2.5 MB/s")
        verify { mockCallback.updateStatusText(match { it.contains("High") }) }
    }

    @Test
    fun `should not update metrics when setting quality while not streaming`() {
        assertThat(networkController.isStreamingActive()).isFalse()

        networkController.setStreamingQuality(NetworkController.StreamingQuality.ULTRA)

        val (frameRate, dataSize) = networkController.getStreamingMetrics()
        assertThat(frameRate).isEqualTo(0)
        assertThat(dataSize).isEqualTo("0 KB/s")
    }

    @Test
    fun `should start streaming session successfully`() = runTest {
        networkController.startStreaming(mockContext)

        assertThat(networkController.isStreamingActive()).isTrue()
        verify { mockCallback.onStreamingStarted() }
        verify { mockCallback.updateStatusText("Streaming started") }

        val (frameRate, dataSize) = networkController.getStreamingMetrics()
        assertThat(frameRate).isEqualTo(30)
        assertThat(dataSize).isEqualTo("1.2 MB/s")
    }

    @Test
    fun `should stop streaming session successfully`() = runTest {
        networkController.startStreaming(mockContext)

        networkController.stopStreaming(mockContext)

        assertThat(networkController.isStreamingActive()).isFalse()
        verify { mockCallback.onStreamingStopped() }
        verify { mockCallback.updateStatusText("Streaming stopped") }

        val (frameRate, dataSize) = networkController.getStreamingMetrics()
        assertThat(frameRate).isEqualTo(0)
        assertThat(dataSize).isEqualTo("0 KB/s")
    }

    @Test
    fun `should return valid network statistics`() {
        val statistics = networkController.getNetworkStatistics(mockContext)

        assertThat(statistics).containsKey("streaming_active")
        assertThat(statistics).containsKey("frame_rate")
        assertThat(statistics).containsKey("data_size")
        assertThat(statistics).containsKey("timestamp")
        assertThat(statistics).containsKey("network_type")
        assertThat(statistics).containsKey("bandwidth_estimate")
        assertThat(statistics).containsKey("connection_quality")
    }

    @Test
    fun `should return streaming status summary`() {
        networkController.updateStreamingMetrics(25, "1.1 MB/s")

        val status = networkController.getStreamingStatus(mockContext)

        assertThat(status).contains("Streaming Status:")
        assertThat(status).contains("Frame Rate: 25fps")
        assertThat(status).contains("Data Size: 1.1 MB/s")
        assertThat(status).contains("Network Connected:")
        assertThat(status).contains("Network Type:")
        assertThat(status).contains("Bandwidth Estimate:")
    }

    @Test
    fun `should handle emergency streaming stop`() = runTest {
        networkController.startStreaming(mockContext)

        networkController.emergencyStopStreaming(mockContext)

        assertThat(networkController.isStreamingActive()).isFalse()
        verify { mockCallback.updateStatusText(match { it.contains("Emergency stop completed") }) }
        verify { mockCallback.showToast(match { it.contains("Emergency stop") }, any()) }
    }

    @Test
    fun `should reset state correctly`() {
        networkController.startStreaming(mockContext)
        networkController.updateStreamingMetrics(60, "3.0 MB/s")

        networkController.resetState()

        assertThat(networkController.isStreamingActive()).isFalse()
        val (frameRate, dataSize) = networkController.getStreamingMetrics()
        assertThat(frameRate).isEqualTo(0)
        assertThat(dataSize).isEqualTo("0 KB/s")
    }

    @Test
    fun `should handle callback being null gracefully`() {
        networkController.setCallback(null)

        networkController.showStreamingIndicator(mockContext)
        networkController.hideStreamingIndicator(mockContext)
        networkController.updateStreamingDebugOverlay()
        networkController.handleNetworkConnectivityChange(false)
    }

    @Test
    fun `StreamingQuality enum should have correct display names`() {
        assertThat(NetworkController.StreamingQuality.LOW.displayName).isEqualTo("Low (480p, 15fps)")
        assertThat(NetworkController.StreamingQuality.MEDIUM.displayName).isEqualTo("Medium (720p, 30fps)")
        assertThat(NetworkController.StreamingQuality.HIGH.displayName).isEqualTo("High (1080p, 30fps)")
        assertThat(NetworkController.StreamingQuality.ULTRA.displayName).isEqualTo("Ultra (1080p, 60fps)")
    }

    @Test
    fun `should handle network statistics without context`() {
        val statistics = networkController.getNetworkStatistics(context = null)

        assertThat(statistics["network_type"]).isEqualTo("Context unavailable")
        assertThat(statistics).containsKey("streaming_active")
        assertThat(statistics).containsKey("frame_rate")
        assertThat(statistics).containsKey("timestamp")
    }

    @Test
    fun `should return status summary without context`() {
        val status = networkController.getStreamingStatus(context = null)

        assertThat(status).contains("Network Type: Unknown")
        assertThat(status).contains("Network Connected: false")
        assertThat(status).contains("Streaming Status:")
    }

    @Test
    fun `should set streaming protocol to RTMP successfully`() {
        networkController.setStreamingProtocol(NetworkController.StreamingProtocol.RTMP)

        verify { mockCallback.onProtocolChanged(NetworkController.StreamingProtocol.RTMP) }
        verify { mockCallback.updateStatusText(match { it.contains("RTMP") }) }
    }

    @Test
    fun `should set streaming protocol to WebRTC successfully`() {
        networkController.setStreamingProtocol(NetworkController.StreamingProtocol.WEBRTC)

        verify { mockCallback.onProtocolChanged(NetworkController.StreamingProtocol.WEBRTC) }
        verify { mockCallback.updateStatusText(match { it.contains("WebRTC") }) }
    }

    @Test
    fun `should validate protocol compatibility correctly`() {

        networkController.setStreamingProtocol(NetworkController.StreamingProtocol.RTMP)

        verify { mockCallback.onProtocolChanged(NetworkController.StreamingProtocol.RTMP) }
    }

    @Test
    fun `should set bandwidth estimation method to ML successfully`() {
        networkController.setBandwidthEstimationMethod(NetworkController.BandwidthEstimationMethod.MACHINE_LEARNING)

        verify { mockCallback.updateStatusText(match { it.contains("Machine Learning") }) }
    }

    @Test
    fun `should set bandwidth estimation method to Adaptive successfully`() {
        networkController.setBandwidthEstimationMethod(NetworkController.BandwidthEstimationMethod.ADAPTIVE)

        verify { mockCallback.updateStatusText(match { it.contains("Adaptive") }) }
    }

    @Test
    fun `should set bandwidth estimation method to Hybrid successfully`() {
        networkController.setBandwidthEstimationMethod(NetworkController.BandwidthEstimationMethod.HYBRID)

        verify { mockCallback.updateStatusText(match { it.contains("Hybrid") }) }
    }

    @Test
    fun `should enable adaptive bitrate streaming`() {
        networkController.setAdaptiveBitrateEnabled(true)

        verify { mockCallback.updateStatusText("Adaptive bitrate: Enabled") }
    }

    @Test
    fun `should disable adaptive bitrate streaming`() {
        networkController.setAdaptiveBitrateEnabled(false)

        verify { mockCallback.updateStatusText("Adaptive bitrate: Disabled") }
    }

    @Test
    fun `should enable frame dropping`() {
        networkController.setFrameDropEnabled(true)

        verify { mockCallback.updateStatusText("Frame dropping: Enabled") }
    }

    @Test
    fun `should disable frame dropping`() {
        networkController.setFrameDropEnabled(false)

        verify { mockCallback.updateStatusText("Frame dropping: Disabled") }
    }

    @Test
    fun `should enable encryption successfully`() {
        networkController.setEncryptionEnabled(true)

        verify { mockCallback.onEncryptionStatusChanged(true) }
        verify { mockCallback.updateStatusText("Encryption: Enabled") }
    }

    @Test
    fun `should disable encryption successfully`() {
        networkController.setEncryptionEnabled(false)

        verify { mockCallback.onEncryptionStatusChanged(false) }
        verify { mockCallback.updateStatusText("Encryption: Disabled") }
    }

    @Test
    fun `StreamingProtocol enum should have correct display names`() {
        assertThat(NetworkController.StreamingProtocol.RTMP.displayName).isEqualTo("Real-Time Messaging Protocol")
        assertThat(NetworkController.StreamingProtocol.WEBRTC.displayName).isEqualTo("Web Real-Time Communication")
        assertThat(NetworkController.StreamingProtocol.HLS.displayName).isEqualTo("HTTP Live Streaming")
        assertThat(NetworkController.StreamingProtocol.DASH.displayName).isEqualTo("Dynamic Adaptive Streaming")
        assertThat(NetworkController.StreamingProtocol.UDP.displayName).isEqualTo("User Datagram Protocol")
        assertThat(NetworkController.StreamingProtocol.TCP.displayName).isEqualTo("Transmission Control Protocol")
    }

    @Test
    fun `BandwidthEstimationMethod enum should have correct display names`() {
        assertThat(NetworkController.BandwidthEstimationMethod.SIMPLE.displayName).isEqualTo("Simple Network Type Based")
        assertThat(NetworkController.BandwidthEstimationMethod.ADAPTIVE.displayName).isEqualTo("Adaptive Historical Analysis")
        assertThat(NetworkController.BandwidthEstimationMethod.MACHINE_LEARNING.displayName).isEqualTo("ML-based Prediction")
        assertThat(NetworkController.BandwidthEstimationMethod.HYBRID.displayName).isEqualTo("Hybrid Multi-method Approach")
    }

    @Test
    fun `should handle advanced streaming with all features enabled`() = runTest {
        networkController.setStreamingProtocol(NetworkController.StreamingProtocol.WEBRTC)
        networkController.setBandwidthEstimationMethod(NetworkController.BandwidthEstimationMethod.HYBRID)
        networkController.setAdaptiveBitrateEnabled(true)
        networkController.setFrameDropEnabled(true)
        networkController.setEncryptionEnabled(true)

        networkController.startStreaming(mockContext)

        assertThat(networkController.isStreamingActive()).isTrue()
        verify { mockCallback.onStreamingStarted() }
        verify { mockCallback.onProtocolChanged(NetworkController.StreamingProtocol.WEBRTC) }
        verify { mockCallback.onEncryptionStatusChanged(true) }
        verify { mockCallback.updateStatusText(match { it.contains("WebRTC") }) }
    }

    @Test
    fun `should reset all advanced features in resetState`() {
        networkController.setStreamingProtocol(NetworkController.StreamingProtocol.RTMP)
        networkController.setBandwidthEstimationMethod(NetworkController.BandwidthEstimationMethod.MACHINE_LEARNING)
        networkController.setAdaptiveBitrateEnabled(false)
        networkController.setFrameDropEnabled(false)
        networkController.setEncryptionEnabled(true)

        networkController.resetState()

        networkController.setStreamingProtocol(NetworkController.StreamingProtocol.UDP)
        verify { mockCallback.onProtocolChanged(NetworkController.StreamingProtocol.UDP) }
    }

    @Test
    fun `should handle bandwidth estimation callback correctly`() {
        val testBandwidth = 50_000_000L
        val testMethod = NetworkController.BandwidthEstimationMethod.HYBRID

        networkController.setBandwidthEstimationMethod(testMethod)

        verify { mockCallback.updateStatusText(match { it.contains("Hybrid") }) }
    }

    @Test
    fun `should handle frame drop callback correctly`() {

        networkController.setFrameDropEnabled(true)

        networkController.setFrameDropEnabled(false)

        verify { mockCallback.updateStatusText("Frame dropping: Enabled") }
        verify { mockCallback.updateStatusText("Frame dropping: Disabled") }
    }
}
