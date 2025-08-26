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

        mockPackageInfo.versionName = "1.2.3"
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertEquals("Should return version without debug suffix", "1.2.3", codeStr)
    }

    @Test
    fun testGetCodeStrDebugMode() {

        mockPackageInfo.versionName = "1.2.3"
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertTrue("Should contain version number", codeStr.contains("1.2.3"))
    }

    @Test
    fun testGetCodeStrWithNullVersionName() {

        mockPackageInfo.versionName = null
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertEquals("Should handle null version name", "null", codeStr)
    }

    @Test
    fun testGetCodeStrWithEmptyVersionName() {

        mockPackageInfo.versionName = ""
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertEquals("Should handle empty version name", "", codeStr)
    }

    @Test
    fun testGetCodeStrWithLongVersionName() {

        val longVersion = "1.2.3.4567890.build.12345678"
        mockPackageInfo.versionName = longVersion
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertTrue("Should contain full version string", codeStr.contains(longVersion))
    }

    @Test
    fun testGetCodeStrWithSpecialCharacters() {

        val specialVersion = "1.2.3-beta+build.123"
        mockPackageInfo.versionName = specialVersion
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertNotNull("Code string should not be null", codeStr)
        Assert.assertTrue("Should handle special characters", codeStr.contains(specialVersion))
    }

    @Test(expected = Exception::class)
    fun testGetCodeStrWithPackageManagerException() {

        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0))
            .thenThrow(PackageManager.NameNotFoundException())
        
        VersionUtils.getCodeStr(mockContext)
    }

    @Test
    fun testGetCodeStrPerformance() {

        mockPackageInfo.versionName = "1.2.3"
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val startTime = System.nanoTime()
        val iterations = 1000
        
        repeat(iterations) {
            VersionUtils.getCodeStr(mockContext)
        }
        
        val endTime = System.nanoTime()
        val averageTime = (endTime - startTime) / iterations
        
        Assert.assertTrue("Version string generation should be efficient", 
            averageTime < 100_000L)
    }

    @Test
    fun testGetCodeStrConsistency() {

        mockPackageInfo.versionName = "1.2.3"
        whenever(mockPackageManager.getPackageInfo("com.topdon.tc001", 0)).thenReturn(mockPackageInfo)
        
        val codeStr1 = VersionUtils.getCodeStr(mockContext)
        val codeStr2 = VersionUtils.getCodeStr(mockContext)
        
        Assert.assertEquals("Should return consistent values", codeStr1, codeStr2)
    }

    @Test
    fun testGetCodeStrWithDifferentPackageNames() {

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
        
        threads.forEach { it.join() }
        
        Assert.assertEquals("Should have results from all threads", 10, results.size)
        
        val firstResult = results.first()
        results.forEach { result ->
            Assert.assertEquals("All thread results should be identical", firstResult, result)
        }
    }
