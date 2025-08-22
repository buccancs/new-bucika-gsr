package com.multisensor.recording.controllers

import android.hardware.usb.UsbDevice

class UsbDevicePrioritizer(private val performanceAnalytics: UsbPerformanceAnalytics) {

    companion object {
        private const val QUALITY_WEIGHT = 0.35
        private const val HISTORY_WEIGHT = 0.25
        private const val CHARACTERISTICS_WEIGHT = 0.20
        private const val EFFICIENCY_WEIGHT = 0.20

        private const val MAX_RESPONSE_TIME_MS = 50.0
        private const val MAX_CONNECTION_COUNT = 1000
        private const val LEARNING_RATE = 0.1

        private val MODEL_PRIORITY_MAP = mapOf(
            "TC001" to 1.0,
            "TC001_Plus" to 0.95,
            "TC001_Pro" to 0.9,
            "TC001_Lite" to 0.8
        )
    }

    enum class PriorityLevel {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        DISABLED
    }

    data class DevicePriorityAssessment(
        val deviceKey: String,
        val device: UsbDevice,
        val priorityLevel: PriorityLevel,
        val priorityScore: Double,
        val qualityScore: Double,
        val historyScore: Double,
        val characteristicsScore: Double,
        val efficiencyScore: Double,
        val confidence: Double,
        val recommendations: List<String>
    )

    data class DeviceSelectionResult(
        val primaryDevice: DevicePriorityAssessment?,
        val secondaryDevices: List<DevicePriorityAssessment>,
        val allDeviceAssessments: List<DevicePriorityAssessment>,
        val selectionRationale: String,
        val optimizationMetrics: SelectionOptimizationMetrics
    )

    data class SelectionOptimizationMetrics(
        val totalQualityScore: Double,
        val expectedReliability: Double,
        val resourceEfficiency: Double,
        val diversityIndex: Double,
        val riskScore: Double,
        val optimizationConfidence: Double
    )

    private val deviceScoreHistory = mutableMapOf<String, MutableList<Double>>()
    private val devicePerformanceWeights = mutableMapOf<String, MutableMap<String, Double>>()

    fun assessDevicePriority(
        deviceKey: String,
        device: UsbDevice,
        connectionTime: Long?,
        connectionCount: Int
    ): DevicePriorityAssessment {

        val qualityScore = calculateQualityScore(deviceKey)
        val historyScore = calculateHistoryScore(deviceKey, connectionTime, connectionCount)
        val characteristicsScore = calculateCharacteristicsScore(device)
        val efficiencyScore = calculateEfficiencyScore(deviceKey)

        val adaptiveWeights = getAdaptiveWeights(deviceKey)

        val priorityScore = (
                qualityScore * adaptiveWeights.getOrDefault("quality", QUALITY_WEIGHT) +
                        historyScore * adaptiveWeights.getOrDefault("history", HISTORY_WEIGHT) +
                        characteristicsScore * adaptiveWeights.getOrDefault("characteristics", CHARACTERISTICS_WEIGHT) +
                        efficiencyScore * adaptiveWeights.getOrDefault("efficiency", EFFICIENCY_WEIGHT)
                )

        val priorityLevel = determinePriorityLevel(priorityScore, qualityScore)

        val confidence = calculateAssessmentConfidence(deviceKey, priorityScore)

        val recommendations = generateDeviceRecommendations(
            qualityScore, historyScore, characteristicsScore, efficiencyScore
        )

        updateScoreHistory(deviceKey, priorityScore)

        return DevicePriorityAssessment(
            deviceKey = deviceKey,
            device = device,
            priorityLevel = priorityLevel,
            priorityScore = priorityScore,
            qualityScore = qualityScore,
            historyScore = historyScore,
            characteristicsScore = characteristicsScore,
            efficiencyScore = efficiencyScore,
            confidence = confidence,
            recommendations = recommendations
        )
    }

