package com.multisensor.recording.ui.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.fragment.findNavController
import androidx.test.core.app.ApplicationProvider
import com.multisensor.recording.R
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NavigationUtilsTest {

    private lateinit var mockFragment: Fragment
    private lateinit var mockNavController: NavController
    private lateinit var mockDestination: NavDestination
    private lateinit var mockNavGraph: NavGraph
    private lateinit var context: Context

    @Before
    fun setUp() {
        mockFragment = mockk()
        mockNavController = mockk()
        mockDestination = mockk()
        mockNavGraph = mockk()
        context = ApplicationProvider.getApplicationContext()

        every { mockNavController.currentDestination } returns mockDestination
        every { mockNavController.graph } returns mockNavGraph
        every { mockDestination.id } returns R.id.nav_recording
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `navigateToFragment should navigate when destination is different`() {
        val destinationId = R.id.nav_devices
        every { mockFragment.findNavController() } returns mockNavController
        every { mockNavController.navigate(destinationId) } just Runs

        NavigationUtils.navigateToFragment(mockFragment, destinationId)

        verify { mockNavController.navigate(destinationId) }
    }

    @Test
    fun `navigateToFragment should not navigate when destination is same`() {
        val destinationId = R.id.nav_recording
        every { mockFragment.findNavController() } returns mockNavController

        NavigationUtils.navigateToFragment(mockFragment, destinationId)

        verify(exactly = 0) { mockNavController.navigate(any<Int>()) }
    }

    @Test
    fun `navigateToFragment should handle navigation exceptions gracefully`() {
        val destinationId = R.id.nav_devices
        every { mockFragment.findNavController() } throws RuntimeException("Navigation error")

        try {
            NavigationUtils.navigateToFragment(mockFragment, destinationId)
        } catch (e: Exception) {
            fail("NavigationUtils should handle exceptions gracefully, but threw: ${e.message}")
        }
    }

    @Test
    fun `launchActivity should create correct intent without extras`() {
        val activityClass = TestActivity::class.java
        mockkStatic(Intent::class)
        val mockIntent = mockk<Intent>(relaxed = true)
        every { Intent(context, activityClass) } returns mockIntent
        every { context.startActivity(any()) } just Runs

        NavigationUtils.launchActivity(context, activityClass)

        verify { context.startActivity(mockIntent) }
        verify(exactly = 0) { mockIntent.putExtra(any<String>(), any<String>()) }
    }

    @Test
    fun `launchActivity should add extras when provided`() {
        val activityClass = TestActivity::class.java
        val extras = Bundle().apply {
            putString("key1", "value1")
            putString("key2", "value2")
        }
        mockkStatic(Intent::class)
        val mockIntent = mockk<Intent>(relaxed = true)
        every { Intent(context, activityClass) } returns mockIntent
        every { context.startActivity(any()) } just Runs

        NavigationUtils.launchActivity(context, activityClass, extras)

        verify { context.startActivity(mockIntent) }
        verify { mockIntent.putExtras(extras) }
    }

    @Test
    fun `handleDrawerNavigation should navigate to recording`() {
        every { mockNavController.navigate(R.id.nav_recording) } just Runs

        val result = NavigationUtils.handleDrawerNavigation(mockNavController, R.id.nav_recording)

        assertTrue("Navigation should succeed", result)
        verify { mockNavController.navigate(R.id.nav_recording) }
    }

    @Test
    fun `handleDrawerNavigation should navigate to devices`() {
        every { mockNavController.navigate(R.id.nav_devices) } just Runs

        val result = NavigationUtils.handleDrawerNavigation(mockNavController, R.id.nav_devices)

        assertTrue("Navigation should succeed", result)
        verify { mockNavController.navigate(R.id.nav_devices) }
    }

    @Test
    fun `handleDrawerNavigation should navigate to calibration`() {
        every { mockNavController.navigate(R.id.nav_calibration) } just Runs

        val result = NavigationUtils.handleDrawerNavigation(mockNavController, R.id.nav_calibration)

        assertTrue("Navigation should succeed", result)
        verify { mockNavController.navigate(R.id.nav_calibration) }
    }

    @Test
    fun `handleDrawerNavigation should navigate to files`() {
        every { mockNavController.navigate(R.id.nav_files) } just Runs

        val result = NavigationUtils.handleDrawerNavigation(mockNavController, R.id.nav_files)

        assertTrue("Navigation should succeed", result)
        verify { mockNavController.navigate(R.id.nav_files) }
    }

    @Test
    fun `handleDrawerNavigation should return false for unknown item`() {
        val unknownItemId = 999999

        val result = NavigationUtils.handleDrawerNavigation(mockNavController, unknownItemId)

        assertFalse("Navigation should fail for unknown item", result)
        verify(exactly = 0) { mockNavController.navigate(any<Int>()) }
    }

    @Test
    fun `handleDrawerNavigation should handle navigation exceptions`() {
        every { mockNavController.navigate(any<Int>()) } throws RuntimeException("Navigation error")

        val result = NavigationUtils.handleDrawerNavigation(mockNavController, R.id.nav_recording)

        assertFalse("Navigation should fail gracefully", result)
    }

    @Test
    fun `getCurrentDestinationName should return correct names`() {
        every { mockDestination.id } returns R.id.nav_recording
        assertEquals("Recording", NavigationUtils.getCurrentDestinationName(mockNavController))

        every { mockDestination.id } returns R.id.nav_devices
        assertEquals("Devices", NavigationUtils.getCurrentDestinationName(mockNavController))

        every { mockDestination.id } returns R.id.nav_calibration
        assertEquals("Calibration", NavigationUtils.getCurrentDestinationName(mockNavController))

        every { mockDestination.id } returns R.id.nav_files
        assertEquals("Files", NavigationUtils.getCurrentDestinationName(mockNavController))

        every { mockDestination.id } returns 999999
        assertEquals("Unknown", NavigationUtils.getCurrentDestinationName(mockNavController))
    }

    @Test
    fun `canNavigateToDestination should return true when destination exists and different`() {
        val destinationId = R.id.nav_devices
        every { mockNavGraph.findNode(destinationId) } returns mockDestination
        every { mockDestination.id } returns R.id.nav_recording

        val result = NavigationUtils.canNavigateToDestination(mockNavController, destinationId)

        assertTrue("Should be able to navigate to different destination", result)
    }

    @Test
    fun `canNavigateToDestination should return false when destination is current`() {
        val destinationId = R.id.nav_recording
        every { mockNavGraph.findNode(destinationId) } returns mockDestination
        every { mockDestination.id } returns R.id.nav_recording

        val result = NavigationUtils.canNavigateToDestination(mockNavController, destinationId)

        assertFalse("Should not navigate to same destination", result)
    }

    @Test
    fun `canNavigateToDestination should return false when destination does not exist`() {
        val destinationId = 999999
        every { mockNavGraph.findNode(destinationId) } returns null

        val result = NavigationUtils.canNavigateToDestination(mockNavController, destinationId)

        assertFalse("Should not navigate to non-existent destination", result)
    }

    @Test
    fun `canNavigateToDestination should handle exceptions gracefully`() {
        val destinationId = R.id.nav_devices
        every { mockNavGraph.findNode(destinationId) } throws RuntimeException("Graph error")

        val result = NavigationUtils.canNavigateToDestination(mockNavController, destinationId)

        assertFalse("Should handle exceptions gracefully", result)
    }

    private class TestActivity
}
