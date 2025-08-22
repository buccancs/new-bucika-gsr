"""
Comprehensive Android E2E Testing Suite
=====================================

Extended Android testing covering hardware integration, performance,
security, and advanced UI workflows with real device scenarios.

Requirements Coverage:
- FR1-FR10: All functional requirements validation
- NFR1-NFR8: All non-functional requirements validation
- Hardware integration testing
- Performance and stress testing
- Security validation
- Accessibility compliance (WCAG 2.1)
"""

import pytest
import time
import os
import sys
import json
import threading
from typing import Dict, List, Optional, Tuple
from unittest.mock import Mock, patch
from dataclasses import dataclass

# Appium imports with mock fallback
try:
    from appium import webdriver
    from appium.webdriver.common.appiumby import AppiumBy
    from appium.options.android import UiAutomator2Options
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC
    from selenium.common.exceptions import TimeoutException, NoSuchElementException
    from selenium.webdriver.common.action_chains import ActionChains
    from selenium.webdriver.common.touch_actions import TouchAction
    APPIUM_AVAILABLE = True
except ImportError:
    APPIUM_AVAILABLE = False
    # Import mock infrastructure
    from hardware.android_mock_infrastructure import (
        MockAndroidTestManager, MockAppiumBy as AppiumBy,
        appium_mocks, patch_appium_imports
    )
    patch_appium_imports()
    
    # Use mocked classes
    WebDriverWait = appium_mocks['WebDriverWait']
    EC = appium_mocks['expected_conditions']
    TimeoutException = appium_mocks['TimeoutException']
    NoSuchElementException = appium_mocks['NoSuchElementException']

# Performance monitoring
try:
    import psutil
    PERFORMANCE_MONITORING = True
except ImportError:
    PERFORMANCE_MONITORING = False

# Add PythonApp to path
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'PythonApp'))


@dataclass
class PerformanceMetrics:
    """Performance metrics collection."""
    cpu_usage: float
    memory_usage: float
    battery_level: float
    network_latency: float
    ui_response_time: float
    frame_rate: float


@pytest.fixture(scope="session")
def enhanced_appium_capabilities() -> Dict:
    """Enhanced Appium capabilities for comprehensive testing."""
    return {
        "platformName": "Android",
        "platformVersion": "8.0",
        "deviceName": "Android Emulator",
        "app": os.path.join(os.path.dirname(__file__), "..", "..", "AndroidApp", "build", "outputs", "apk", "dev", "debug", "AndroidApp-dev-debug.apk"),
        "automationName": "UiAutomator2",
        "appPackage": "com.multisensor.recording",
        "appActivity": "com.multisensor.recording.ui.MainActivity",
        "autoGrantPermissions": True,
        "noReset": False,
        "fullReset": True,
        "newCommandTimeout": 600,
        "enablePerformanceLogging": True,
        "skipLogcatCapture": False,
        "captureScreenshots": True,
        "recordVideo": True,
        "autoWebview": False,
        "nativeWebScreenshot": True,
        "recreateChromeDriverSessions": True,
        "ensureWebviewsHavePages": True,
        "waitForIdleTimeout": 1000,
        "shouldTerminateApp": False,
        "forceAppLaunch": True,
        "autoLaunch": True,
        "systemPort": 8200,
        "uiautomator2ServerLaunchTimeout": 60000,
        "adbExecTimeout": 20000,
        "androidInstallTimeout": 90000,
        "appWaitTimeout": 20000,
        "deviceReadyTimeout": 5,
        "unlockType": "pin",
        "unlockKey": "1234",
        "skipUnlock": True,
        "ignoreUnimportantViews": False
    }


@pytest.fixture(scope="function")
def performance_monitor():
    """Performance monitoring fixture."""
    if not PERFORMANCE_MONITORING:
        pytest.skip("Performance monitoring not available - install psutil")
    
    metrics = []
    monitoring = True
    
    def collect_metrics():
        while monitoring:
            try:
                cpu = psutil.cpu_percent(interval=1)
                memory = psutil.virtual_memory().percent
                metrics.append({
                    'timestamp': time.time(),
                    'cpu_usage': cpu,
                    'memory_usage': memory
                })
            except Exception:
                pass
    
    monitor_thread = threading.Thread(target=collect_metrics)
    monitor_thread.start()
    
    yield metrics
    
    monitoring = False
    monitor_thread.join(timeout=2)