    fun optimizeDeviceSelection(
        deviceAssessments: List<DevicePriorityAssessment>,
        maxDevices: Int = 3
    ): DeviceSelectionResult {

        if (deviceAssessments.isEmpty()) {
            return getEmptySelectionResult()
        }

        val sortedDevices = deviceAssessments.sortedWith(
            compareByDescending<DevicePriorityAssessment> { it.priorityScore }
                .thenByDescending { it.confidence }
                .thenBy { it.deviceKey }
        )

        val primaryDevice = sortedDevices.firstOrNull {
            it.priorityLevel in listOf(PriorityLevel.CRITICAL, PriorityLevel.HIGH) &&
                    it.qualityScore > 0.7
        } ?: sortedDevices.firstOrNull()

        val secondaryDevices = selectSecondaryDevices(
            sortedDevices.filter { it != primaryDevice },
            maxDevices - 1
        )

        val optimizationMetrics = calculateOptimizationMetrics(
            primaryDevice, secondaryDevices
        )

        val selectionRationale = generateSelectionRationale(
            primaryDevice, secondaryDevices, optimizationMetrics
        )

        return DeviceSelectionResult(
            primaryDevice = primaryDevice,
            secondaryDevices = secondaryDevices,
            allDeviceAssessments = sortedDevices,
            selectionRationale = selectionRationale,
            optimizationMetrics = optimizationMetrics
        )
    }

    fun updateDevicePriorityFeedback(
        deviceKey: String,
        performanceScore: Double,
        actualReliability: Double,
        resourceUsage: Double
    ) {
        val currentAssessment = deviceScoreHistory[deviceKey]?.lastOrNull() ?: 0.5
        val predictionError = performanceScore - currentAssessment

        val weights = devicePerformanceWeights.computeIfAbsent(deviceKey) {
            mutableMapOf(
                "quality" to QUALITY_WEIGHT,
                "history" to HISTORY_WEIGHT,
                "characteristics" to CHARACTERISTICS_WEIGHT,
                "efficiency" to EFFICIENCY_WEIGHT
            )
        }

        if (kotlin.math.abs(predictionError) > 0.1) {
            weights["quality"] = weights["quality"]!! * (1 + LEARNING_RATE * predictionError)
            weights["efficiency"] = weights["efficiency"]!! * (1 + LEARNING_RATE * (resourceUsage - 0.5))

            val totalWeight = weights.values.sum()
            weights.forEach { (key, value) -> weights[key] = value / totalWeight }
        }

        updateScoreHistory(deviceKey, performanceScore)
    }

    fun generatePriorityAnalysisReport(assessments: List<DevicePriorityAssessment>): String {
        return buildString {
            append("Advanced Device Priority Analysis Report\n")
            append("═══════════════════════════════════════════\n\n")

            append("Multi-Criteria Decision Analysis Summary:\n")
            append("• Evaluation Criteria: 4 (Quality, History, Characteristics, Efficiency)\n")
            append("• Total Devices Assessed: ${assessments.size}\n")
            append("• Average Confidence: ${"%.3f".format(assessments.map { it.confidence }.average())}\n\n")

            append("Priority Distribution:\n")
            PriorityLevel.values().forEach { level ->
                val count = assessments.count { it.priorityLevel == level }
                val percentage = if (assessments.isNotEmpty()) (count * 100.0) / assessments.size else 0.0
                append("• ${level.name}: $count devices (${"%.1f".format(percentage)}%)\n")
            }
            append("\n")

            append("Detailed Device Rankings:\n")
            append("┌─────────────────────────────────────────────────────────────────────┐\n")
            append("│ Rank │ Device Key          │ Priority │ Score │ Quality│ History│ Conf │\n")
            append("├─────────────────────────────────────────────────────────────────────┤\n")

            assessments.sortedByDescending { it.priorityScore }.forEachIndexed { index, assessment ->
                append(
                    "│ %4d │ %-19s │ %-8s │ %.3f │ %.3f  │ %.3f  │ %.3f │\n".format(
                        index + 1,
                        assessment.deviceKey.take(19),
                        assessment.priorityLevel.name.take(8),
                        assessment.priorityScore,
                        assessment.qualityScore,
                        assessment.historyScore,
                        assessment.confidence
                    )
                )
            }
            append("└─────────────────────────────────────────────────────────────────────┘\n\n")

            val scores = assessments.map { it.priorityScore }
            if (scores.isNotEmpty()) {
                append("Statistical Analysis:\n")
                append("• Mean Priority Score: ${"%.4f".format(scores.average())}\n")
                append("• Standard Deviation: ${"%.4f".format(calculateStandardDeviation(scores))}\n")
                append("• Median Score: ${"%.4f".format(scores.sorted()[scores.size / 2])}\n")
                append("• Score Range: ${"%.4f".format(scores.maxOrNull()!! - scores.minOrNull()!!)}\n\n")
            }

            append("System Recommendations:\n")
            val highPriorityDevices = assessments.filter {
                it.priorityLevel in listOf(PriorityLevel.CRITICAL, PriorityLevel.HIGH)
            }

            when {
                highPriorityDevices.isEmpty() ->
                    append("⚠️  No high-priority devices available - consider device troubleshooting\n")

                highPriorityDevices.size == 1 ->
                    append("ℹ️  Single high-priority device - consider redundancy planning\n")

                else ->
                    append("✅ Multiple high-priority devices available - optimal configuration\n")
            }

            val lowConfidenceDevices = assessments.filter { it.confidence < 0.7 }
            if (lowConfidenceDevices.isNotEmpty()) {
                append("🔍 ${lowConfidenceDevices.size} device(s) need more performance data for accurate assessment\n")
            }
        }
    }

