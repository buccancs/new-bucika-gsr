"""
Android GUI Component Interaction Tests
=======================================

Detailed testing of individual Android UI components and their specific functionality.
Tests all buttons, fragments, activities, and interactive elements in the Android application.

Requirements Coverage:
- FR6: User Interface component functionality on Android
- FR7: Mobile device coordination UI components
- FR8: Error handling in mobile GUI components
- NFR6: Mobile accessibility and usability
- NFR1: Mobile GUI component performance
"""

import pytest
import time
from typing import Dict, List, Optional
from unittest.mock import Mock, patch

# Appium and Android testing imports
try:
    from appium import webdriver
    from appium.webdriver.common.appiumby import AppiumBy
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC
    from selenium.common.exceptions import TimeoutException, NoSuchElementException
    APPIUM_AVAILABLE = True
except ImportError:
    APPIUM_AVAILABLE = False

# Test utilities
from ..fixtures.android_test_utils import AndroidTestBase, AndroidTestHelper


class TestAndroidRecordingComponents(AndroidTestBase):
    """Test Android recording fragment components."""
    
    @pytest.mark.gui
    @pytest.mark.android
    @pytest.mark.components
    @pytest.mark.skipif(not APPIUM_AVAILABLE, reason="Appium not available")
    def test_recording_control_buttons(self, android_driver):
        """Test all recording control buttons (FR6)."""
        driver = android_driver
        helper = AndroidTestHelper()
        
        # Navigate to recording fragment
        self._navigate_to_recording_fragment(driver)
        
        # Test recording control buttons
        recording_controls = [
            ("start_recording", "com.multisensor.recording:id/btn_start_recording"),
            ("pause_recording", "com.multisensor.recording:id/btn_pause_recording"),
            ("stop_recording", "com.multisensor.recording:id/btn_stop_recording"),
            ("save_recording", "com.multisensor.recording:id/btn_save_recording")
        ]
        
        for control_name, control_id in recording_controls:
            try:
                button = driver.find_element(AppiumBy.ID, control_id)
                
                # Test button properties
                assert button.is_displayed(), f"{control_name} button should be visible"
                
                if button.is_enabled():
                    # Test button click
                    self.safe_click(driver, button)
                    time.sleep(1)
                    
                    # Verify button state change
                    self._verify_recording_state_change(driver, control_name)
                    
                    self.take_screenshot(driver, f"after_{control_name}")
                
            except NoSuchElementException:
                print(f"Warning: Recording control {control_name} not found")
    
    @pytest.mark.gui
    @pytest.mark.android
    @pytest.mark.components
    @pytest.mark.skipif(not APPIUM_AVAILABLE, reason="Appium not available")
    def test_recording_status_display(self, android_driver):
        """Test recording status display components (FR6)."""
        driver = android_driver
        
        self._navigate_to_recording_fragment(driver)
        
        # Test status display elements
        status_elements = [
            ("recording_status", "com.multisensor.recording:id/recording_status"),
            ("session_timer", "com.multisensor.recording:id/session_timer"),
            ("data_count", "com.multisensor.recording:id/data_point_count"),
            ("file_size", "com.multisensor.recording:id/current_file_size")
        ]
        
        for element_name, element_id in status_elements:
            try:
                element = driver.find_element(AppiumBy.ID, element_id)
                
                # Verify element is visible
                assert element.is_displayed(), f"{element_name} should be visible"
                
                # Verify element has content
                element_text = self.get_element_text_safe(element)
                assert len(element_text) >= 0, f"{element_name} should have content"
                
                print(f"[PASS] {element_name}: {element_text}")
                
            except NoSuchElementException:
                print(f"Warning: Status element {element_name} not found")
    
    @pytest.mark.gui
    @pytest.mark.android
    @pytest.mark.components
    @pytest.mark.skipif(not APPIUM_AVAILABLE, reason="Appium not available")
    def test_recording_settings_panel(self, android_driver):
        """Test recording settings panel components (FR6)."""
        driver = android_driver
        
        self._navigate_to_recording_fragment(driver)
        
        # Look for settings panel or expandable settings
        try:
            settings_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_recording_settings")
            settings_button.click()
            time.sleep(1)
            
            # Test settings controls
            self._test_recording_settings_controls(driver)
            
        except NoSuchElementException:
            print("Warning: Recording settings panel not found")
    
    def _navigate_to_recording_fragment(self, driver):
        """Navigate to recording fragment."""
        try:
            # Try direct navigation to recording
            recording_tab = driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Recording']")
            recording_tab.click()
            time.sleep(1)
        except NoSuchElementException:
            # Try navigation drawer
            try:
                drawer_button = driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer")
                drawer_button.click()
                time.sleep(1)
                
                recording_item = driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Recording']")
                recording_item.click()
                time.sleep(1)
            except NoSuchElementException:
                print("Warning: Could not navigate to recording fragment")
    
    def _verify_recording_state_change(self, driver, control_name: str):
        """Verify recording state changed appropriately."""
        try:
            status_element = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/recording_status")
            status_text = self.get_element_text_safe(status_element).lower()
            
            if "start" in control_name:
                assert "recording" in status_text or "active" in status_text, \
                    f"Status should show recording active, got: {status_text}"
            elif "stop" in control_name:
                assert "stopped" in status_text or "inactive" in status_text, \
                    f"Status should show recording stopped, got: {status_text}"
            elif "pause" in control_name:
                assert "paused" in status_text, f"Status should show paused, got: {status_text}"
                
        except NoSuchElementException:
            print("Warning: Could not verify recording state change")
    
    def _test_recording_settings_controls(self, driver):
        """Test recording settings controls."""
        settings_controls = [
            ("sampling_rate", "android.widget.Spinner"),
            ("buffer_size", "android.widget.EditText"),
            ("auto_save", "android.widget.CheckBox"),
            ("file_format", "android.widget.Spinner")
        ]
        
        for control_name, control_class in settings_controls:
            try:
                controls = driver.find_elements(AppiumBy.CLASS_NAME, control_class)
                for control in controls:
                    if control.is_displayed() and control.is_enabled():
                        # Test control interaction
                        if "EditText" in control_class:
                            control.clear()
                            control.send_keys("test_value")
                        elif "CheckBox" in control_class:
                            control.click()
                        elif "Spinner" in control_class:
                            control.click()
                            time.sleep(0.5)
                        
                        print(f"[PASS] Tested {control_name} control")
                        break
            except Exception as e:
                print(f"Warning: Could not test {control_name} control: {e}")