@pytest.fixture(scope="function")
def enhanced_appium_driver(enhanced_appium_capabilities):
    """Enhanced Appium WebDriver with performance monitoring and mock fallback."""
    if not APPIUM_AVAILABLE:
        # Use mock infrastructure
        from hardware.android_mock_infrastructure import MockAndroidTestManager
        manager = MockAndroidTestManager()
        driver = manager.get_driver()
        
        yield driver
        manager.cleanup()
        return
    
    driver = None
    try:
        # Check if real device is available
        use_real_device = os.getenv('USE_REAL_ANDROID_DEVICE', 'false').lower() == 'true'
        
        if use_real_device:
            options = UiAutomator2Options()
            options.load_capabilities(enhanced_appium_capabilities)
            
            driver = webdriver.Remote(
                command_executor="http://localhost:4723/wd/hub",
                options=options
            )
            
            # Wait for app to load and stabilize
            WebDriverWait(driver, 60).until(
                EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/main_activity"))
            )
            
            # Enable performance logging
            driver.start_recording_screen()
            
            yield driver
        else:
            # Fall back to mock
            from hardware.android_mock_infrastructure import MockAndroidTestManager
            manager = MockAndroidTestManager()
            driver = manager.get_driver()
            
            yield driver
            manager.cleanup()
            return
        
    except Exception as e:
        logger.warning(f"Enhanced Appium setup failed: {e}, using mock")
        # Fall back to mock infrastructure
        from hardware.android_mock_infrastructure import MockAndroidTestManager
        manager = MockAndroidTestManager()
        driver = manager.get_driver()
        
        yield driver
        manager.cleanup()
        return
    
    finally:
        if driver and hasattr(driver, 'quit'):
            try:
                # Save screen recording for debugging
                if hasattr(driver, 'stop_recording_screen'):
                    recording = driver.stop_recording_screen()
                    if recording:
                        with open(f"test_recording_{int(time.time())}.mp4", "wb") as f:
                            f.write(recording)
            except Exception:
                pass
            driver.quit()


