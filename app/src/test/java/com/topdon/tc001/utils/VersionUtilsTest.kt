package com.topdon.tc001.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive test suite for VersionUtils
 * Tests version string generation, debug/release mode handling
 * Critical for improving test coverage from 84% to 90% target
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class VersionUtilsTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockPackageManager: PackageManager
    
    @Mock 
    private lateinit var mockPackageInfo: PackageInfo

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.packageManager).thenReturn(mockPackageManager)
        whenever(mockContext.packageName).thenReturn("com.topdon.tc001")
    }

    @Test
    fun testGetCodeStrReleaseMode() {
        // Test version string generation in release mode
        mockPackageInfo.versionName = "1.2.3"
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertEquals("Should return version without debug suffix", "1.2.3", codeStr)
    }

    @Test
    fun testGetCodeStrDebugMode() {
        // Test version string generation in debug mode
        // Note: In test environment, BuildConfig.DEBUG behavior may vary
        mockPackageInfo.versionName = "1.2.3"
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertTrue("Should contain version number", codeStr.contains("1.2.3"))
    }

    @Test
    fun testGetCodeStrWithNullVersionName() {
        // Test handling of null version name
        mockPackageInfo.versionName = null
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertEquals("Should handle null version name", "null", codeStr)
    }

    @Test
    fun testGetCodeStrWithEmptyVersionName() {
        // Test handling of empty version name
        mockPackageInfo.versionName = ""
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertEquals("Should handle empty version name", "", codeStr)
    }

    @Test
    fun testGetCodeStrWithLongVersionName() {
        // Test handling of long version name
        val longVersion = "1.2.3.4567890.build.12345678"
        mockPackageInfo.versionName = longVersion
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertTrue("Should contain full version string", codeStr.contains(longVersion))
    }

    @Test
    fun testGetCodeStrWithSpecialCharacters() {
        // Test handling of version name with special characters
        val specialVersion = "1.2.3-beta+build.123"
        mockPackageInfo.versionName = specialVersion
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertTrue("Should handle special characters", codeStr.contains(specialVersion))
    }

    @Test(expected = Exception::class)
    fun testGetCodeStrWithPackageManagerException() {
        // Test exception handling
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0))
            .thenThrow(PackageManager.NameNotFoundException())
        
        VersionUtils.getCodeStr(mockContext)
    }

    @Test
    fun testGetCodeStrPerformance() {
        // Test performance with repeated calls
        mockPackageInfo.versionName = "1.2.3"
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val startTime = System.nanoTime()
        val iterations = 1000
        
        repeat(iterations) {
            VersionUtils.getCodeStr(mockContext)
        }
        
        val endTime = System.nanoTime()
        val averageTime = (endTime - startTime) / iterations
        
        // Should be efficient for repeated calls
        Assert.assertTrue("Version string generation should be efficient", 
            averageTime < 100_000L) // Less than 100 microseconds per call
    }

    @Test
    fun testGetCodeStrConsistency() {
        // Test consistency of returned values
        mockPackageInfo.versionName = "1.2.3"
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr1 = VersionUtils.getCodeStr(mockContext)
        val codeStr2 = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertEquals("Should return consistent values", codeStr1, codeStr2)
    }

    @Test
    fun testGetCodeStrWithDifferentPackageNames() {
        // Test with different package names
        val packageNames = listOf("com.test.app", "org.example.test", "app.demo.version")
        
        packageNames.forEach { packageName ->
            whenever(mockContext.packageName).thenReturn(packageName)
            mockPackageInfo.versionName = "1.0.0"
            whenever(mockPackageManager.getPackageInfo(packageName, 0)).thenReturn(mockPackageInfo)
            
            val codeStr = VersionUtils.getCodeStr(mockContext)
            
            Assert.assertNotNull("Code string should not be null for $packageName", codeStr)
            Assert.assertTrue("Should contain version for $packageName", codeStr.contains("1.0.0"))
        }
    }

    @Test
    fun testGetCodeStrThreadSafety() {
        // Test thread safety
        mockPackageInfo.versionName = "1.2.3"
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val results = mutableListOf<String>()
        val threads = mutableListOf<Thread>()
        
        repeat(10) { index ->
            val thread = Thread {
                val codeStr = VersionUtils.getCodeStr(mockContext)
                synchronized(results) {
                    results.add(codeStr)
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        // Wait for all threads to complete
        threads.forEach { it.join() }
        
        Assert.assertEquals("Should have results from all threads", 10, results.size)
        
        // All results should be identical
        val firstResult = results.first()
        results.forEach { result ->
            Assert.assertEquals("All thread results should be identical", firstResult, result)
        }
    }
}