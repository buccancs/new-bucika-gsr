package com.multisensor.recording.util
data class TemperatureRange(
    val min: Float,
    val max: Float,
    val displayName: String = "${min.toInt()}°C - ${max.toInt()}°C"
) {
    init {
        require(min <= max) { "Minimum temperature must be less than or equal to maximum temperature" }
    }
    val span: Float get() = max - min
    fun contains(temperature: Float): Boolean = temperature in min..max
    fun normalise(temperature: Float): Float {
        if (span == 0f) return 0f
        return ((temperature - min) / span).coerceIn(0f, 1f)
    }
    fun fromNormalized(normalised: Float): Float {
        return min + (normalised.coerceIn(0f, 1f) * span)
    }
    companion object {
        val BODY_TEMPERATURE = TemperatureRange(30f, 40f, "Body Temp")
        val ROOM_TEMPERATURE = TemperatureRange(15f, 35f, "Room Temp")
        val ENVIRONMENT = TemperatureRange(0f, 50f, "Environment")
        val GENERAL = TemperatureRange(-20f, 120f, "General")
        val INDUSTRIAL = TemperatureRange(-10f, 200f, "Industrial")
        val AUTOMOTIVE = TemperatureRange(-40f, 150f, "Automotive")
        
        fun centred(centre: Float, span: Float): TemperatureRange {
            val halfSpan = span / 2f
            return TemperatureRange(centre - halfSpan, centre + halfSpan)
        }
    }
}