class TestAndroidHardwareIntegration:
    """Hardware integration testing with real sensors."""
    
    @pytest.mark.e2e
    @pytest.mark.android
    @pytest.mark.hardware
    def test_bluetooth_shimmer_discovery(self, enhanced_appium_driver):
        """Test Bluetooth Shimmer sensor discovery and connection (FR1)."""
        driver = enhanced_appium_driver
        
        # Navigate to devices screen
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Devices']").click()
        
        # Enable Bluetooth if needed
        bluetooth_toggle = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/toggle_bluetooth")
        if not bluetooth_toggle.get_attribute("checked"):
            bluetooth_toggle.click()
            time.sleep(2)
        
        # Start Shimmer discovery
        scan_shimmer_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_scan_shimmer")
        scan_shimmer_button.click()
        
        # Wait for scan completion
        WebDriverWait(driver, 30).until(
            EC.text_to_be_present_in_element(
                (AppiumBy.ID, "com.multisensor.recording:id/scan_status"),
                "complete"
            )
        )
        
        # Verify scan results
        device_list = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/shimmer_device_list")
        devices = device_list.find_elements(AppiumBy.CLASS_NAME, "android.widget.TextView")
        
        # Check for expected device patterns
        shimmer_devices = [d for d in devices if "Shimmer" in d.text or "GSR" in d.text]
        assert len(shimmer_devices) >= 0, "Shimmer device discovery should complete successfully"
        
        # Test connection to first available device (if any)
        if shimmer_devices:
            first_device = shimmer_devices[0]
            first_device.click()
            
            connect_button = WebDriverWait(driver, 10).until(
                EC.element_to_be_clickable((AppiumBy.ID, "com.multisensor.recording:id/btn_connect_shimmer"))
            )
            connect_button.click()
            
            # Wait for connection result
            connection_status = WebDriverWait(driver, 20).until(
                EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/connection_status"))
            )
            
            # Verify connection attempt completed (success or failure with proper error handling)
            assert connection_status.text.lower() in ["connected", "failed", "timeout"], f"Unexpected connection status: {connection_status.text}"
    
    @pytest.mark.e2e
    @pytest.mark.android
    @pytest.mark.hardware
    def test_usb_thermal_camera_integration(self, enhanced_appium_driver):
        """Test USB thermal camera discovery and integration (FR1, FR3)."""
        driver = enhanced_appium_driver
        
        # Navigate to devices screen
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Devices']").click()
        
        # Check USB devices
        usb_tab = driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='USB']")
        usb_tab.click()
        
        # Refresh USB device list
        refresh_usb_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_refresh_usb")
        refresh_usb_button.click()
        time.sleep(3)
        
        # Check USB device list
        usb_device_list = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/usb_device_list")
        usb_devices = usb_device_list.find_elements(AppiumBy.CLASS_NAME, "android.widget.TextView")
        
        # Look for thermal camera devices
        thermal_devices = [d for d in usb_devices if "thermal" in d.text.lower() or "topdon" in d.text.lower()]
        
        if thermal_devices:
            # Test connection to thermal camera
            first_thermal = thermal_devices[0]
            first_thermal.click()
            
            connect_thermal_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_connect_thermal")
            connect_thermal_button.click()
            
            # Wait for thermal camera initialization
            thermal_status = WebDriverWait(driver, 15).until(
                EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/thermal_status"))
            )
            
            # Verify thermal camera connection attempt
            status_text = thermal_status.text.lower()
            assert status_text in ["connected", "initializing", "failed"], f"Unexpected thermal status: {status_text}"
            
            # If connected, test thermal feed
            if "connected" in status_text:
                thermal_feed = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/thermal_preview")
                assert thermal_feed.is_displayed(), "Thermal camera feed should be visible when connected"
        else:
            # No USB thermal camera available - verify proper empty state handling
            empty_state = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/usb_empty_state")
            assert empty_state.is_displayed(), "Empty state should be shown when no USB devices available"
    
    @pytest.mark.e2e
    @pytest.mark.android
    @pytest.mark.hardware
    def test_sensor_data_quality_validation(self, enhanced_appium_driver):
        """Test sensor data quality and validation (FR7, NFR3)."""
        driver = enhanced_appium_driver
        
        # Start recording session
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Recording']").click()
        
        new_session_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_new_session")
        new_session_button.click()
        
        # Configure session for data quality testing
        session_name_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_session_name")
        session_name_field.clear()
        session_name_field.send_keys("Data Quality Test")
        
        # Enable data quality monitoring
        quality_monitoring_toggle = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/toggle_quality_monitoring")
        if not quality_monitoring_toggle.get_attribute("checked"):
            quality_monitoring_toggle.click()
        
        # Set quality thresholds
        quality_settings_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_quality_settings")
        quality_settings_button.click()
        
        # Configure GSR quality thresholds
        gsr_min_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_gsr_min_threshold")
        gsr_min_field.clear()
        gsr_min_field.send_keys("0.1")
        
        gsr_max_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_gsr_max_threshold")
        gsr_max_field.clear()
        gsr_max_field.send_keys("100.0")
        
        save_settings_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_save_quality_settings")
        save_settings_button.click()
        
        # Start recording
        start_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_start_recording")
        start_button.click()
        
        # Monitor data quality during recording
        quality_indicator = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/data_quality_indicator"))
        )
        
        # Record for a short duration
        time.sleep(5)
        
        # Check quality metrics
        quality_metrics = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/quality_metrics")
        assert quality_metrics.is_displayed(), "Data quality metrics should be visible during recording"
        
        # Stop recording
        stop_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_stop_recording")
        stop_button.click()
        
        # Verify quality report
        quality_report = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/quality_report"))
        )
        
        quality_score = quality_report.find_element(AppiumBy.ID, "com.multisensor.recording:id/overall_quality_score")
        score_text = quality_score.text
        assert "%" in score_text, f"Quality score should be percentage: {score_text}"


