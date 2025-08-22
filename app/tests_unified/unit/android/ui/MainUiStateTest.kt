package com.multisensor.recording.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MainUiStateTest : BehaviorSpec({

    given("MainUiState with various configurations") {

        `when`("system is ready and not recording") {
            val state = MainUiState(
                isInitialized = true,
                isRecording = false,
                isLoadingRecording = false,
                isPcConnected = true,
                isCameraConnected = true
            )

            then("should be able to start recording") {
                state.canStartRecording shouldBe true
            }
        }

        `when`("system is not initialized") {
            val state = MainUiState(
                isInitialized = false,
                isRecording = false,
                isLoadingRecording = false,
                isPcConnected = true
            )

            then("should not be able to start recording") {
                state.canStartRecording shouldBe false
            }
        }

        `when`("system is already recording") {
            val state = MainUiState(
                isInitialized = true,
                isRecording = true,
                isLoadingRecording = false,
                isPcConnected = true
            )

            then("should not be able to start recording again") {
                state.canStartRecording shouldBe false
            }
        }

        `when`("system is loading") {
            val state = MainUiState(
                isInitialized = true,
                isRecording = false,
                isLoadingRecording = true,
                isPcConnected = true
            )

            then("should not be able to start recording while loading") {
                state.canStartRecording shouldBe false
            }
        }

        `when`("PC is not connected but manual controls are shown") {
            val state = MainUiState(
                isInitialized = true,
                isRecording = false,
                isLoadingRecording = false,
                isPcConnected = false,
                showManualControls = true,
                isCameraConnected = true
            )

            then("should be able to start recording with manual controls") {
                state.canStartRecording shouldBe true
            }
        }
    }

    given("MainUiState for stopping recording") {

        `when`("system is recording and not loading") {
            val state = MainUiState(
                isRecording = true,
                isLoadingRecording = false
            )

            then("should be able to stop recording") {
                state.canStopRecording shouldBe true
            }
        }

        `when`("system is not recording") {
            val state = MainUiState(
                isRecording = false,
                isLoadingRecording = false
            )

            then("should not be able to stop recording when not recording") {
                state.canStopRecording shouldBe false
            }
        }

        `when`("system is loading") {
            val state = MainUiState(
                isRecording = true,
                isLoadingRecording = true
            )

            then("should not be able to stop recording while loading") {
                state.canStopRecording shouldBe false
            }
        }
    }

    given("MainUiState for calibration operations") {

        `when`("system is ready and not busy") {
            val state = MainUiState(
                isInitialized = true,
                isRecording = false,
                isCalibrationRunning = false,
                isLoadingCalibration = false
            )

            then("should be able to run calibration") {
                state.canRunCalibration shouldBe true
            }
        }

        `when`("system is recording") {
            val state = MainUiState(
                isInitialized = true,
                isRecording = true,
                isCalibrationRunning = false,
                isLoadingCalibration = false
            )

            then("should not be able to run calibration while recording") {
                state.canRunCalibration shouldBe false
            }
        }

        `when`("calibration is already running") {
            val state = MainUiState(
                isInitialized = true,
                isRecording = false,
                isCalibrationRunning = true,
                isLoadingCalibration = false
            )

            then("should not be able to run calibration again") {
                state.canRunCalibration shouldBe false
            }
        }
    }

    given("MainUiState system health status") {

        `when`("system is not initialized") {
            val state = MainUiState(isInitialized = false)

            then("should return DISCONNECTED overall health status") {
                state.systemHealthStatus.overallHealth shouldBe SystemHealthStatus.HealthStatus.DISCONNECTED
            }
        }

        `when`("error message is present") {
            val state = MainUiState(
                isInitialized = true,
                errorMessage = "Test error"
            )

            then("should return DISCONNECTED overall health when has error") {
                state.systemHealthStatus.overallHealth shouldBe SystemHealthStatus.HealthStatus.DISCONNECTED
            }
        }

        `when`("system is recording") {
            val state = MainUiState(
                isInitialized = true,
                isRecording = true,
                errorMessage = null,
                isPcConnected = true,
                isCameraConnected = true
            )

            then("should return CONNECTED overall health when recording") {
                state.systemHealthStatus.overallHealth shouldBe SystemHealthStatus.HealthStatus.CONNECTED
            }
        }

        `when`("PC and sensors are connected") {
            val state = MainUiState(
                isInitialized = true,
                isRecording = false,
                errorMessage = null,
                isPcConnected = true,
                isShimmerConnected = true,
                isCameraConnected = true
            )

            then("should return CONNECTED overall health when ready") {
                state.systemHealthStatus.overallHealth shouldBe SystemHealthStatus.HealthStatus.CONNECTED
            }
        }

        `when`("only PC is connected") {
            val state = MainUiState(
                isInitialized = true,
                isRecording = false,
                errorMessage = null,
                isPcConnected = true,
                isShimmerConnected = false,
                isThermalConnected = false
            )

            then("should return DISCONNECTED overall health for partial connection") {
                state.systemHealthStatus.overallHealth shouldBe SystemHealthStatus.HealthStatus.DISCONNECTED
            }
        }

        `when`("nothing is connected") {
            val state = MainUiState(
                isInitialized = true,
                isRecording = false,
                errorMessage = null,
                isPcConnected = false,
                isShimmerConnected = false,
                isThermalConnected = false
            )

            then("should return DISCONNECTED overall health when nothing connected") {
                state.systemHealthStatus.overallHealth shouldBe SystemHealthStatus.HealthStatus.DISCONNECTED
            }
        }
    }

    given("BatteryStatus enum") {

        `when`("checking all enum values") {
            val statuses = BatteryStatus.values()

            then("should contain all expected statuses") {
                statuses shouldContain BatteryStatus.UNKNOWN
                statuses shouldContain BatteryStatus.CHARGING
                statuses shouldContain BatteryStatus.DISCHARGING
                statuses shouldContain BatteryStatus.LOW
                statuses shouldContain BatteryStatus.FULL
            }
        }
    }

    given("ShimmerDeviceInfo data class") {

        `when`("creating device info with valid data") {
            val deviceInfo = ShimmerDeviceInfo(
                deviceId = "Shimmer3-ABC123",
                batteryLevel = 75,
                signalStrength = 85,
                isConnected = true,
                lastDataReceived = System.currentTimeMillis()
            )

            then("should have correct properties") {
                deviceInfo.deviceId shouldBe "Shimmer3-ABC123"
                deviceInfo.batteryLevel shouldBe 75
                deviceInfo.signalStrength shouldBe 85
                deviceInfo.isConnected shouldBe true
                deviceInfo.lastDataReceived shouldNotBe null
            }
        }
    }

    given("SessionDisplayInfo data class") {

        `when`("creating session info with valid data") {
            val sessionInfo = SessionDisplayInfo(
                sessionId = "session_123",
                startTime = 1640995200000L,
                duration = 3661000L,
                deviceCount = 3,
                recordingMode = "multi_sensor",
                status = "completed"
            )

            then("should have correct properties") {
                sessionInfo.sessionId shouldBe "session_123"
                sessionInfo.startTime shouldBe 1640995200000L
                sessionInfo.duration shouldBe 3661000L
                sessionInfo.deviceCount shouldBe 3
                sessionInfo.recordingMode shouldBe "multi_sensor"
                sessionInfo.status shouldBe "completed"
            }
        }
    }
})
