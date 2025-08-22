package com.multisensor.recording.util

import android.Manifest
import android.os.Build
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AllAndroidPermissionsTest {
    @Test
    fun `getAllPermissions should return non-empty array`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertNotNull(permissions)
        assertTrue(permissions.isNotEmpty())
        assertTrue(permissions.size > 50)
    }

    @Test
    fun `getAllPermissions should include core permissions`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertTrue(permissions.contains(Manifest.permission.CAMERA))
        assertTrue(permissions.contains(Manifest.permission.RECORD_AUDIO))
        assertTrue(permissions.contains(Manifest.permission.INTERNET))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_NETWORK_STATE))
        assertTrue(permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        assertTrue(permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE))
    }

    @Test
    fun `getAllPermissions should include location permissions`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @Test
    fun `getAllPermissions should include bluetooth permissions`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH))
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_ADMIN))
    }

    @Test
    fun `getAllPermissions should include communication permissions`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertTrue(permissions.contains(Manifest.permission.READ_PHONE_STATE))
        assertTrue(permissions.contains(Manifest.permission.SEND_SMS))
        assertTrue(permissions.contains(Manifest.permission.READ_CONTACTS))
        assertTrue(permissions.contains(Manifest.permission.WRITE_CONTACTS))
        assertTrue(permissions.contains(Manifest.permission.READ_CALENDAR))
        assertTrue(permissions.contains(Manifest.permission.WRITE_CALENDAR))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `getAllPermissions should include API 29+ permissions on Android Q+`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertTrue(permissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.ACTIVITY_RECOGNITION))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `getAllPermissions should include API 31+ permissions on Android S+`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_SCAN))
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_ADVERTISE))
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `getAllPermissions should include API 33+ permissions on Android T+`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertTrue(permissions.contains(Manifest.permission.READ_MEDIA_IMAGES))
        assertTrue(permissions.contains(Manifest.permission.READ_MEDIA_VIDEO))
        assertTrue(permissions.contains(Manifest.permission.READ_MEDIA_AUDIO))
        assertTrue(permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(permissions.contains(Manifest.permission.BODY_SENSORS_BACKGROUND))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    fun `getAllPermissions should not include newer API permissions on older versions`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertFalse(permissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        assertFalse(permissions.contains(Manifest.permission.ACTIVITY_RECOGNITION))

        assertFalse(permissions.contains(Manifest.permission.BLUETOOTH_SCAN))
        assertFalse(permissions.contains(Manifest.permission.BLUETOOTH_ADVERTISE))

        assertFalse(permissions.contains(Manifest.permission.READ_MEDIA_IMAGES))
        assertFalse(permissions.contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    @Test
    fun `getAllPermissions should not contain duplicates`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        val uniquePermissions = permissions.toSet()
        assertEquals(permissions.size, uniquePermissions.size)
    }

    @Test
    fun `getDangerousPermissions should return subset of all permissions`() {
        val allPermissions = AllAndroidPermissions.getAllPermissions()
        val dangerousPermissions = AllAndroidPermissions.getDangerousPermissions()

        assertNotNull(dangerousPermissions)
        assertTrue(dangerousPermissions.isNotEmpty())
        assertTrue(dangerousPermissions.size < allPermissions.size)

        dangerousPermissions.forEach { permission ->
            assertTrue("$permission should be in all permissions", allPermissions.contains(permission))
        }
    }

    @Test
    fun `getDangerousPermissions should include runtime permissions`() {
        val dangerousPermissions = AllAndroidPermissions.getDangerousPermissions()

        assertTrue(dangerousPermissions.contains(Manifest.permission.CAMERA))
        assertTrue(dangerousPermissions.contains(Manifest.permission.RECORD_AUDIO))
        assertTrue(dangerousPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(dangerousPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
        assertTrue(dangerousPermissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE))
        assertTrue(dangerousPermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        assertTrue(dangerousPermissions.contains(Manifest.permission.READ_CONTACTS))
        assertTrue(dangerousPermissions.contains(Manifest.permission.READ_PHONE_STATE))
    }

    @Test
    fun `getDangerousPermissions should not include normal permissions`() {
        val dangerousPermissions = AllAndroidPermissions.getDangerousPermissions()

        assertFalse(dangerousPermissions.contains(Manifest.permission.INTERNET))
        assertFalse(dangerousPermissions.contains(Manifest.permission.ACCESS_NETWORK_STATE))
        assertFalse(dangerousPermissions.contains(Manifest.permission.WAKE_LOCK))
        assertFalse(dangerousPermissions.contains(Manifest.permission.VIBRATE))
    }

    @Test
    fun `getDangerousPermissions should not contain duplicates`() {
        val dangerousPermissions = AllAndroidPermissions.getDangerousPermissions()

        val uniquePermissions = dangerousPermissions.toSet()
        assertEquals(dangerousPermissions.size, uniquePermissions.size)
    }

    @Test
    fun `getPermissionGroupDescriptions should return non-empty map`() {
        val descriptions = AllAndroidPermissions.getPermissionGroupDescriptions()

        assertNotNull(descriptions)
        assertTrue(descriptions.isNotEmpty())
    }

    @Test
    fun `getPermissionGroupDescriptions should include expected categories`() {
        val descriptions = AllAndroidPermissions.getPermissionGroupDescriptions()

        assertTrue(descriptions.containsKey("Location"))
        assertTrue(descriptions.containsKey("Storage"))
        assertTrue(descriptions.containsKey("Camera"))
        assertTrue(descriptions.containsKey("Microphone"))
        assertTrue(descriptions.containsKey("Phone"))
        assertTrue(descriptions.containsKey("SMS"))
        assertTrue(descriptions.containsKey("Contacts"))
        assertTrue(descriptions.containsKey("Calendar"))
        assertTrue(descriptions.containsKey("Sensors"))
        assertTrue(descriptions.containsKey("Bluetooth"))
        assertTrue(descriptions.containsKey("Network"))
        assertTrue(descriptions.containsKey("System"))
        assertTrue(descriptions.containsKey("Media"))
        assertTrue(descriptions.containsKey("Notifications"))
    }

    @Test
    fun `getPermissionGroupDescriptions should have meaningful descriptions`() {
        val descriptions = AllAndroidPermissions.getPermissionGroupDescriptions()

        descriptions.values.forEach { description ->
            assertNotNull(description)
            assertTrue(description.isNotEmpty())
            assertTrue(description.length > 10)
        }
    }

    @Test
    fun `permission arrays should be consistent across calls`() {
        val permissions1 = AllAndroidPermissions.getAllPermissions()
        val permissions2 = AllAndroidPermissions.getAllPermissions()

        assertArrayEquals(permissions1, permissions2)
    }

    @Test
    fun `dangerous permissions should be consistent across calls`() {
        val dangerous1 = AllAndroidPermissions.getDangerousPermissions()
        val dangerous2 = AllAndroidPermissions.getDangerousPermissions()

        assertArrayEquals(dangerous1, dangerous2)
    }

    @Test
    fun `permission group descriptions should be consistent across calls`() {
        val descriptions1 = AllAndroidPermissions.getPermissionGroupDescriptions()
        val descriptions2 = AllAndroidPermissions.getPermissionGroupDescriptions()

        assertEquals(descriptions1, descriptions2)
    }

    @Test
    fun `getAllPermissions should include system permissions`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertTrue(permissions.contains(Manifest.permission.FOREGROUND_SERVICE))
        assertTrue(permissions.contains(Manifest.permission.SYSTEM_ALERT_WINDOW))
        assertTrue(permissions.contains(Manifest.permission.WRITE_SETTINGS))
        assertTrue(permissions.contains(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
    }

    @Test
    fun `getAllPermissions should include sensor permissions`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertTrue(permissions.contains(Manifest.permission.BODY_SENSORS))
    }

    @Test
    fun `getAllPermissions should include device admin permissions`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        assertTrue(permissions.contains(Manifest.permission.BIND_DEVICE_ADMIN))
        assertTrue(permissions.contains(Manifest.permission.BIND_INPUT_METHOD))
        assertTrue(permissions.contains(Manifest.permission.BIND_ACCESSIBILITY_SERVICE))
        assertTrue(permissions.contains(Manifest.permission.BIND_WALLPAPER))
    }

    @Test
    fun `permission count should be reasonable`() {
        val allPermissions = AllAndroidPermissions.getAllPermissions()
        val dangerousPermissions = AllAndroidPermissions.getDangerousPermissions()

        assertTrue("Should have at least 60 total permissions", allPermissions.size >= 60)
        assertTrue("Should have no more than 150 total permissions", allPermissions.size <= 150)

        assertTrue("Should have at least 20 dangerous permissions", dangerousPermissions.size >= 20)
        assertTrue("Should have no more than 50 dangerous permissions", dangerousPermissions.size <= 50)

        val dangerousRatio = dangerousPermissions.size.toDouble() / allPermissions.size
        assertTrue("Dangerous permissions should be 20-60% of total", dangerousRatio >= 0.2 && dangerousRatio <= 0.6)
    }

    @Test
    fun `all permissions should be valid Android permission strings`() {
        val permissions = AllAndroidPermissions.getAllPermissions()

        permissions.forEach { permission ->
            assertNotNull("Permission should not be null", permission)
            assertTrue("Permission should not be empty", permission.isNotEmpty())
            assertTrue(
                "Permission should start with android.permission.",
                permission.startsWith("android.permission."),
            )
            assertFalse("Permission should not contain spaces", permission.contains(" "))
        }
    }

    @Test
    fun `dangerous permissions should be valid Android permission strings`() {
        val permissions = AllAndroidPermissions.getDangerousPermissions()

        permissions.forEach { permission ->
            assertNotNull("Permission should not be null", permission)
            assertTrue("Permission should not be empty", permission.isNotEmpty())
            assertTrue(
                "Permission should start with android.permission.",
                permission.startsWith("android.permission."),
            )
            assertFalse("Permission should not contain spaces", permission.contains(" "))
        }
    }
}
