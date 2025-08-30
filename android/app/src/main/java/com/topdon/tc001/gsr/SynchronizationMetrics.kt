package com.topdon.tc001.gsr

/**
 * Synchronization metrics for GSR and thermal capture coordination
 */
data class SynchronizationMetrics(
    val timestamp: Long = System.nanoTime(),
    val clockOffset: Long = 0L,
    val precision: Long = 1000000L, // 1ms precision in nanoseconds
    val gsrSamplesCaptured: Long = 0L,
    val totalGSRSamplesSynced: Long = 0L,
    val averageTemporalDriftNs: Double = 0.0
)