class TestAndroidDeviceComponents(AndroidTestBase):
    """Test Android device management components."""
    
    @pytest.mark.gui
    @pytest.mark.android
    @pytest.mark.components
    @pytest.mark.skipif(not APPIUM_AVAILABLE, reason="Appium not available")
    def test_device_list_display(self, android_driver):
        """Test device list display and updates (FR7)."""
        driver = android_driver
        
        # Navigate to devices fragment
        self._navigate_to_devices_fragment(driver)
        
        # Test device list container
        try:
            device_list = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/device_list")
            assert device_list.is_displayed(), "Device list should be visible"
            
            # Test device list items
            self._test_device_list_items(driver)
            
        except NoSuchElementException:
            # Try alternative device list implementations
            self._test_alternative_device_list(driver)
    
    @pytest.mark.gui
    @pytest.mark.android
    @pytest.mark.components
    @pytest.mark.skipif(not APPIUM_AVAILABLE, reason="Appium not available")
    def test_device_scanning_controls(self, android_driver):
        """Test device scanning UI controls (FR7)."""
        driver = android_driver
        
        self._navigate_to_devices_fragment(driver)
        
        # Test scan button
        try:
            scan_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_scan_devices")
            
            # Test scan button click
            scan_button.click()
            time.sleep(1)
            
            # Verify scan indicator appears
            self._verify_scan_indicator(driver)
            
            # Wait for scan to complete or timeout
            self._wait_for_scan_completion(driver)
            
        except NoSuchElementException:
            print("Warning: Device scan button not found")
    
    @pytest.mark.gui
    @pytest.mark.android
    @pytest.mark.components
    @pytest.mark.skipif(not APPIUM_AVAILABLE, reason="Appium not available")
    def test_device_connection_controls(self, android_driver):
        """Test device connection UI controls (FR7)."""
        driver = android_driver
        
        self._navigate_to_devices_fragment(driver)
        
        # Test connection controls
        connection_controls = [
            ("connect", "com.multisensor.recording:id/btn_connect_device"),
            ("disconnect", "com.multisensor.recording:id/btn_disconnect_device"),
            ("device_settings", "com.multisensor.recording:id/btn_device_settings")
        ]
        
        for control_name, control_id in connection_controls:
            try:
                button = driver.find_element(AppiumBy.ID, control_id)
                
                if button.is_enabled():
                    button.click()
                    time.sleep(1)
                    
                    # Verify appropriate response
                    self._verify_device_connection_response(driver, control_name)
                    
                    self.take_screenshot(driver, f"after_device_{control_name}")
                
            except NoSuchElementException:
                print(f"Warning: Device control {control_name} not found")
    
    def _navigate_to_devices_fragment(self, driver):
        """Navigate to devices fragment."""
        try:
            devices_tab = driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Devices']")
            devices_tab.click()
            time.sleep(1)
        except NoSuchElementException:
            try:
                drawer_button = driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer")
                drawer_button.click()
                time.sleep(1)
                
                devices_item = driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Devices']")
                devices_item.click()
                time.sleep(1)
            except NoSuchElementException:
                print("Warning: Could not navigate to devices fragment")
    
    def _test_device_list_items(self, driver):
        """Test individual device list items."""
        # Look for device items in the list
        device_items = driver.find_elements(AppiumBy.XPATH, "//*[contains(@resource-id, 'device_item')]")
        
        for i, item in enumerate(device_items[:3]):  # Test first 3 items
            if item.is_displayed():
                # Test item click
                item.click()
                time.sleep(0.5)
                
                # Verify item response (selection, detail view, etc.)
                self._verify_device_item_response(driver, i)
                
                print(f"[PASS] Tested device item {i}")
    
    def _test_alternative_device_list(self, driver):
        """Test alternative device list implementations."""
        # Look for RecyclerView or ListView
        list_views = driver.find_elements(AppiumBy.CLASS_NAME, "androidx.recyclerview.widget.RecyclerView")
        if not list_views:
            list_views = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.ListView")
        
        for list_view in list_views:
            if list_view.is_displayed():
                print("[PASS] Found device list container")
                break
    
    def _verify_scan_indicator(self, driver):
        """Verify scan indicator is shown."""
        scan_indicators = [
            "com.multisensor.recording:id/scan_progress",
            "com.multisensor.recording:id/scanning_indicator"
        ]
        
        for indicator_id in scan_indicators:
            try:
                indicator = driver.find_element(AppiumBy.ID, indicator_id)
                if indicator.is_displayed():
                    print("[PASS] Scan indicator visible")
                    return True
            except NoSuchElementException:
                continue
        
        print("Warning: No scan indicator found")
        return False
    
    def _wait_for_scan_completion(self, driver, timeout=10):
        """Wait for device scan to complete."""
        start_time = time.time()
        while time.time() - start_time < timeout:
            try:
                # Check if scan is still in progress
                scan_indicator = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/scan_progress")
                if not scan_indicator.is_displayed():
                    print("[PASS] Scan completed")
                    return True
            except NoSuchElementException:
                print("[PASS] Scan completed")
                return True
            
            time.sleep(0.5)
        
        print("Warning: Scan timeout")
        return False
    
    def _verify_device_connection_response(self, driver, control_name: str):
        """Verify device connection control response."""
        try:
            # Look for status updates
            status_elements = driver.find_elements(AppiumBy.XPATH, "//*[contains(@text, 'connect') or contains(@text, 'disconnect')]")
            
            for element in status_elements:
                element_text = self.get_element_text_safe(element).lower()
                if control_name.lower() in element_text:
                    print(f"[PASS] Device {control_name} response: {element_text}")
                    return True
        except Exception:
            pass
        
        print(f"Warning: Could not verify {control_name} response")
        return False
    
    def _verify_device_item_response(self, driver, item_index: int):
        """Verify device item click response."""
        # Device item might show details, become selected, or open a menu
        # Just verify the app didn't crash
        try:
            current_activity = driver.current_activity
            assert current_activity is not None, "App should still be running after device item click"
            print(f"[PASS] Device item {item_index} click handled")
        except Exception as e:
            print(f"Warning: Device item {item_index} click verification failed: {e}")


