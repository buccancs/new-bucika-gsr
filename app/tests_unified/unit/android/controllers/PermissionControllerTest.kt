package com.multisensor.recording.controllers

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.multisensor.recording.managers.PermissionManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PermissionControllerTest {

    private lateinit var permissionController: PermissionController
    private lateinit var mockPermissionManager: PermissionManager
    private lateinit var mockCallback: PermissionController.PermissionCallback
    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockPermissionManager = mockk(relaxed = true)
        mockCallback = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)
        mockActivity = mockk(relaxed = true)
        mockSharedPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.putStringSet(any(), any()) } returns mockEditor
        every { mockEditor.clear() } returns mockEditor
        every { mockEditor.apply() } just Runs

        permissionController = PermissionController(mockPermissionManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `setCallback should initialize state storage when callback is Context`() {
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.getBoolean(any(), any()) } returns false
        every { mockSharedPreferences.getInt(any(), any()) } returns 0
        every { mockSharedPreferences.getLong(any(), any()) } returns 0L

        permissionController.setCallback(mockContext as PermissionController.PermissionCallback)

        verify { mockContext.getSharedPreferences("permission_controller_prefs", Context.MODE_PRIVATE) }
    }

    @Test
    fun `setCallback should not initialize state storage when callback is not Context`() {
        permissionController.setCallback(mockCallback)

        verify(exactly = 0) { mockContext.getSharedPreferences(any(), any()) }
    }

    @Test
    fun `areAllPermissionsGranted should delegate to PermissionManager`() {
        every { mockPermissionManager.areAllPermissionsGranted(mockContext) } returns true

        val result = permissionController.areAllPermissionsGranted(mockContext)

        assertTrue(result)
        verify { mockPermissionManager.areAllPermissionsGranted(mockContext) }
    }

    @Test
    fun `checkPermissions should call onAllPermissionsGranted when permissions are granted`() = runTest {
        every { mockPermissionManager.areAllPermissionsGranted(mockContext) } returns true
        permissionController.setCallback(mockCallback)

        permissionController.checkPermissions(mockContext)

        verify { mockCallback.onPermissionCheckStarted() }
        verify { mockCallback.onAllPermissionsGranted() }
        verify(exactly = 0) { mockPermissionManager.requestPermissions(any(), any()) }
    }

    @Test
    fun `checkPermissions should request permissions when permissions are missing`() = runTest {
        every { mockPermissionManager.areAllPermissionsGranted(mockContext) } returns false
        permissionController.setCallback(mockCallback)

        permissionController.checkPermissions(mockActivity)

        verify { mockCallback.onPermissionCheckStarted() }
        verify { mockCallback.updateStatusText("Requesting permissions...") }
        verify { mockPermissionManager.requestPermissions(mockActivity, any()) }
    }

    @Test
    fun `checkPermissions should not request permissions for non-Activity context`() = runTest {
        every { mockPermissionManager.areAllPermissionsGranted(mockContext) } returns false
        permissionController.setCallback(mockCallback)

        permissionController.checkPermissions(mockContext)

        verify { mockCallback.onPermissionCheckStarted() }
        verify(exactly = 0) { mockPermissionManager.requestPermissions(any(), any()) }
    }

    @Test
    fun `PermissionManager onAllPermissionsGranted should trigger callback`() = runTest {
        every { mockPermissionManager.areAllPermissionsGranted(mockActivity) } returns false
        permissionController.setCallback(mockCallback)

        val callbackSlot = slot<PermissionManager.PermissionCallback>()
        every { mockPermissionManager.requestPermissions(mockActivity, capture(callbackSlot)) } just Runs

        permissionController.checkPermissions(mockActivity)
        callbackSlot.captured.onAllPermissionsGranted()

        verify { mockCallback.onAllPermissionsGranted() }
        verify { mockCallback.onPermissionRequestCompleted() }
    }

    @Test
    fun `PermissionManager onPermissionsTemporarilyDenied should trigger callback`() = runTest {
        val deniedPermissions = listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO")
        every { mockPermissionManager.areAllPermissionsGranted(mockActivity) } returns false
        permissionController.setCallback(mockActivity as PermissionController.PermissionCallback)

        val callbackSlot = slot<PermissionManager.PermissionCallback>()
        every { mockPermissionManager.requestPermissions(mockActivity, capture(callbackSlot)) } just Runs

        permissionController.checkPermissions(mockActivity)
        callbackSlot.captured.onPermissionsTemporarilyDenied(deniedPermissions, 3, 5)

        verify { mockCallback.onPermissionsTemporarilyDenied(deniedPermissions, 3, 5) }
        verify { mockCallback.onPermissionRequestCompleted() }
    }

    @Test
    fun `PermissionManager onPermissionsPermanentlyDenied should trigger callback`() = runTest {
        val deniedPermissions = listOf("android.permission.CAMERA")
        every { mockPermissionManager.areAllPermissionsGranted(mockActivity) } returns false
        permissionController.setCallback(mockActivity as PermissionController.PermissionCallback)

        val callbackSlot = slot<PermissionManager.PermissionCallback>()
        every { mockPermissionManager.requestPermissions(mockActivity, capture(callbackSlot)) } just Runs

        permissionController.checkPermissions(mockActivity)
        callbackSlot.captured.onPermissionsPermanentlyDenied(deniedPermissions)

        verify { mockCallback.onPermissionsPermanentlyDenied(deniedPermissions) }
        verify { mockCallback.onPermissionRequestCompleted() }
    }

    @Test
    fun `requestPermissionsManually should reset state and call checkPermissions`() = runTest {
        every { mockPermissionManager.areAllPermissionsGranted(mockContext) } returns false
        permissionController.setCallback(mockCallback)

        permissionController.requestPermissionsManually(mockContext)

        verify { mockCallback.showPermissionButton(false) }
        verify { mockCallback.updateStatusText("Requesting permissions...") }
        verify { mockCallback.onPermissionCheckStarted() }
    }

    @Test
    fun `requestPermissionsManually should reset retry count to zero`() = runTest {
        every { mockPermissionManager.areAllPermissionsGranted(mockContext) } returns true
        permissionController.setCallback(mockContext as PermissionController.PermissionCallback)

        repeat(3) {
            permissionController.checkPermissions(mockContext)
        }

        permissionController.requestPermissionsManually(mockContext)

        assertEquals(0, permissionController.getPermissionRetryCount())
    }

    @Test
    fun `resetState should clear internal state and persist changes`() {
        permissionController.setCallback(mockContext as PermissionController.PermissionCallback)

        permissionController.resetState()

        assertFalse(permissionController.hasCheckedPermissionsOnStartup())
        assertEquals(0, permissionController.getPermissionRetryCount())
        verify { mockEditor.putBoolean("has_checked_permissions_on_startup", false) }
        verify { mockEditor.putInt("permission_retry_count", 0) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `clearPersistedState should clear SharedPreferences`() {
        permissionController.setCallback(mockContext as PermissionController.PermissionCallback)

        permissionController.clearPersistedState()

        verify { mockEditor.clear() }
        verify { mockEditor.apply() }
        assertFalse(permissionController.hasCheckedPermissionsOnStartup())
        assertEquals(0, permissionController.getPermissionRetryCount())
    }

    @Test
    fun `initializePermissionsOnStartup should check permissions on first call`() = runTest {
        every { mockPermissionManager.areAllPermissionsGranted(mockContext) } returns true
        permissionController.setCallback(mockCallback)

        permissionController.initializePermissionsOnStartup(mockContext)

        verify { mockCallback.onPermissionCheckStarted() }
        verify { mockCallback.onAllPermissionsGranted() }
        assertTrue(permissionController.hasCheckedPermissionsOnStartup())
    }

    @Test
    fun `initializePermissionsOnStartup should skip permissions check on subsequent calls`() = runTest {
        every { mockPermissionManager.areAllPermissionsGranted(mockContext) } returns true
        permissionController.setCallback(mockCallback)

        permissionController.initializePermissionsOnStartup(mockContext)

        clearMocks(mockCallback)

        permissionController.initializePermissionsOnStartup(mockContext)

        verify(exactly = 0) { mockCallback.onPermissionCheckStarted() }
        verify(exactly = 0) { mockCallback.onAllPermissionsGranted() }
    }

    @Test
    fun `updatePermissionButtonVisibility should show button when permissions missing`() {
        every { mockPermissionManager.areAllPermissionsGranted(mockContext) } returns false
        permissionController.setCallback(mockCallback)

        permissionController.updatePermissionButtonVisibility(mockContext)

        verify { mockCallback.showPermissionButton(true) }
    }

    @Test
    fun `updatePermissionButtonVisibility should hide button when all permissions granted`() {
        every { mockPermissionManager.areAllPermissionsGranted(mockContext) } returns true
        permissionController.setCallback(mockCallback)

        permissionController.updatePermissionButtonVisibility(mockContext)

        verify { mockCallback.showPermissionButton(false) }
    }

    @Test
    fun `storePermanentlyDeniedPermissions should save to SharedPreferences`() {
        val deniedPermissions = listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO")
        every { mockPermissionManager.areAllPermissionsGranted(mockActivity) } returns false
        permissionController.setCallback(mockActivity as PermissionController.PermissionCallback)

        val callbackSlot = slot<PermissionManager.PermissionCallback>()
        every { mockPermissionManager.requestPermissions(mockActivity, capture(callbackSlot)) } just Runs

        permissionController.checkPermissions(mockActivity)
        callbackSlot.captured.onPermissionsPermanentlyDenied(deniedPermissions)

        verify { mockEditor.putStringSet("permanently_denied_permissions", deniedPermissions.toSet()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `getPermanentlyDeniedPermissions should return stored permissions`() {
        val deniedPermissions = setOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO")
        every {
            mockSharedPreferences.getStringSet(
                "permanently_denied_permissions",
                emptySet()
            )
        } returns deniedPermissions
        permissionController.setCallback(mockContext as PermissionController.PermissionCallback)

        val result = permissionController.getPermanentlyDeniedPermissions()

        assertEquals(deniedPermissions, result)
    }

    @Test
    fun `getPermanentlyDeniedPermissions should return empty set when no SharedPreferences`() {
        permissionController.setCallback(mockCallback)

        val result = permissionController.getPermanentlyDeniedPermissions()

        assertEquals(emptySet<String>(), result)
    }

    @Test
    fun `state should be loaded from SharedPreferences on initialization`() {
        every { mockSharedPreferences.getBoolean("has_checked_permissions_on_startup", false) } returns true
        every { mockSharedPreferences.getInt("permission_retry_count", 0) } returns 2
        every { mockSharedPreferences.getLong("last_permission_request_time", 0) } returns System.currentTimeMillis()

        permissionController.setCallback(mockContext as PermissionController.PermissionCallback)

        assertTrue(permissionController.hasCheckedPermissionsOnStartup())
        assertEquals(2, permissionController.getPermissionRetryCount())
    }

    @Test
    fun `state should be reset when 24 hours have passed`() {
        val oldTime = System.currentTimeMillis() - (25 * 60 * 60 * 1000)
        every { mockSharedPreferences.getBoolean("has_checked_permissions_on_startup", false) } returns true
        every { mockSharedPreferences.getInt("permission_retry_count", 0) } returns 3
        every { mockSharedPreferences.getLong("last_permission_request_time", 0) } returns oldTime

        permissionController.setCallback(mockContext as PermissionController.PermissionCallback)

        assertFalse(permissionController.hasCheckedPermissionsOnStartup())
        assertEquals(0, permissionController.getPermissionRetryCount())
        verify { mockEditor.putBoolean("has_checked_permissions_on_startup", false) }
        verify { mockEditor.putInt("permission_retry_count", 0) }
    }

    @Test
    fun `getPermissionDisplayName should delegate to PermissionManager`() {
        val permission = "android.permission.CAMERA"
        every { mockPermissionManager.getPermissionDisplayName(permission) } returns "Camera"

        val result = permissionController.getPermissionDisplayName(permission)

        assertEquals("Camera", result)
        verify { mockPermissionManager.getPermissionDisplayName(permission) }
    }

    @Test
    fun `getPermissionStatus should return complete status string`() {
        permissionController.setCallback(mockContext as PermissionController.PermissionCallback)
        every { mockSharedPreferences.getLong("last_permission_request_time", 0) } returns 12345L

        val status = permissionController.getPermissionStatus()

        assertTrue(status.contains("Permission Controller Status:"))
        assertTrue(status.contains("Has checked permissions on startup:"))
        assertTrue(status.contains("Permission retry count:"))
        assertTrue(status.contains("State persistence:"))
        assertTrue(status.contains("Permanently denied permissions:"))
        assertTrue(status.contains("Last request time:"))
    }

    @Test
    fun `logCurrentPermissionStates should delegate to PermissionManager`() {
        permissionController.logCurrentPermissionStates(mockContext)

        verify { mockPermissionManager.logCurrentPermissionStates(mockContext) }
    }

    @Test
    fun `operations should not crash when callback is null`() {
        try {
            permissionController.checkPermissions(mockContext)
            permissionController.updatePermissionButtonVisibility(mockContext)
            permissionController.requestPermissionsManually(mockContext)

        } catch (e: Exception) {
            fail("Operations should not crash when callback is null, but got: ${e.message}")
        }
    }

    @Test
    fun `operations should handle SharedPreferences initialization failure gracefully`() {
        every { mockContext.getSharedPreferences(any(), any()) } throws RuntimeException("Permission denied")

        try {
            permissionController.setCallback(mockContext as PermissionController.PermissionCallback)
            permissionController.resetState()
            permissionController.clearPersistedState()

        } catch (e: Exception) {
            fail("Operations should handle SharedPreferences initialization failure gracefully, but got: ${e.message}")
        }
    }

    @Test
    fun `multiple callbacks should work correctly`() = runTest {
        val mockCallback2 = mockk<PermissionController.PermissionCallback>(relaxed = true)
        every { mockPermissionManager.areAllPermissionsGranted(mockContext) } returns true

        permissionController.setCallback(mockCallback)
        permissionController.checkPermissions(mockContext)

        verify { mockCallback.onAllPermissionsGranted() }

        permissionController.setCallback(mockCallback2)
        permissionController.checkPermissions(mockContext)

        verify { mockCallback2.onAllPermissionsGranted() }
    }

    @Test
    fun `complete permission flow - all granted scenario`() = runTest {
        every { mockPermissionManager.areAllPermissionsGranted(mockActivity) } returns true
        permissionController.setCallback(mockCallback)

        permissionController.initializePermissionsOnStartup(mockActivity)

        verify { mockCallback.onPermissionCheckStarted() }
        verify { mockCallback.onAllPermissionsGranted() }
        verify { mockCallback.showPermissionButton(false) }
        assertTrue(permissionController.hasCheckedPermissionsOnStartup())
    }

    @Test
    fun `complete permission flow - denied then granted scenario`() = runTest {
        every { mockPermissionManager.areAllPermissionsGranted(mockActivity) } returns false andThen true
        permissionController.setCallback(mockCallback)

        val callbackSlot = slot<PermissionManager.PermissionCallback>()
        every { mockPermissionManager.requestPermissions(mockActivity, capture(callbackSlot)) } just Runs

        permissionController.checkPermissions(mockActivity)

        verify { mockCallback.onPermissionCheckStarted() }
        verify { mockCallback.updateStatusText("Requesting permissions...") }

        callbackSlot.captured.onAllPermissionsGranted()

        verify { mockCallback.onAllPermissionsGranted() }
        verify { mockCallback.onPermissionRequestCompleted() }
    }

    @Test
    fun `complete permission flow - permanently denied scenario`() = runTest {
        val deniedPermissions = listOf("android.permission.CAMERA")
        every { mockPermissionManager.areAllPermissionsGranted(mockActivity) } returns false
        permissionController.setCallback(mockActivity as PermissionController.PermissionCallback)

        val callbackSlot = slot<PermissionManager.PermissionCallback>()
        every { mockPermissionManager.requestPermissions(mockActivity, capture(callbackSlot)) } just Runs

        permissionController.checkPermissions(mockActivity)
        callbackSlot.captured.onPermissionsPermanentlyDenied(deniedPermissions)

        verify { mockCallback.onPermissionsPermanentlyDenied(deniedPermissions) }
        verify { mockCallback.onPermissionRequestCompleted() }
        verify { mockEditor.putStringSet("permanently_denied_permissions", deniedPermissions.toSet()) }
        assertEquals(deniedPermissions.toSet(), permissionController.getPermanentlyDeniedPermissions())
    }
}