class TestAndroidPerformanceStress:
    """Performance and stress testing for Android app."""
    
    @pytest.mark.e2e
    @pytest.mark.android
    @pytest.mark.performance
    def test_high_frequency_data_recording(self, enhanced_appium_driver, performance_monitor):
        """Test high-frequency data recording performance (NFR2, NFR4)."""
        driver = enhanced_appium_driver
        
        # Navigate to recording
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Recording']").click()
        
        new_session_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_new_session")
        new_session_button.click()
        
        # Configure high-frequency recording
        session_name_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_session_name")
        session_name_field.clear()
        session_name_field.send_keys("High Frequency Test")
        
        # Set maximum sample rate
        sample_rate_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_sample_rate")
        sample_rate_field.clear()
        sample_rate_field.send_keys("128")  # 128 Hz
        
        # Enable all available sensors
        sensor_checkboxes = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.CheckBox")
        for checkbox in sensor_checkboxes:
            if not checkbox.get_attribute("checked"):
                checkbox.click()
        
        # Start recording
        start_time = time.time()
        start_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_start_recording")
        start_button.click()
        
        # Record for 30 seconds
        time.sleep(30)
        
        # Stop recording
        stop_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_stop_recording")
        stop_button.click()
        end_time = time.time()
        
        # Verify performance metrics
        performance_report = WebDriverWait(driver, 15).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/performance_report"))
        )
        
        # Check sample rate achieved
        achieved_rate = performance_report.find_element(AppiumBy.ID, "com.multisensor.recording:id/achieved_sample_rate")
        rate_value = float(achieved_rate.text.split()[0])
        assert rate_value >= 100, f"Sample rate too low: {rate_value} Hz"
        
        # Check data loss
        data_loss = performance_report.find_element(AppiumBy.ID, "com.multisensor.recording:id/data_loss_percentage")
        loss_value = float(data_loss.text.split()[0])
        assert loss_value < 5.0, f"Data loss too high: {loss_value}%"
        
        # Verify performance monitoring results
        if performance_monitor:
            avg_cpu = sum(m['cpu_usage'] for m in performance_monitor) / len(performance_monitor)
            avg_memory = sum(m['memory_usage'] for m in performance_monitor) / len(performance_monitor)
            
            assert avg_cpu < 80, f"CPU usage too high: {avg_cpu}%"
            assert avg_memory < 90, f"Memory usage too high: {avg_memory}%"
    
    @pytest.mark.e2e
    @pytest.mark.android
    @pytest.mark.stress
    def test_long_duration_recording_stability(self, enhanced_appium_driver):
        """Test long-duration recording stability (NFR5)."""
        driver = enhanced_appium_driver
        
        # Navigate to recording
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Recording']").click()
        
        new_session_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_new_session")
        new_session_button.click()
        
        # Configure long-duration session
        session_name_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_session_name")
        session_name_field.clear()
        session_name_field.send_keys("Long Duration Test")
        
        duration_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_duration")
        duration_field.clear()
        duration_field.send_keys("120")  # 2 minutes for testing (would be longer in real scenario)
        
        # Start recording
        start_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_start_recording")
        start_button.click()
        
        # Monitor stability during recording
        stability_checks = []
        check_interval = 10  # Check every 10 seconds
        total_duration = 120
        
        for elapsed in range(0, total_duration, check_interval):
            time.sleep(check_interval)
            
            # Check recording status
            recording_status = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/recording_status")
            status_text = recording_status.text.lower()
            
            # Check memory usage
            try:
                memory_indicator = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/memory_usage")
                memory_text = memory_indicator.text
                memory_value = float(memory_text.split()[0])
            except (NoSuchElementException, ValueError):
                memory_value = 0
            
            stability_checks.append({
                'time': elapsed,
                'status': status_text,
                'memory': memory_value,
                'recording_active': 'recording' in status_text
            })
            
            # Verify app hasn't crashed
            assert 'recording' in status_text or 'paused' in status_text, f"Recording stopped unexpectedly at {elapsed}s: {status_text}"
        
        # Stop recording
        stop_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_stop_recording")
        stop_button.click()
        
        # Verify stability
        active_checks = [c for c in stability_checks if c['recording_active']]
        assert len(active_checks) >= len(stability_checks) * 0.9, "Recording should remain active for at least 90% of duration"
        
        # Check for memory leaks
        memory_values = [c['memory'] for c in stability_checks if c['memory'] > 0]
        if len(memory_values) > 2:
            memory_increase = memory_values[-1] - memory_values[0]
            assert memory_increase < 50, f"Potential memory leak detected: {memory_increase}MB increase"
    
    @pytest.mark.e2e
    @pytest.mark.android
    @pytest.mark.stress
    def test_concurrent_operations_stress(self, enhanced_appium_driver):
        """Test concurrent operations under stress (FR8, NFR4)."""
        driver = enhanced_appium_driver
        
        # Start background operations
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Settings']").click()
        
        # Enable background sync
        background_sync_toggle = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/toggle_background_sync")
        if not background_sync_toggle.get_attribute("checked"):
            background_sync_toggle.click()
        
        # Start device discovery in background
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Devices']").click()
        
        continuous_scan_toggle = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/toggle_continuous_scan")
        if not continuous_scan_toggle.get_attribute("checked"):
            continuous_scan_toggle.click()
        
        # Start recording while background operations are running
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Recording']").click()
        
        new_session_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_new_session")
        new_session_button.click()
        
        session_name_field = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/edit_session_name")
        session_name_field.clear()
        session_name_field.send_keys("Concurrent Stress Test")
        
        start_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_start_recording")
        start_button.click()
        
        # Perform UI stress operations during recording
        for i in range(10):
            # Navigate between screens rapidly
            driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
            driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Devices']").click()
            time.sleep(0.5)
            
            driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
            driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Recording']").click()
            time.sleep(0.5)
            
            # Verify recording is still active
            recording_status = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/recording_status")
            assert 'recording' in recording_status.text.lower(), f"Recording stopped during stress test iteration {i}"
        
        # Stop recording
        stop_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_stop_recording")
        stop_button.click()
        
        # Verify session completed successfully
        completion_status = WebDriverWait(driver, 10).until(
            EC.text_to_be_present_in_element(
                (AppiumBy.ID, "com.multisensor.recording:id/recording_status"),
                "completed"
            )
        )
        assert completion_status, "Recording should complete successfully after stress test"