    private fun calculateQualityScore(deviceKey: String): Double {
        val qualityMetrics = performanceAnalytics.calculateConnectionQuality(deviceKey)
        return qualityMetrics.overallQuality
    }

    private fun calculateHistoryScore(deviceKey: String, connectionTime: Long?, connectionCount: Int): Double {
        val reliabilityScore = minOf(connectionCount.toDouble() / MAX_CONNECTION_COUNT, 1.0)

        val recencyScore = connectionTime?.let { time ->
            val hoursSinceConnection = (System.currentTimeMillis() - time) / (1000.0 * 3600.0)
            kotlin.math.exp(-hoursSinceConnection / 24.0)
        } ?: 0.0

        return reliabilityScore * 0.7 + recencyScore * 0.3
    }

    private fun calculateCharacteristicsScore(device: UsbDevice): Double {
        val modelPriority = getModelPriority(device)

        val vendorScore = if (device.vendorId == 0x0BDA) 1.0 else 0.8

        val featureScore = when (device.productId) {
            0x3901 -> 1.0
            0x5840 -> 0.95
            0x5830 -> 0.9
            0x5838 -> 0.85
            else -> 0.7
        }

        return (modelPriority * 0.4 + vendorScore * 0.3 + featureScore * 0.3)
    }

    private fun calculateEfficiencyScore(deviceKey: String): Double {
        val resourceMetrics = performanceAnalytics.getResourceUtilization()

        val cpuEfficiency = resourceMetrics["efficiency_score"] ?: 0.8

        val memoryEfficiency = 1.0 - (resourceMetrics["memory_usage"] ?: 0.2)

        val eventEfficiency = minOf((resourceMetrics["event_rate"] ?: 5.0) / 10.0, 1.0)

        return (cpuEfficiency * 0.5 + memoryEfficiency * 0.3 + eventEfficiency * 0.2)
    }

    private fun getModelPriority(device: UsbDevice): Double {
        val deviceName = device.deviceName.lowercase()
        return MODEL_PRIORITY_MAP.entries.firstOrNull { (model, _) ->
            deviceName.contains(model.lowercase())
        }?.value ?: 0.8
    }

    private fun determinePriorityLevel(priorityScore: Double, qualityScore: Double): PriorityLevel {
        return when {
            priorityScore >= 0.9 && qualityScore >= 0.8 -> PriorityLevel.CRITICAL
            priorityScore >= 0.75 -> PriorityLevel.HIGH
            priorityScore >= 0.5 -> PriorityLevel.MEDIUM
            priorityScore >= 0.25 -> PriorityLevel.LOW
            else -> PriorityLevel.DISABLED
        }
    }

    private fun calculateAssessmentConfidence(deviceKey: String, priorityScore: Double): Double {
        val scoreHistory = deviceScoreHistory[deviceKey] ?: return 0.5

        if (scoreHistory.size < 2) return 0.5

        val variance = calculateVariance(scoreHistory)
        val stabilityConfidence = kotlin.math.exp(-variance * 10)

        val dataConfidence = minOf(scoreHistory.size.toDouble() / 10.0, 1.0)

        return (stabilityConfidence * 0.7 + dataConfidence * 0.3)
    }

    private fun selectSecondaryDevices(
        availableDevices: List<DevicePriorityAssessment>,
        maxCount: Int
    ): List<DevicePriorityAssessment> {
        if (availableDevices.isEmpty() || maxCount <= 0) return emptyList()

        val selected = mutableListOf<DevicePriorityAssessment>()
        val remaining = availableDevices.toMutableList()

        repeat(minOf(maxCount, remaining.size)) {
            val bestDevice = remaining.maxByOrNull { device ->
                device.priorityScore + calculateDiversityBonus(device, selected)
            }

            bestDevice?.let {
                selected.add(it)
                remaining.remove(it)
            }
        }

        return selected
    }

