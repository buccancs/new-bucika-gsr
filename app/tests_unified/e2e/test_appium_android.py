"""
Appium-based Android End-to-End Tests
=====================================

Provides comprehensive Android UI automation testing using Appium WebDriver.
Tests complete user workflows including device setup, recording sessions,
and cross-platform interactions with Web dashboard and Desktop app.

Requirements Coverage:
- FR1: Device management and discovery
- FR2: Synchronized recording workflows  
- FR4: Session lifecycle management
- FR6: User interface navigation and accessibility
- FR8: Fault tolerance and error handling
- NFR6: Accessibility compliance validation
"""

import pytest
import time
import os
import sys
from typing import Dict, List, Optional
from unittest.mock import Mock, patch

# Appium imports
try:
    from appium import webdriver
    from appium.webdriver.common.appiumby import AppiumBy
    from appium.options.android import UiAutomator2Options
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC
    from selenium.common.exceptions import TimeoutException, NoSuchElementException
    APPIUM_AVAILABLE = True
except ImportError:
    APPIUM_AVAILABLE = False
    webdriver = None
    AppiumBy = None
    UiAutomator2Options = None
    WebDriverWait = None
    EC = None
    TimeoutException = None
    NoSuchElementException = None

# Add PythonApp to path for web dashboard integration
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'PythonApp'))

try:
    from PythonApp.web_ui.web_dashboard import WebDashboardServer
    WEB_AVAILABLE = True
except ImportError:
    WEB_AVAILABLE = False
    WebDashboardServer = None


@pytest.fixture(scope="session")
def appium_capabilities() -> Dict:
    """Appium capabilities for Android testing."""
    return {
        "platformName": "Android",
        "platformVersion": "8.0",  # Minimum supported version
        "deviceName": "Android Emulator",
        "app": os.path.join(os.path.dirname(__file__), "..", "..", "AndroidApp", "build", "outputs", "apk", "debug", "app-debug.apk"),
        "automationName": "UiAutomator2",
        "appPackage": "com.multisensor.recording",
        "appActivity": "com.multisensor.recording.ui.MainActivity",
        "autoGrantPermissions": True,
        "noReset": False,
        "fullReset": True,
        "newCommandTimeout": 300
    }


@pytest.fixture(scope="session")
def web_dashboard():
    """Start Web dashboard server for integration testing."""
    if not WEB_AVAILABLE:
        pytest.skip("Web dashboard not available")
    
    server = WebDashboardServer(port=5000, debug=False)
    server.start()
    time.sleep(2)  # Allow server to start
    
    yield server
    
    server.stop()


@pytest.fixture(scope="function")
def appium_driver(appium_capabilities):
    """Initialize Appium WebDriver for Android testing."""
    if not APPIUM_AVAILABLE:
        pytest.skip("Appium not available - install with: pip install Appium-Python-Client")
    
    # Check if Appium server is running
    try:
        options = UiAutomator2Options()
        options.load_capabilities(appium_capabilities)
        
        driver = webdriver.Remote(
            command_executor="http://localhost:4723/wd/hub",
            options=options
        )
        
        # Wait for app to load
        WebDriverWait(driver, 30).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/main_activity"))
        )
        
        yield driver
        
    except Exception as e:
        pytest.skip(f"Appium server not available or app not found: {e}")
    
    finally:
        if 'driver' in locals():
            driver.quit()


