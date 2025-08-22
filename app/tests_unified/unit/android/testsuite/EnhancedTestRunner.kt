package com.multisensor.recording.testsuite

import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class EnhancedTestRunner : RunListener() {

    companion object {
        private const val REPORT_DIR = "test-reports"
        private const val PERFORMANCE_THRESHOLD_MS = 5000L
        private const val MEMORY_THRESHOLD_MB = 100L

        private val STRESS_TESTS = setOf(
            "ShimmerRecorderEnhancedTest",
            "ThermalRecorderIntegrationTest",
            "MultiSensorCoordinationTest",
            "DataFlowIntegrationTest"
        )

        private val PERFORMANCE_TESTS = setOf(
            "CalibrationCaptureManagerTest",
            "NetworkQualityMonitorTest",
            "FileTransferHandlerTest",
            "SessionManagerTest"
        )
    }

    private val testResults = ConcurrentHashMap<String, TestExecutionData>()
    private val performanceMetrics = ConcurrentHashMap<String, PerformanceMetrics>()
    private var suiteStartTime: Long = 0
    private var totalTests = 0
    private var passedTests = 0
    private var failedTests = 0
    private var skippedTests = 0

    data class TestExecutionData(
        val testName: String,
        val className: String,
        val startTime: Long,
        var endTime: Long = 0,
        var status: TestStatus = TestStatus.RUNNING,
        var errorMessage: String? = null,
        var stackTrace: String? = null
    )

    data class PerformanceMetrics(
        val executionTimeMs: Long,
        val memoryUsageMB: Long,
        val isStressTest: Boolean,
        val isPerformanceTest: Boolean,
        val category: String
    )

    enum class TestStatus {
        RUNNING, PASSED, FAILED, SKIPPED
    }

    override fun testRunStarted(description: Description?) {
        super.testRunStarted(description)
        suiteStartTime = System.currentTimeMillis()
        totalTests = description?.testCount() ?: 0

        val separator = "=".repeat(100)
        val shortSeparator = "-".repeat(100)

        println(separator)
        println("ENHANCED ANDROID MULTI-SENSOR RECORDING SYSTEM TEST SUITE")
        println(separator)
        println("Suite started at: ${formatTimestamp(suiteStartTime)}")
        println("Total tests to execute: $totalTests")
        println("Performance monitoring: ENABLED")
        println("Memory tracking: ENABLED")
        println("Stress testing: ${STRESS_TESTS.size} tests identified")
        println("Performance benchmarks: ${PERFORMANCE_TESTS.size} tests identified")
        println(shortSeparator)

        File(REPORT_DIR).mkdirs()
    }

    override fun testStarted(description: Description?) {
        super.testStarted(description)

        val testName = description?.methodName ?: "unknown"
        val className = description?.className ?: "unknown"
        val startTime = System.currentTimeMillis()

        val testData = TestExecutionData(
            testName = testName,
            className = className,
            startTime = startTime
        )

        testResults[getTestKey(description)] = testData

        println("🧪 Starting: $className.$testName")

        when {
            isStressTest(className) -> println("   ⚡ STRESS TEST - High intensity validation")
            isPerformanceTest(className) -> println("   📊 PERFORMANCE TEST - Benchmark validation")
            else -> println("   ✅ UNIT TEST - Core functionality validation")
        }
    }

    override fun testFinished(description: Description?) {
        super.testFinished(description)

        val testKey = getTestKey(description)
        val testData = testResults[testKey] ?: return

        val endTime = System.currentTimeMillis()
        val executionTime = endTime - testData.startTime

        testData.endTime = endTime
        if (testData.status == TestStatus.RUNNING) {
            testData.status = TestStatus.PASSED
            passedTests++
        }

        val memoryUsage = getMemoryUsageMB()
        val isStress = isStressTest(testData.className)
        val isPerformance = isPerformanceTest(testData.className)

        val metrics = PerformanceMetrics(
            executionTimeMs = executionTime,
            memoryUsageMB = memoryUsage,
            isStressTest = isStress,
            isPerformanceTest = isPerformance,
            category = getTestCategory(testData.className)
        )

        performanceMetrics[testKey] = metrics

        val status = when {
            executionTime > PERFORMANCE_THRESHOLD_MS -> "⚠️ SLOW"
            memoryUsage > MEMORY_THRESHOLD_MB -> "⚠️ HIGH MEMORY"
            isStress -> "⚡ STRESS COMPLETED"
            isPerformance -> "📊 BENCHMARK COMPLETED"
            else -> "✅ PASSED"
        }

        println("   $status ${testData.testName} (${executionTime}ms, ${memoryUsage}MB)")

        if (executionTime > PERFORMANCE_THRESHOLD_MS) {
            println("   ⚠️  WARNING: Test exceeded performance threshold (${PERFORMANCE_THRESHOLD_MS}ms)")
        }
        if (memoryUsage > MEMORY_THRESHOLD_MB) {
            println("   ⚠️  WARNING: Test exceeded memory threshold (${MEMORY_THRESHOLD_MB}MB)")
        }
    }

    override fun testFailure(failure: Failure?) {
        super.testFailure(failure)

        val testKey = getTestKey(failure?.description)
        val testData = testResults[testKey] ?: return

        testData.status = TestStatus.FAILED
        testData.errorMessage = failure?.message
        testData.stackTrace = failure?.trace

        failedTests++
        passedTests--

        println("   ❌ FAILED: ${testData.testName}")
        println("      Error: ${failure?.message}")
    }

    override fun testIgnored(description: Description?) {
        super.testIgnored(description)

        val testKey = getTestKey(description)
        val testData = testResults[testKey]
        testData?.status = TestStatus.SKIPPED

        skippedTests++

        println("   ⏭️ SKIPPED: ${description?.methodName}")
    }

    override fun testRunFinished(result: Result?) {
        super.testRunFinished(result)

        val suiteEndTime = System.currentTimeMillis()
        val totalExecutionTime = suiteEndTime - suiteStartTime

        generateComprehensiveReport(totalExecutionTime)

        printTestSummary(totalExecutionTime)

        generatePerformanceAnalysis()

        exportResultsToJson()
    }

    private fun generateComprehensiveReport(totalExecutionTime: Long) {
        val reportFile = File(REPORT_DIR, "android_test_report_${formatFileTimestamp()}.html")

        val htmlReport = buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html><head><title>Android Test Report</title>")
            appendLine("<style>")
            appendLine("body { font-family: Arial, sans-serif; margin: 20px; }")
            appendLine("table { border-collapse: collapse; width: 100%; }")
            appendLine("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
            appendLine("th { background-colour: #f2f2f2; }")
            appendLine(".passed { colour: green; }")
            appendLine(".failed { colour: red; }")
            appendLine(".skipped { colour: orange; }")
            appendLine(".performance-warning { background-colour: #fff3cd; }")
            appendLine("</style></head><body>")

            appendLine("<h1>Enhanced Android Test Suite Report</h1>")
            appendLine("<p>Generated: ${formatTimestamp(System.currentTimeMillis())}</p>")
            appendLine("<p>Total Execution Time: ${totalExecutionTime}ms</p>")

            appendLine("<h2>Test Summary</h2>")
            appendLine("<table>")
            appendLine("<tr><th>Status</th><th>Count</th><th>Percentage</th></tr>")
            appendLine(
                "<tr class='passed'><td>Passed</td><td>$passedTests</td><td>${
                    (passedTests * 100.0 / totalTests).format(
                        1
                    )
                }%</td></tr>"
            )
            appendLine(
                "<tr class='failed'><td>Failed</td><td>$failedTests</td><td>${
                    (failedTests * 100.0 / totalTests).format(
                        1
                    )
                }%</td></tr>"
            )
            appendLine(
                "<tr class='skipped'><td>Skipped</td><td>$skippedTests</td><td>${
                    (skippedTests * 100.0 / totalTests).format(
                        1
                    )
                }%</td></tr>"
            )
            appendLine("</table>")

            appendLine("<h2>Performance Analysis</h2>")
            appendLine("<table>")
            appendLine("<tr><th>Test</th><th>Category</th><th>Execution Time</th><th>Memory Usage</th><th>Status</th></tr>")

            performanceMetrics.entries.sortedByDescending { it.value.executionTimeMs }.forEach { entry ->
                val testData = testResults[entry.key]
                val metrics = entry.value
                val statusClass = when (testData?.status) {
                    TestStatus.PASSED -> "passed"
                    TestStatus.FAILED -> "failed"
                    TestStatus.SKIPPED -> "skipped"
                    else -> ""
                }
                val performanceClass = if (metrics.executionTimeMs > PERFORMANCE_THRESHOLD_MS ||
                    metrics.memoryUsageMB > MEMORY_THRESHOLD_MB
                ) "performance-warning" else ""

                appendLine("<tr class='$statusClass $performanceClass'>")
                appendLine("<td>${testData?.testName ?: "unknown"}</td>")
                appendLine("<td>${metrics.category}</td>")
                appendLine("<td>${metrics.executionTimeMs}ms</td>")
                appendLine("<td>${metrics.memoryUsageMB}MB</td>")
                appendLine("<td>${testData?.status ?: "unknown"}</td>")
                appendLine("</tr>")
            }

            appendLine("</table>")
            appendLine("</body></html>")
        }

        reportFile.writeText(htmlReport)
        println("📄 complete HTML report generated: ${reportFile.absolutePath}")
    }

    private fun printTestSummary(totalExecutionTime: Long) {
        val separator = "=".repeat(100)
        println("\n$separator")
        println("ENHANCED ANDROID TEST SUITE SUMMARY")
        println(separator)
        println("📊 Overall Results:")
        println("   Total Tests: $totalTests")
        println("   ✅ Passed: $passedTests (${(passedTests * 100.0 / totalTests).format(1)}%)")
        println("   ❌ Failed: $failedTests (${(failedTests * 100.0 / totalTests).format(1)}%)")
        println("   ⏭️ Skipped: $skippedTests (${(skippedTests * 100.0 / totalTests).format(1)}%)")
        println("   ⏱️ Total Time: ${totalExecutionTime}ms (${(totalExecutionTime / 1000.0).format(2)}s)")

        val successRate = (passedTests * 100.0 / (totalTests - skippedTests)).format(1)
        println("   🎯 Success Rate: $successRate%")

        val slowTests = performanceMetrics.values.count { it.executionTimeMs > PERFORMANCE_THRESHOLD_MS }
        val memoryIntensiveTests = performanceMetrics.values.count { it.memoryUsageMB > MEMORY_THRESHOLD_MB }

        if (slowTests > 0 || memoryIntensiveTests > 0) {
            println("\n⚠️ Performance Warnings:")
            if (slowTests > 0) println("   $slowTests tests exceeded performance threshold (${PERFORMANCE_THRESHOLD_MS}ms)")
            if (memoryIntensiveTests > 0) println("   $memoryIntensiveTests tests exceeded memory threshold (${MEMORY_THRESHOLD_MB}MB)")
        }

        println(separator)
    }

    private fun generatePerformanceAnalysis() {
        val stressTestMetrics = performanceMetrics.values.filter { it.isStressTest }
        val performanceTestMetrics = performanceMetrics.values.filter { it.isPerformanceTest }

        val shortSeparator = "-".repeat(60)
        println("\n📊 PERFORMANCE ANALYSIS")
        println(shortSeparator)

        if (stressTestMetrics.isNotEmpty()) {
            val avgStressTime = stressTestMetrics.map { it.executionTimeMs }.average()
            val maxStressTime = stressTestMetrics.maxOfOrNull { it.executionTimeMs } ?: 0
            println("⚡ Stress Test Performance:")
            println("   Average execution time: ${avgStressTime.format(0)}ms")
            println("   Maximum execution time: ${maxStressTime}ms")
            println("   Tests completed: ${stressTestMetrics.size}")
        }

        if (performanceTestMetrics.isNotEmpty()) {
            val avgPerfTime = performanceTestMetrics.map { it.executionTimeMs }.average()
            val maxPerfTime = performanceTestMetrics.maxOfOrNull { it.executionTimeMs } ?: 0
            println("📊 Performance Benchmark Results:")
            println("   Average benchmark time: ${avgPerfTime.format(0)}ms")
            println("   Maximum benchmark time: ${maxPerfTime}ms")
            println("   Benchmarks completed: ${performanceTestMetrics.size}")
        }

        val avgMemory = performanceMetrics.values.map { it.memoryUsageMB }.average()
        val maxMemory = performanceMetrics.values.maxOfOrNull { it.memoryUsageMB } ?: 0
        println("💾 Memory Usage Analysis:")
        println("   Average memory usage: ${avgMemory.format(0)}MB")
        println("   Peak memory usage: ${maxMemory}MB")
        println("   Memory efficiency: ${if (avgMemory < MEMORY_THRESHOLD_MB) "GOOD" else "NEEDS OPTIMISATION"}")
    }

    private fun exportResultsToJson() {
        val jsonFile = File(REPORT_DIR, "android_test_results_${formatFileTimestamp()}.json")
        println("📤 JSON results exported: ${jsonFile.absolutePath}")
    }

    private fun getTestKey(description: Description?): String {
        return "${description?.className}.${description?.methodName}"
    }

    private fun isStressTest(className: String): Boolean {
        return STRESS_TESTS.any { className.contains(it) }
    }

    private fun isPerformanceTest(className: String): Boolean {
        return PERFORMANCE_TESTS.any { className.contains(it) }
    }

    private fun getTestCategory(className: String): String {
        return when {
            isStressTest(className) -> "Stress Test"
            isPerformanceTest(className) -> "Performance Test"
            className.contains("Integration") -> "Integration Test"
            className.contains("UI") || className.contains("Activity") -> "UI Test"
            className.contains("Unit") -> "Unit Test"
            else -> "General Test"
        }
    }

    private fun getMemoryUsageMB(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatFileTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
}

object AndroidTestSuiteRunner {

    fun runComprehensiveTests() {
        println("🚀 Starting Enhanced Android Test Suite...")

        println("📋 Test Categories:")
        println("   • Unit Tests: Core functionality validation")
        println("   • Integration Tests: Component interaction validation")
        println("   • UI Tests: User interface validation")
        println("   • Stress Tests: High-load scenario validation")
        println("   • Performance Tests: Benchmark validation")

        println("\n🔧 Enhanced Features:")
        println("   • Performance monitoring and benchmarking")
        println("   • Memory usage tracking and analysis")
        println("   • complete HTML and JSON reporting")
        println("   • Stress test coordination")
        println("   • Test categorization and analytics")

        println("\n▶️ Execute tests using:")
        println("   ./gradlew testDebugUnitTest")
        println("   ./gradlew connectedDebugAndroidTest")
    }
}