    private fun calculateDiversityBonus(
        candidate: DevicePriorityAssessment,
        selected: List<DevicePriorityAssessment>
    ): Double {
        if (selected.isEmpty()) return 0.0

        val modelDiversity = if (selected.none {
                it.device.productId == candidate.device.productId
            }) 0.1 else 0.0

        val performanceDiversity = selected.minOfOrNull { selected ->
            kotlin.math.abs(selected.priorityScore - candidate.priorityScore)
        } ?: 0.0

        return modelDiversity + performanceDiversity * 0.1
    }

    private fun calculateOptimizationMetrics(
        primary: DevicePriorityAssessment?,
        secondary: List<DevicePriorityAssessment>
    ): SelectionOptimizationMetrics {
        val allSelected = listOfNotNull(primary) + secondary

        val totalQualityScore = allSelected.sumOf { it.qualityScore } / maxOf(allSelected.size, 1)
        val expectedReliability = allSelected.sumOf { it.historyScore } / maxOf(allSelected.size, 1)
        val resourceEfficiency = allSelected.sumOf { it.efficiencyScore } / maxOf(allSelected.size, 1)

        val diversityIndex = if (allSelected.size > 1) {
            1.0 - allSelected.map { it.priorityLevel }.groupingBy { it }.eachCount().values
                .sumOf { count -> (count.toDouble() / allSelected.size).let { it * it } }
        } else 0.0

        val riskScore = 1.0 - (allSelected.sumOf { it.confidence } / maxOf(allSelected.size, 1))
        val optimizationConfidence = allSelected.sumOf { it.confidence } / maxOf(allSelected.size, 1)

        return SelectionOptimizationMetrics(
            totalQualityScore = totalQualityScore,
            expectedReliability = expectedReliability,
            resourceEfficiency = resourceEfficiency,
            diversityIndex = diversityIndex,
            riskScore = riskScore,
            optimizationConfidence = optimizationConfidence
        )
    }

    private fun getAdaptiveWeights(deviceKey: String): Map<String, Double> {
        return devicePerformanceWeights[deviceKey] ?: mapOf(
            "quality" to QUALITY_WEIGHT,
            "history" to HISTORY_WEIGHT,
            "characteristics" to CHARACTERISTICS_WEIGHT,
            "efficiency" to EFFICIENCY_WEIGHT
        )
    }

    private fun updateScoreHistory(deviceKey: String, score: Double) {
        val history = deviceScoreHistory.computeIfAbsent(deviceKey) { mutableListOf() }
        history.add(score)

        if (history.size > 50) {
            history.removeAt(0)
        }
    }

    private fun generateDeviceRecommendations(
        qualityScore: Double,
        historyScore: Double,
        characteristicsScore: Double,
        efficiencyScore: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (qualityScore < 0.5) recommendations.add("Check physical connection quality")
        if (historyScore < 0.3) recommendations.add("Device needs more usage history for accurate assessment")
        if (characteristicsScore < 0.7) recommendations.add("Consider upgrading to newer device model")
        if (efficiencyScore < 0.6) recommendations.add("Monitor resource usage - may impact system performance")

        if (recommendations.isEmpty()) {
            recommendations.add("Device performing optimally - no issues detected")
        }

        return recommendations
    }

    private fun generateSelectionRationale(
        primary: DevicePriorityAssessment?,
        secondary: List<DevicePriorityAssessment>,
        metrics: SelectionOptimizationMetrics
    ): String {
        return buildString {
            append("Device Selection Optimisation Results:\n")
            append("• Primary Device: ${primary?.deviceKey ?: "None"} ")
            append("(Score: ${"%.3f".format(primary?.priorityScore ?: 0.0)})\n")
            append("• Secondary Devices: ${secondary.size}\n")
            append("• Total Quality Score: ${"%.3f".format(metrics.totalQualityScore)}\n")
            append("• Expected Reliability: ${"%.3f".format(metrics.expectedReliability)}\n")
            append("• Resource Efficiency: ${"%.3f".format(metrics.resourceEfficiency)}\n")
            append("• Configuration Risk: ${"%.3f".format(metrics.riskScore)}\n")
        }
    }

    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return kotlin.math.sqrt(variance)
    }

    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.sumOf { (it - mean) * (it - mean) } / values.size
    }

    private fun getEmptySelectionResult(): DeviceSelectionResult {
        return DeviceSelectionResult(
            primaryDevice = null,
            secondaryDevices = emptyList(),
            allDeviceAssessments = emptyList(),
            selectionRationale = "No devices available for selection",
            optimizationMetrics = SelectionOptimizationMetrics(0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        )
    }
}
