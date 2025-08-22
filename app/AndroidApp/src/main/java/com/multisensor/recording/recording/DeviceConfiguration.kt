package com.multisensor.recording.recording

data class DeviceConfiguration(
    val samplingRate: Double = DEFAULT_SAMPLING_RATE,
    val enabledSensors: Set<SensorChannel> = setOf(SensorChannel.GSR, SensorChannel.PPG),
    val gsrRange: Int = DEFAULT_GSR_RANGE,
    val accelRange: Int = DEFAULT_ACCEL_RANGE,
    val gyroRange: Int = DEFAULT_GYRO_RANGE,
    val magRange: Int = DEFAULT_MAG_RANGE,
    val enableLowPowerMode: Boolean = false,
    val enableAutoCalibration: Boolean = true,
    val bufferSize: Int = DEFAULT_BUFFER_SIZE,
) {
    companion object {
        const val DEFAULT_SAMPLING_RATE = 51.2
        const val DEFAULT_GSR_RANGE = 4
        const val DEFAULT_ACCEL_RANGE = 2
        const val DEFAULT_GYRO_RANGE = 250
        const val DEFAULT_MAG_RANGE = 1
        const val DEFAULT_BUFFER_SIZE = 100

        const val SENSOR_GSR = 0x04
        const val SENSOR_PPG = 0x4000
        const val SENSOR_ACCEL = 0x80
        const val SENSOR_GYRO = 0x40
        const val SENSOR_MAG = 0x20
        const val SENSOR_ECG = 0x10
        const val SENSOR_EMG = 0x08

        fun createDefault(): DeviceConfiguration =
            DeviceConfiguration(
                samplingRate = DEFAULT_SAMPLING_RATE,
                enabledSensors = setOf(SensorChannel.GSR, SensorChannel.PPG, SensorChannel.ACCEL),
                gsrRange = DEFAULT_GSR_RANGE,
                accelRange = DEFAULT_ACCEL_RANGE,
            )

        fun createHighPerformance(): DeviceConfiguration =
            DeviceConfiguration(
                samplingRate = 128.0,
                enabledSensors = SensorChannel.values().toSet(),
                gsrRange = DEFAULT_GSR_RANGE,
                accelRange = DEFAULT_ACCEL_RANGE,
                enableAutoCalibration = true,
                bufferSize = 200,
            )

        fun createLowPower(): DeviceConfiguration =
            DeviceConfiguration(
                samplingRate = 25.6,
                enabledSensors = setOf(SensorChannel.GSR),
                enableLowPowerMode = true,
                bufferSize = 50,
            )
    }

    enum class SensorChannel(
        val displayName: String,
        val bitmask: Int,
    ) {
        GSR("GSR (Skin Conductance)", SENSOR_GSR),
        PPG("PPG (Heart Rate)", SENSOR_PPG),
        ACCEL("Accelerometer (Combined)", SENSOR_ACCEL),
        ACCEL_X("Accelerometer X", SENSOR_ACCEL),
        ACCEL_Y("Accelerometer Y", SENSOR_ACCEL),
        ACCEL_Z("Accelerometer Z", SENSOR_ACCEL),
        GYRO("Gyroscope (Combined)", SENSOR_GYRO),
        GYRO_X("Gyroscope X", SENSOR_GYRO),
        GYRO_Y("Gyroscope Y", SENSOR_GYRO),
        GYRO_Z("Gyroscope Z", SENSOR_GYRO),
        MAG("Magnetometer (Combined)", SENSOR_MAG),
        MAG_X("Magnetometer X", SENSOR_MAG),
        MAG_Y("Magnetometer Y", SENSOR_MAG),
        MAG_Z("Magnetometer Z", SENSOR_MAG),
        ECG("ECG", SENSOR_ECG),
        EMG("EMG", SENSOR_EMG),
        ;

        companion object {
            fun getGSRPlusChannels(): Set<SensorChannel> = setOf(GSR, PPG, ACCEL, GYRO, MAG)

            fun getAccelChannels(): Set<SensorChannel> = setOf(ACCEL_X, ACCEL_Y, ACCEL_Z)
            fun getGyroChannels(): Set<SensorChannel> = setOf(GYRO_X, GYRO_Y, GYRO_Z)
            fun getMagChannels(): Set<SensorChannel> = setOf(MAG_X, MAG_Y, MAG_Z)

            fun getAllIndividualChannels(): Set<SensorChannel> =
                setOf(GSR, PPG, ECG, EMG) + getAccelChannels() + getGyroChannels() + getMagChannels()
        }
    }

    fun getSensorBitmask(): Int = enabledSensors.fold(0) { acc, sensor -> acc or sensor.bitmask }

    fun getEnabledChannelCount(): Int = enabledSensors.size

    fun isSensorEnabled(sensor: SensorChannel): Boolean = sensor in enabledSensors

    fun getEstimatedDataRate(): Double = samplingRate * getEnabledChannelCount()

    fun getEstimatedBandwidth(): Int {
        return (getEstimatedDataRate() * 10).toInt()
    }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (samplingRate <= 0 || samplingRate > 1000) {
            errors.add("Sampling rate must be between 0 and 1000 Hz")
        }

        if (enabledSensors.isEmpty()) {
            errors.add("At least one sensor must be enabled")
        }

        if (gsrRange !in 1..8) {
            errors.add("GSR range must be between 1 and 8")
        }

        if (accelRange !in listOf(2, 4, 8, 16)) {
            errors.add("Accelerometer range must be 2, 4, 8, or 16g")
        }

        if (bufferSize <= 0 || bufferSize > 1000) {
            errors.add("Buffer size must be between 1 and 1000")
        }

        return errors
    }

    fun withSensors(sensors: Set<SensorChannel>): DeviceConfiguration = copy(enabledSensors = sensors)

    fun withSamplingRate(rate: Double): DeviceConfiguration = copy(samplingRate = rate)

    override fun toString(): String =
        "DeviceConfiguration(rate=${samplingRate}Hz, sensors=${enabledSensors.size}, " +
                "channels=[${enabledSensors.joinToString { it.displayName }}])"
}