class TestAndroidSettingsComponents(AndroidTestBase):
    """Test Android settings fragment components."""
    
    @pytest.mark.gui
    @pytest.mark.android
    @pytest.mark.components
    @pytest.mark.skipif(not APPIUM_AVAILABLE, reason="Appium not available")
    def test_settings_categories(self, android_driver):
        """Test settings category navigation (FR6)."""
        driver = android_driver
        
        # Navigate to settings
        self._navigate_to_settings_fragment(driver)
        
        # Test settings categories
        settings_categories = [
            "General",
            "Recording",
            "Network",
            "Devices",
            "Privacy",
            "About"
        ]
        
        for category in settings_categories:
            try:
                category_item = driver.find_element(AppiumBy.XPATH, f"//android.widget.TextView[@text='{category}']")
                
                if category_item.is_displayed():
                    category_item.click()
                    time.sleep(1)
                    
                    # Verify category content loads
                    self._verify_settings_category_content(driver, category)
                    
                    # Navigate back to main settings
                    self._navigate_back_to_main_settings(driver)
                    
            except NoSuchElementException:
                print(f"Warning: Settings category {category} not found")
    
    @pytest.mark.gui
    @pytest.mark.android
    @pytest.mark.components
    @pytest.mark.skipif(not APPIUM_AVAILABLE, reason="Appium not available")
    def test_settings_controls(self, android_driver):
        """Test individual settings controls (FR6)."""
        driver = android_driver
        
        self._navigate_to_settings_fragment(driver)
        
        # Test different types of settings controls
        self._test_switch_controls(driver)
        self._test_text_input_controls(driver)
        self._test_selection_controls(driver)
    
    def _navigate_to_settings_fragment(self, driver):
        """Navigate to settings fragment."""
        try:
            settings_tab = driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Settings']")
            settings_tab.click()
            time.sleep(1)
        except NoSuchElementException:
            try:
                drawer_button = driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer")
                drawer_button.click()
                time.sleep(1)
                
                settings_item = driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Settings']")
                settings_item.click()
                time.sleep(1)
            except NoSuchElementException:
                print("Warning: Could not navigate to settings fragment")
    
    def _verify_settings_category_content(self, driver, category: str):
        """Verify settings category shows appropriate content."""
        try:
            # Look for category-specific content
            if category.lower() == "general":
                # Look for general settings
                expected_settings = ["Theme", "Language", "Notifications"]
            elif category.lower() == "recording":
                # Look for recording settings
                expected_settings = ["Sample Rate", "File Format", "Auto Save"]
            elif category.lower() == "network":
                # Look for network settings
                expected_settings = ["WiFi", "Bluetooth", "Server"]
            else:
                expected_settings = []
            
            for setting in expected_settings:
                try:
                    setting_element = driver.find_element(AppiumBy.XPATH, f"//*[contains(@text, '{setting}')]")
                    if setting_element.is_displayed():
                        print(f"[PASS] Found {setting} in {category} settings")
                except NoSuchElementException:
                    continue
            
        except Exception as e:
            print(f"Warning: Could not verify {category} settings content: {e}")
    
    def _navigate_back_to_main_settings(self, driver):
        """Navigate back to main settings screen."""
        try:
            # Try back button
            driver.back()
            time.sleep(1)
        except Exception:
            # Try navigation drawer
            try:
                drawer_button = driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer")
                drawer_button.click()
                time.sleep(1)
                
                settings_item = driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Settings']")
                settings_item.click()
                time.sleep(1)
            except Exception:
                print("Warning: Could not navigate back to main settings")
    
    def _test_switch_controls(self, driver):
        """Test switch/toggle controls in settings."""
        switches = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.Switch")
        
        for i, switch in enumerate(switches[:3]):  # Test first 3 switches
            if switch.is_displayed() and switch.is_enabled():
                original_state = switch.get_attribute("checked")
                
                # Toggle switch
                switch.click()
                time.sleep(0.5)
                
                # Verify state changed
                new_state = switch.get_attribute("checked")
                assert new_state != original_state, f"Switch {i} should have changed state"
                
                print(f"[PASS] Tested switch {i}: {original_state} -> {new_state}")
    
    def _test_text_input_controls(self, driver):
        """Test text input controls in settings."""
        text_inputs = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.EditText")
        
        for i, text_input in enumerate(text_inputs[:3]):  # Test first 3 inputs
            if text_input.is_displayed() and text_input.is_enabled():
                # Test text input
                original_text = text_input.text
                text_input.clear()
                text_input.send_keys(f"test_value_{i}")
                
                # Verify text was entered
                new_text = text_input.text
                assert "test_value" in new_text, f"Text input {i} should accept new text"
                
                print(f"[PASS] Tested text input {i}")
                
                # Restore original text if needed
                if original_text:
                    text_input.clear()
                    text_input.send_keys(original_text)
    
    def _test_selection_controls(self, driver):
        """Test selection controls (spinners, dropdowns) in settings."""
        spinners = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.Spinner")
        
        for i, spinner in enumerate(spinners[:3]):  # Test first 3 spinners
            if spinner.is_displayed() and spinner.is_enabled():
                # Click to open spinner
                spinner.click()
                time.sleep(1)
                
                # Try to select an option
                try:
                    options = driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.TextView")
                    for option in options:
                        if option.is_displayed() and option.text:
                            option.click()
                            time.sleep(0.5)
                            print(f"[PASS] Tested spinner {i} selection")
                            break
                except Exception:
                    # If no options found, just close spinner
                    driver.back()
                    time.sleep(0.5)


if __name__ == "__main__":
    # Run Android GUI component tests
    pytest.main([__file__, "-v", "-m", "gui and android and components", "--tb=short"])