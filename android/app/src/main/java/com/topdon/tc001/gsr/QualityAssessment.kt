package com.topdon.tc001.gsr

data class QualityAssessment(
    val overallScore: Double,
    val gsrScore: Double,
    val temperatureScore: Double,
    val snrRatio: Double,
    val artifactLevel: Double,
    val timestamp: Long = System.currentTimeMillis()
)