class TestAppiumAndroidE2E:
    """End-to-end tests using Appium for Android UI automation."""
    
    @pytest.mark.e2e
    @pytest.mark.appium
    @pytest.mark.android
    def test_app_launch_and_navigation(self, appium_driver):
        """Test app launch and basic navigation (FR6)."""
        driver = appium_driver
        
        # Verify main activity loaded
        main_activity = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/main_activity")
        assert main_activity.is_displayed()
        
        # Test navigation drawer
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        
        navigation_drawer = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/nav_view"))
        )
        assert navigation_drawer.is_displayed()
        
        # Test navigation to different screens
        screens = ["Recording", "Devices", "Settings", "Help"]
        for screen in screens:
            nav_item = driver.find_element(AppiumBy.XPATH, f"//android.widget.TextView[@text='{screen}']")
            nav_item.click()
            time.sleep(1)  # Allow screen transition
            
            # Verify screen changed
            screen_indicator = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/toolbar_title")
            assert screen.lower() in screen_indicator.text.lower()
    
    @pytest.mark.e2e
    @pytest.mark.appium
    @pytest.mark.android
    def test_device_discovery_workflow(self, appium_driver):
        """Test device discovery and connection workflow (FR1)."""
        driver = appium_driver
        
        # Navigate to Devices screen
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Devices']").click()
        
        # Start device scan
        scan_button = WebDriverWait(driver, 10).until(
            EC.element_to_be_clickable((AppiumBy.ID, "com.multisensor.recording:id/btn_scan_devices"))
        )
        scan_button.click()
        
        # Wait for scan to complete
        WebDriverWait(driver, 15).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/device_list"))
        )
        
        # Verify scan results or empty state
        device_list = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/device_list")
        assert device_list.is_displayed()
        
        # Check for expected device types in scan results
        device_items = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.TextView")
        device_types = [item.text for item in device_items if "Shimmer" in item.text or "Thermal" in item.text]
        
        # Verify scan completed (either devices found or empty state shown)
        scan_status = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/scan_status")
        assert "scanning" not in scan_status.text.lower()
    
    @pytest.mark.e2e
    @pytest.mark.appium
    @pytest.mark.android
    def test_recording_session_workflow(self, appium_driver):
        """Test complete recording session workflow (FR2, FR4)."""
        driver = appium_driver
        
        # Navigate to Recording screen
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Recording']").click()
        
        # Start new session
        new_session_button = WebDriverWait(driver, 10).until(
            EC.element_to_be_clickable((AppiumBy.ID, "com.multisensor.recording:id/btn_new_session"))
        )
        new_session_button.click()
        
        # Configure session
        session_name_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_session_name")
        session_name_field.clear()
        session_name_field.send_keys("E2E Test Session")
        
        # Set recording duration
        duration_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_duration")
        duration_field.clear()
        duration_field.send_keys("5")  # 5 seconds for quick test
        
        # Start recording
        start_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_start_recording")
        start_button.click()
        
        # Verify recording started
        recording_indicator = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/recording_status"))
        )
        assert "recording" in recording_indicator.text.lower()
        
        # Wait for recording to complete
        WebDriverWait(driver, 20).until(
            EC.text_to_be_present_in_element(
                (AppiumBy.ID, "com.multisensor.recording:id/recording_status"), 
                "completed"
            )
        )
        
        # Verify session saved
        session_list = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/session_list")
        session_items = session_list.find_elements(AppiumBy.CLASS_NAME, "android.widget.TextView")
        session_names = [item.text for item in session_items]
        assert any("E2E Test Session" in name for name in session_names)
    
    @pytest.mark.e2e
    @pytest.mark.appium
    @pytest.mark.android
    def test_accessibility_compliance(self, appium_driver):
        """Test accessibility features and compliance (NFR6)."""
        driver = appium_driver
        
        # Check for content descriptions on interactive elements
        interactive_elements = driver.find_elements(AppiumBy.XPATH, "//android.widget.Button | //android.widget.ImageButton")
        
        for element in interactive_elements:
            content_desc = element.get_attribute("content-desc")
            assert content_desc is not None and len(content_desc) > 0, f"Element missing content description: {element}"
        
        # Test TalkBack navigation simulation
        focusable_elements = driver.find_elements(AppiumBy.XPATH, "//*[@focusable='true']")
        assert len(focusable_elements) > 0, "No focusable elements found for accessibility navigation"
        
        # Test text size compatibility
        text_elements = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.TextView")
        for element in text_elements:
            if element.is_displayed():
                text_size = element.size
                assert text_size['height'] >= 44, f"Text element too small for accessibility: {text_size}"
    
    @pytest.mark.e2e
    @pytest.mark.appium
    @pytest.mark.android
    def test_error_handling_and_recovery(self, appium_driver):
        """Test error handling and fault tolerance (FR8)."""
        driver = appium_driver
        
        # Test network disconnection scenario
        driver.set_network_connection(1)  # Airplane mode
        
        # Navigate to a screen requiring network
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Devices']").click()
        
        # Attempt network operation
        scan_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_scan_devices")
        scan_button.click()
        
        # Verify error handling
        error_message = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/error_message"))
        )
        assert "network" in error_message.text.lower() or "connection" in error_message.text.lower()
        
        # Restore network and test recovery
        driver.set_network_connection(6)  # WiFi + Data
        
        retry_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_retry")
        retry_button.click()
        
        # Verify recovery
        WebDriverWait(driver, 15).until(
            EC.invisibility_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/error_message"))
        )