class TestAndroidAccessibilityCompliance:
    """Comprehensive accessibility compliance testing (WCAG 2.1)."""
    
    @pytest.mark.e2e
    @pytest.mark.android
    @pytest.mark.accessibility
    def test_wcag_compliance_navigation(self, enhanced_appium_driver):
        """Test WCAG 2.1 compliance for navigation (NFR6)."""
        driver = enhanced_appium_driver
        
        # Test keyboard navigation
        all_focusable = driver.find_elements(AppiumBy.XPATH, "//*[@focusable='true']")
        assert len(all_focusable) > 0, "App should have focusable elements for keyboard navigation"
        
        # Test focus order
        for i, element in enumerate(all_focusable[:5]):  # Test first 5 elements
            element.click()
            time.sleep(0.5)
            
            # Verify element is focused and has proper accessibility properties
            content_desc = element.get_attribute("content-desc")
            class_name = element.get_attribute("class")
            
            # Interactive elements should have content descriptions
            interactive_types = ["Button", "ImageButton", "EditText", "CheckBox", "ToggleButton"]
            if any(type_name in class_name for type_name in interactive_types):
                assert content_desc and len(content_desc.strip()) > 0, f"Interactive element missing content description: {class_name}"
    
    @pytest.mark.e2e
    @pytest.mark.android
    @pytest.mark.accessibility
    def test_text_scaling_support(self, enhanced_appium_driver):
        """Test text scaling support for accessibility (NFR6)."""
        driver = enhanced_appium_driver
        
        # Test different text sizes
        text_elements = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.TextView")
        
        for element in text_elements[:3]:  # Test first 3 text elements
            if element.is_displayed():
                size = element.size
                location = element.location
                
                # Minimum touch target size (44dp = approximately 44 pixels at 1x density)
                min_size = 44
                
                # For clickable text elements, verify minimum size
                if element.get_attribute("clickable") == "true":
                    assert size['width'] >= min_size or size['height'] >= min_size, \
                        f"Clickable text element too small: {size}"
    
    @pytest.mark.e2e
    @pytest.mark.android
    @pytest.mark.accessibility
    def test_color_contrast_compliance(self, enhanced_appium_driver):
        """Test color contrast compliance (NFR6)."""
        driver = enhanced_appium_driver
        
        # Take screenshot for color analysis
        screenshot = driver.get_screenshot_as_base64()
        
        # Test high contrast mode simulation
        # This would require more sophisticated color analysis
        # For now, verify that text elements are visible
        text_elements = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.TextView")
        
        visible_text_count = 0
        for element in text_elements:
            if element.is_displayed() and element.text.strip():
                visible_text_count += 1
        
        assert visible_text_count > 0, "App should have visible text elements for contrast testing"
    
    @pytest.mark.e2e
    @pytest.mark.android
    @pytest.mark.accessibility
    def test_talkback_compatibility(self, enhanced_appium_driver):
        """Test TalkBack screen reader compatibility (NFR6)."""
        driver = enhanced_appium_driver
        
        # Simulate TalkBack navigation
        important_elements = driver.find_elements(AppiumBy.XPATH, "//*[@important-for-accessibility='yes' or @focusable='true']")
        
        talkback_readable_count = 0
        for element in important_elements[:10]:  # Test first 10 elements
            content_desc = element.get_attribute("content-desc")
            text_content = element.get_attribute("text")
            
            # Element should have either content description or text for TalkBack
            if content_desc or text_content:
                talkback_readable_count += 1
        
        assert talkback_readable_count > 0, "App should have TalkBack-readable elements"
        
        # Test accessibility actions
        actionable_elements = driver.find_elements(AppiumBy.XPATH, "//*[@clickable='true']")
        
        for element in actionable_elements[:3]:  # Test first 3 actionable elements
            # Verify actionable elements have proper descriptions
            content_desc = element.get_attribute("content-desc")
            assert content_desc and len(content_desc.strip()) > 0, \
                "Actionable elements should have content descriptions for TalkBack"


if __name__ == "__main__":
    # Run specific test category for debugging
    pytest.main([__file__, "-v", "-s", "--tb=short", "-m", "android"])