package com.multisensor.recording.testhelpers

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.*

/**
 * Collects and manages test results for comprehensive reporting.
 * Provides structured logging and file output for test analysis.
 */
class TestResultCollector(private val testSuite: String) {
    
    private val testResults = mutableMapOf<String, Boolean>()
    private val testStartTime = Date().time
    private val tag = "TestResultCollector"

    /**
     * Add a test result with name and success status.
     */
    fun addResult(testName: String, success: Boolean) {
        testResults[testName] = success
        val status = if (success) "✅ PASS" else "❌ FAIL"
        Log.i(tag, "$status - $testName")
    }

    /**
     * Add multiple results from a map.
     */
    fun addResults(results: Map<String, Boolean>) {
        results.forEach { (testName, success) ->
            addResult(testName, success)
        }
    }

    /**
     * Log comprehensive test results summary.
     */
    fun logResults() {
        Log.i(tag, "=".repeat(60))
        Log.i(tag, "$testSuite TEST RESULTS")
        Log.i(tag, "=".repeat(60))

        val totalTests = testResults.size
        val passedTests = testResults.values.count { it }
        val failedTests = totalTests - passedTests
        val successRate = if (totalTests > 0) (passedTests.toDouble() / totalTests * 100) else 0.0

        Log.i(tag, "Total Tests: $totalTests")
        Log.i(tag, "Passed: $passedTests")
        Log.i(tag, "Failed: $failedTests")
        Log.i(tag, "Success Rate: ${String.format("%.1f", successRate)}%")
        Log.i(tag, "")

        Log.i(tag, "DETAILED RESULTS:")
        testResults.toSortedMap().forEach { (testName, result) ->
            val status = if (result) "✅ PASS" else "❌ FAIL"
            Log.i(tag, "$status - $testName")
        }

        Log.i(tag, "=".repeat(60))
        saveResultsToFile()
    }

    /**
     * Save test results to JSON file for analysis.
     */
    private fun saveResultsToFile() {
        try {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val resultsDir = context.getExternalFilesDir("test_results")
            resultsDir?.mkdirs()

            val timestamp = Date().time
            val resultsFile = File(resultsDir, "${testSuite.lowercase()}_test_results_$timestamp.json")

            val resultsJson = buildString {
                append("{\n")
                append("  \"test_suite\": \"$testSuite\",\n")
                append("  \"timestamp\": $timestamp,\n")
                append("  \"execution_time_ms\": ${timestamp - testStartTime},\n")
                append("  \"total_tests\": ${testResults.size},\n")
                append("  \"passed_tests\": ${testResults.values.count { it }},\n")
                append("  \"failed_tests\": ${testResults.values.count { !it }},\n")
                append("  \"success_rate\": ${getSuccessRate()},\n")
                append("  \"results\": {\n")

                testResults.entries.forEachIndexed { index, (testName, result) ->
                    append("    \"$testName\": $result")
                    if (index < testResults.size - 1) append(",")
                    append("\n")
                }

                append("  }\n")
                append("}")
            }

            resultsFile.writeText(resultsJson)
            Log.i(tag, "Test results saved to: ${resultsFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(tag, "Failed to save test results to file", e)
        }
    }

    /**
     * Calculate success rate as a percentage.
     */
    private fun getSuccessRate(): Double {
        return if (testResults.isEmpty()) 0.0 
               else (testResults.values.count { it }.toDouble() / testResults.size * 100)
    }

    /**
     * Get summary statistics.
     */
    fun getSummary(): TestSummary {
        return TestSummary(
            testSuite = testSuite,
            totalTests = testResults.size,
            passedTests = testResults.values.count { it },
            failedTests = testResults.values.count { !it },
            successRate = getSuccessRate()
        )
    }
}

/**
 * Data class for test summary information.
 */
data class TestSummary(
    val testSuite: String,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val successRate: Double
)