class TestCrossPlatformWorkflows:
    """Cross-platform workflow tests spanning Android, Web, and Desktop."""
    
    @pytest.mark.e2e
    @pytest.mark.appium
    @pytest.mark.network
    @pytest.mark.integration
    def test_android_to_web_dashboard_integration(self, appium_driver, web_dashboard):
        """Test Android app integration with Web dashboard (FR6, FR10)."""
        driver = appium_driver
        
        # Start session on Android
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Recording']").click()
        
        new_session_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_new_session")
        new_session_button.click()
        
        session_name_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_session_name")
        session_name_field.clear()
        session_name_field.send_keys("Cross-Platform Test")
        
        start_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_start_recording")
        start_button.click()
        
        # Verify session appears in web dashboard
        import requests
        time.sleep(2)  # Allow session registration
        
        response = requests.get("http://localhost:5000/api/sessions")
        assert response.status_code == 200
        
        sessions = response.json()
        session_names = [session.get('name', '') for session in sessions]
        assert any("Cross-Platform Test" in name for name in session_names)
        
        # Stop session from Android
        stop_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_stop_recording")
        stop_button.click()
        
        # Verify session completed in web dashboard
        time.sleep(1)
        response = requests.get("http://localhost:5000/api/sessions")
        sessions = response.json()
        
        test_session = next((s for s in sessions if "Cross-Platform Test" in s.get('name', '')), None)
        assert test_session is not None
        assert test_session.get('status') == 'completed'
    
    @pytest.mark.e2e
    @pytest.mark.appium
    @pytest.mark.slow
    def test_multi_device_coordination(self, appium_driver):
        """Test coordination between multiple devices (FR2, FR8)."""
        driver = appium_driver
        
        # This test would require multiple Android devices/emulators
        # For now, we'll test the coordination UI and mock scenarios
        
        # Navigate to multi-device setup
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Settings']").click()
        
        multi_device_toggle = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/toggle_multi_device")
        if not multi_device_toggle.get_attribute("checked"):
            multi_device_toggle.click()
        
        # Test device role assignment
        device_role_spinner = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/spinner_device_role")
        device_role_spinner.click()
        
        # Select coordinator role
        coordinator_option = driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Coordinator']")
        coordinator_option.click()
        
        # Verify role assignment
        selected_role = device_role_spinner.get_attribute("text")
        assert "coordinator" in selected_role.lower()
        
        # Test synchronization setup
        sync_settings_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_sync_settings")
        sync_settings_button.click()
        
        sync_interval_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_sync_interval")
        sync_interval_field.clear()
        sync_interval_field.send_keys("100")  # 100ms sync interval
        
        apply_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_apply_sync")
        apply_button.click()
        
        # Verify synchronization settings applied
        success_message = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/sync_status"))
        )
        assert "configured" in success_message.text.lower()


if __name__ == "__main__":
    # Run specific test for debugging
    pytest.main([__file__, "-v", "-s", "--tb=short"])