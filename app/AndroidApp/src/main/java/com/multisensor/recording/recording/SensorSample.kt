package com.multisensor.recording.recording

import com.multisensor.recording.recording.DeviceConfiguration.SensorChannel
import java.text.SimpleDateFormat
import java.util.*

data class SensorSample(
    val deviceId: String,
    val deviceTimestamp: Long,
    val systemTimestamp: Long = System.currentTimeMillis(),
    val sessionTimestamp: Long = 0L,
    val sensorValues: Map<SensorChannel, Double> = emptyMap(),
    val batteryLevel: Int = 0,
    val sequenceNumber: Long = 0L,
) {
    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        fun createGSRPlusSample(
            deviceId: String,
            deviceTimestamp: Long,
            gsrConductance: Double,
            ppgValue: Double,
            accelX: Double,
            accelY: Double,
            accelZ: Double,
            batteryLevel: Int = 0,
            sequenceNumber: Long = 0L,
        ): SensorSample {
            val sensorValues =
                mapOf(
                    SensorChannel.GSR to gsrConductance,
                    SensorChannel.PPG to ppgValue,
                    SensorChannel.ACCEL to accelX,
                )

            return SensorSample(
                deviceId = deviceId,
                deviceTimestamp = deviceTimestamp,
                sensorValues = sensorValues,
                batteryLevel = batteryLevel,
                sequenceNumber = sequenceNumber,
            )
        }

        fun createFullSample(
            deviceId: String,
            deviceTimestamp: Long,
            gsrConductance: Double,
            ppgValue: Double,
            accelX: Double,
            accelY: Double,
            accelZ: Double,
            gyroX: Double,
            gyroY: Double,
            gyroZ: Double,
            magX: Double,
            magY: Double,
            magZ: Double,
            batteryLevel: Int = 0,
            sequenceNumber: Long = 0L,
        ): SensorSample {
            val sensorValues =
                mapOf(
                    SensorChannel.GSR to gsrConductance,
                    SensorChannel.PPG to ppgValue,
                    SensorChannel.ACCEL to accelX,
                    SensorChannel.GYRO to gyroX,
                    SensorChannel.MAG to magX,
                )

            return SensorSample(
                deviceId = deviceId,
                deviceTimestamp = deviceTimestamp,
                sensorValues = sensorValues,
                batteryLevel = batteryLevel,
                sequenceNumber = sequenceNumber,
            )
        }

        fun createSimulatedSample(
            deviceId: String,
            sequenceNumber: Long,
            enabledSensors: Set<SensorChannel> = setOf(SensorChannel.GSR, SensorChannel.PPG, SensorChannel.ACCEL),
        ): SensorSample {
            val currentTime = System.currentTimeMillis()
            val sensorValues = mutableMapOf<SensorChannel, Double>()

            enabledSensors.forEach { sensor ->
                val value =
                    when (sensor) {
                        SensorChannel.GSR -> generatePhysiologicalGSR(sequenceNumber, deviceId)
                        SensorChannel.PPG -> generatePhysiologicalPPG(sequenceNumber, deviceId)
                        SensorChannel.ACCEL -> generatePhysiologicalAccel(sequenceNumber, deviceId)
                        SensorChannel.GYRO -> generatePhysiologicalGyro(sequenceNumber, deviceId)
                        SensorChannel.MAG -> generatePhysiologicalMag(sequenceNumber, deviceId)
                        SensorChannel.ECG -> generatePhysiologicalECG(sequenceNumber, deviceId)
                        SensorChannel.EMG -> generatePhysiologicalEMG(sequenceNumber, deviceId)
                        else -> 0.0
                    }
                sensorValues[sensor] = value
            }

            return SensorSample(
                deviceId = deviceId,
                deviceTimestamp = currentTime,
                sensorValues = sensorValues,
                batteryLevel = (80 + (sequenceNumber % 20)).toInt(),
                sequenceNumber = sequenceNumber,
            )
        }

        /**
         * Generate physiologically realistic GSR data based on human autonomic nervous system patterns
         */
        fun generatePhysiologicalGSR(sequenceNumber: Long, deviceId: String): Double {
            val timeSeconds = sequenceNumber * 0.01 // Assuming 100Hz sampling
            
            // Base conductance (typical resting: 2-10 μS)
            val baseGSR = 3.5
            
            // Slow tonic changes (1-5 minute cycles)
            val tonicDrift = kotlin.math.sin(timeSeconds * kotlin.math.PI / 180.0) * 0.8
            
            // Phasic responses (spontaneous fluctuations every 20-60 seconds)
            val phasicResponse = kotlin.math.sin(timeSeconds * 0.1 + deviceId.hashCode() * 0.01) * 0.5
            
            // Breathing-related modulation (16 breaths per minute)
            val breathingModulation = kotlin.math.sin(timeSeconds * 2 * kotlin.math.PI * 16.0 / 60.0) * 0.1
            
            // Small physiological variation (not random - deterministic based on sequence)
            val physiologicalNoise = kotlin.math.sin(timeSeconds * 3.7 + deviceId.hashCode()) * 0.05
            
            return (baseGSR + tonicDrift + phasicResponse + breathingModulation + physiologicalNoise)
                .coerceIn(0.5, 15.0)
        }

        /**
         * Generate physiologically realistic PPG data based on cardiac physiology
         */
        fun generatePhysiologicalPPG(sequenceNumber: Long, deviceId: String): Double {
            val timeSeconds = sequenceNumber * 0.01
            
            // Heart rate with realistic variability (60-80 BPM)
            val baseHeartRate = 72.0
            val heartRateVariability = kotlin.math.sin(timeSeconds * 0.1) * 5.0
            val currentHeartRate = baseHeartRate + heartRateVariability
            
            // Primary cardiac pulse
            val cardiacPulse = kotlin.math.sin(2 * kotlin.math.PI * currentHeartRate / 60.0 * timeSeconds) * 150
            
            // Dicrotic notch (secondary pulse wave)
            val dicroticNotch = kotlin.math.sin(4 * kotlin.math.PI * currentHeartRate / 60.0 * timeSeconds) * 30
            
            // Respiratory modulation (affects PPG amplitude)
            val respiratoryModulation = kotlin.math.sin(2 * kotlin.math.PI * 16.0 / 60.0 * timeSeconds) * 20
            
            // Baseline and device-specific offset
            val baseline = 2048.0
            val deviceOffset = (deviceId.hashCode() % 200).toDouble()
            
            return baseline + cardiacPulse + dicroticNotch + respiratoryModulation + deviceOffset
        }

        /**
         * Generate physiologically realistic accelerometer data based on human movement
         */
        fun generatePhysiologicalAccel(sequenceNumber: Long, deviceId: String): Double {
            val timeSeconds = sequenceNumber * 0.01
            
            // Gravity component (varies with device orientation)
            val gravityComponent = when (sequenceNumber % 3) {
                0L -> 9.8  // Z-axis typically points up
                1L -> 0.0  // X-axis
                else -> 0.0 // Y-axis
            }
            
            // Breathing-related chest movement
            val breathingMovement = kotlin.math.sin(2 * kotlin.math.PI * 16.0 / 60.0 * timeSeconds) * 0.02
            
            // Heart-induced micromovements (ballistocardiography)
            val heartMovement = kotlin.math.sin(2 * kotlin.math.PI * 72.0 / 60.0 * timeSeconds) * 0.005
            
            // Postural sway (slow, large-scale movements)
            val posturalSway = kotlin.math.sin(timeSeconds * 0.5 + deviceId.hashCode() * 0.1) * 0.01
            
            return gravityComponent + breathingMovement + heartMovement + posturalSway
        }

        /**
         * Generate physiologically realistic gyroscope data based on human body rotation
         */
        fun generatePhysiologicalGyro(sequenceNumber: Long, deviceId: String): Double {
            val timeSeconds = sequenceNumber * 0.01
            
            // Small head movements during breathing
            val breathingRotation = kotlin.math.sin(2 * kotlin.math.PI * 16.0 / 60.0 * timeSeconds) * 2.0
            
            // Postural adjustments (larger, slower movements)
            val posturalRotation = kotlin.math.sin(timeSeconds * 0.1 + deviceId.hashCode() * 0.01) * 1.0
            
            // Physiological tremor (8-12 Hz)
            val physiologicalTremor = kotlin.math.sin(2 * kotlin.math.PI * 10.0 * timeSeconds) * 0.3
            
            return breathingRotation + posturalRotation + physiologicalTremor
        }

        /**
         * Generate physiologically realistic magnetometer data 
         */
        fun generatePhysiologicalMag(sequenceNumber: Long, deviceId: String): Double {
            val timeSeconds = sequenceNumber * 0.01
            
            // Earth's magnetic field (typical: 25-65 μT)
            val earthMagneticField = 45.0
            
            // Slow variations due to device orientation changes
            val orientationChange = kotlin.math.sin(timeSeconds * 0.01 + deviceId.hashCode() * 0.001) * 5.0
            
            // Small movements affecting magnetic field measurement
            val movementNoise = kotlin.math.sin(timeSeconds * 0.5) * 0.5
            
            return earthMagneticField + orientationChange + movementNoise
        }

        /**
         * Generate physiologically realistic ECG data based on cardiac electrophysiology
         */
        fun generatePhysiologicalECG(sequenceNumber: Long, deviceId: String): Double {
            val timeSeconds = sequenceNumber * 0.01
            
            // Heart rate (60-80 BPM)
            val heartRate = 72.0
            val heartPeriod = 60.0 / heartRate
            val cardiacPhase = (timeSeconds % heartPeriod) / heartPeriod
            
            // ECG waveform components
            val pWave = if (cardiacPhase < 0.1) kotlin.math.sin(cardiacPhase * 10 * kotlin.math.PI) * 0.1 else 0.0
            val qrsComplex = if (cardiacPhase in 0.15..0.25) {
                kotlin.math.sin((cardiacPhase - 0.15) * 10 * kotlin.math.PI) * 1.0
            } else 0.0
            val tWave = if (cardiacPhase in 0.4..0.6) {
                kotlin.math.sin((cardiacPhase - 0.4) * 5 * kotlin.math.PI) * 0.3
            } else 0.0
            
            // Baseline and device offset
            val baseline = 0.0
            val deviceOffset = (deviceId.hashCode() % 10) * 0.01
            
            return baseline + pWave + qrsComplex + tWave + deviceOffset
        }

        /**
         * Generate physiologically realistic EMG data based on muscle electrical activity
         */
        fun generatePhysiologicalEMG(sequenceNumber: Long, deviceId: String): Double {
            val timeSeconds = sequenceNumber * 0.01
            
            // Base muscle tone (minimal background activity)
            val baseTone = 0.01
            
            // Breathing-related muscle activity (intercostal muscles)
            val breathingEMG = kotlin.math.sin(2 * kotlin.math.PI * 16.0 / 60.0 * timeSeconds) * 0.02
            
            // Postural muscle activity (varies with position maintenance)
            val posturalEMG = kotlin.math.sin(timeSeconds * 0.3 + deviceId.hashCode() * 0.1) * 0.03
            
            // Motor unit firing patterns (not random - physiological)
            val motorUnitActivity = kotlin.math.sin(timeSeconds * 15.0 + deviceId.hashCode()) * 0.01
            
            return baseTone + breathingEMG + posturalEMG + motorUnitActivity
        }
    }

    fun getSensorValue(channel: SensorChannel): Double? = sensorValues[channel]

    fun hasSensorData(channel: SensorChannel): Boolean = channel in sensorValues

    fun getAvailableChannels(): Set<SensorChannel> = sensorValues.keys

    fun getChannelCount(): Int = sensorValues.size

    fun toCsvString(includeHeader: Boolean = false): String {
        val header =
            if (includeHeader) {
                "Timestamp_ms,DeviceTime_ms,SystemTime_ms,SessionTime_ms,DeviceId,SequenceNumber," +
                        "GSR_Conductance_uS,PPG_A13," +
                        "Accel_X_g,Accel_Y_g,Accel_Z_g," +
                        "Gyro_X_dps,Gyro_Y_dps,Gyro_Z_dps," +
                        "Mag_X_gauss,Mag_Y_gauss,Mag_Z_gauss," +
                        "ECG_mV,EMG_mV,Battery_Percentage\n"
            } else {
                ""
            }

        val values =
            listOf(
                systemTimestamp,
                deviceTimestamp,
                systemTimestamp,
                sessionTimestamp,
                deviceId,
                sequenceNumber,
                getSensorValue(SensorChannel.GSR) ?: 0.0,
                getSensorValue(SensorChannel.PPG) ?: 0.0,
                getSensorValue(SensorChannel.ACCEL_X) ?: getSensorValue(SensorChannel.ACCEL) ?: 0.0,
                getSensorValue(SensorChannel.ACCEL_Y) ?: 0.0,
                getSensorValue(SensorChannel.ACCEL_Z) ?: 0.0,
                getSensorValue(SensorChannel.GYRO_X) ?: getSensorValue(SensorChannel.GYRO) ?: 0.0,
                getSensorValue(SensorChannel.GYRO_Y) ?: 0.0,
                getSensorValue(SensorChannel.GYRO_Z) ?: 0.0,
                getSensorValue(SensorChannel.MAG_X) ?: getSensorValue(SensorChannel.MAG) ?: 0.0,
                getSensorValue(SensorChannel.MAG_Y) ?: 0.0,
                getSensorValue(SensorChannel.MAG_Z) ?: 0.0,
                getSensorValue(SensorChannel.ECG) ?: 0.0,
                getSensorValue(SensorChannel.EMG) ?: 0.0,
                batteryLevel,
            ).joinToString(",")

        return header + values
    }

    fun toJsonString(): String {
        val sensorData =
            sensorValues.entries.joinToString(",") { (channel, value) ->
                "\"${channel.name}\":$value"
            }

        return """{
            "deviceId":"$deviceId",
            "deviceTimestamp":$deviceTimestamp,
            "systemTimestamp":$systemTimestamp,
            "sessionTimestamp":$sessionTimestamp,
            "sequenceNumber":$sequenceNumber,
            "batteryLevel":$batteryLevel,
            "sensorData":{$sensorData}
        }""".replace("\n", "").replace("  ", "")
    }

    fun getFormattedTimestamp(): String = dateFormat.format(Date(systemTimestamp))

    fun getTimeDifference(other: SensorSample): Long = Math.abs(this.systemTimestamp - other.systemTimestamp)

    fun isWithinTimeWindow(
        other: SensorSample,
        windowMs: Long,
    ): Boolean = getTimeDifference(other) <= windowMs

    fun withSessionTimestamp(sessionStart: Long): SensorSample = copy(sessionTimestamp = systemTimestamp - sessionStart)

    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (deviceId.isBlank()) {
            errors.add("Device ID cannot be blank")
        }

        if (deviceTimestamp <= 0) {
            errors.add("Device timestamp must be positive")
        }

        if (systemTimestamp <= 0) {
            errors.add("System timestamp must be positive")
        }

        if (sensorValues.isEmpty()) {
            errors.add("Sample must contain at least one sensor value")
        }

        if (batteryLevel < 0 || batteryLevel > 100) {
            errors.add("Battery level must be between 0 and 100")
        }

        sensorValues.forEach { (channel, value) ->
            when (channel) {
                SensorChannel.GSR -> {
                    if (value < 0 || value > 100) {
                        errors.add("GSR value out of range: $value µS")
                    }
                }

                SensorChannel.PPG -> {
                    if (value < 0 || value > 4096) {
                        errors.add("PPG value out of range: $value")
                    }
                }

                SensorChannel.ACCEL -> {
                    if (Math.abs(value) > 50) {
                        errors.add("Accelerometer value out of range: $value g")
                    }
                }

                else -> {
                    if (!value.isFinite()) {
                        errors.add("${channel.name} value is not finite: $value")
                    }
                }
            }
        }

        return errors
    }

    override fun toString(): String {
        val sensorSummary =
            sensorValues.entries.take(3).joinToString(", ") { (channel, value) ->
                "${channel.name}=%.2f".format(value)
            }
        return "SensorSample($deviceId, seq=$sequenceNumber, ${getFormattedTimestamp()}, [$sensorSummary], bat=$batteryLevel%)"
    }

}
