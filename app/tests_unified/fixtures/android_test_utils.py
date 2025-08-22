"""
Android Test Utilities and Fixtures
===================================

Utilities and fixtures for comprehensive Android GUI testing using Appium.
Provides base classes, device configuration, and helper methods for
reliable Android application testing.
"""

import pytest
import time
import json
import tempfile
from typing import Dict, List, Optional, Any
from pathlib import Path

# Appium and Android testing imports
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


class AndroidTestBase:
    """Base class for Android GUI tests with common utilities."""
    
    def setup_method(self):
        """Setup for each test method."""
        self.test_data_dir = Path(tempfile.mkdtemp(prefix="android_test_"))
        self.screenshots = []
    
    def teardown_method(self):
        """Cleanup after each test method."""
        # Clean up test data
        if hasattr(self, 'test_data_dir') and self.test_data_dir.exists():
            import shutil
            shutil.rmtree(self.test_data_dir, ignore_errors=True)
    
    def take_screenshot(self, driver, name: str = None):
        """Take screenshot for debugging."""
        if name is None:
            name = f"screenshot_{int(time.time())}"
        
        screenshot_path = self.test_data_dir / f"{name}.png"
        driver.save_screenshot(str(screenshot_path))
        self.screenshots.append(screenshot_path)
        return screenshot_path
    
    def wait_for_element(self, driver, locator, timeout=10):
        """Wait for element to be present."""
        return WebDriverWait(driver, timeout).until(
            EC.presence_of_element_located(locator)
        )
    
    def wait_for_element_clickable(self, driver, locator, timeout=10):
        """Wait for element to be clickable."""
        return WebDriverWait(driver, timeout).until(
            EC.element_to_be_clickable(locator)
        )
    
    def safe_click(self, driver, element, max_attempts=3):
        """Safely click element with retries."""
        for attempt in range(max_attempts):
            try:
                element.click()
                return True
            except Exception as e:
                if attempt == max_attempts - 1:
                    raise e
                time.sleep(0.5)
        return False
    
    def get_element_text_safe(self, element):
        """Safely get element text."""
        try:
            return element.text or element.get_attribute("content-desc") or ""
        except Exception:
            return ""
    
    def navigate_with_retry(self, driver, navigation_func, verify_func, max_attempts=3):
        """Navigate with verification and retry."""
        for attempt in range(max_attempts):
            try:
                navigation_func()
                time.sleep(1)
                if verify_func():
                    return True
            except Exception as e:
                if attempt == max_attempts - 1:
                    raise e
                time.sleep(1)
        return False


def get_android_capabilities():
    """Get Android test capabilities."""
    app_path = None
    
    # Try to find the APK
    possible_paths = [
        "AndroidApp/build/outputs/apk/debug/app-debug.apk",
        "../AndroidApp/build/outputs/apk/debug/app-debug.apk",
        "../../AndroidApp/build/outputs/apk/debug/app-debug.apk"
    ]
    
    for path in possible_paths:
        if Path(path).exists():
            app_path = str(Path(path).absolute())
            break
    
    capabilities = {
        "platformName": "Android",
        "platformVersion": "8.0",  # Minimum supported version
        "deviceName": "Android Emulator",
        "automationName": "UiAutomator2",
        "appPackage": "com.multisensor.recording",
        "appActivity": "com.multisensor.recording.MainActivity",
        "autoGrantPermissions": True,
        "noReset": False,
        "fullReset": False,
        "newCommandTimeout": 300,
        "uiautomator2ServerInstallTimeout": 60000,
        "adbExecTimeout": 20000,
        "androidInstallTimeout": 90000,
        # Performance settings
        "skipDeviceInitialization": False,
        "skipServerInstallation": False,
        "ignoreHiddenApiPolicyError": True,
        # Reliability settings
        "waitForQuiescence": False,
        "disableWindowAnimation": True,
    }
    
    if app_path:
        capabilities["app"] = app_path
    
    return capabilities


@pytest.fixture(scope="session")
def android_driver():
    """Appium WebDriver instance for Android testing."""
    if not APPIUM_AVAILABLE:
        pytest.skip("Appium not available")
    
    capabilities = get_android_capabilities()
    
    try:
        options = UiAutomator2Options()
        options.load_capabilities(capabilities)
        
        driver = webdriver.Remote(
            command_executor="http://localhost:4723/wd/hub",
            options=options
        )
        
        # Wait for app to fully load
        WebDriverWait(driver, 60).until(
            EC.presence_of_element_located(
                (AppiumBy.XPATH, "//*[@resource-id='com.multisensor.recording:id/main_activity' or @class='android.widget.FrameLayout']")
            )
        )
        
        # Allow UI to stabilize
        time.sleep(3)
        
        yield driver
        
    except Exception as e:
        pytest.skip(f"Appium server not available or app not found: {e}")
    
    finally:
        if 'driver' in locals():
            try:
                driver.quit()
            except Exception:
                pass


@pytest.fixture
def android_test_helper():
    """Helper utilities for Android testing."""
    return AndroidTestHelper()


class AndroidTestHelper:
    """Helper class for Android GUI testing operations."""
    
    @staticmethod
    def find_element_by_text(driver, text: str, exact_match=True):
        """Find element by text content."""
        try:
            if exact_match:
                xpath = f"//*[@text='{text}']"
            else:
                xpath = f"//*[contains(@text, '{text}')]"
            
            return driver.find_element(AppiumBy.XPATH, xpath)
        except NoSuchElementException:
            return None
    
    @staticmethod
    def find_element_by_content_desc(driver, content_desc: str):
        """Find element by content description."""
        try:
            return driver.find_element(AppiumBy.ACCESSIBILITY_ID, content_desc)
        except NoSuchElementException:
            return None
    
    @staticmethod
    def get_all_text_elements(driver):
        """Get all text elements on screen."""
        return driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.TextView")
    
    @staticmethod
    def get_all_clickable_elements(driver):
        """Get all clickable elements on screen."""
        return driver.find_elements(AppiumBy.XPATH, "//*[@clickable='true']")
    
    @staticmethod
    def get_all_input_elements(driver):
        """Get all input elements on screen."""
        return driver.find_elements(AppiumBy.CLASS_NAME, "android.widget.EditText")
    
    @staticmethod
    def is_keyboard_visible(driver):
        """Check if keyboard is currently visible."""
        try:
            return driver.is_keyboard_shown()
        except Exception:
            return False
    
    @staticmethod
    def hide_keyboard_safe(driver):
        """Safely hide keyboard if visible."""
        try:
            if AndroidTestHelper.is_keyboard_visible(driver):
                driver.hide_keyboard()
                time.sleep(0.5)
        except Exception:
            pass
    
    @staticmethod
    def scroll_to_element(driver, element_text: str, max_scrolls=5):
        """Scroll to find element by text."""
        for i in range(max_scrolls):
            element = AndroidTestHelper.find_element_by_text(driver, element_text, exact_match=False)
            if element and element.is_displayed():
                return element
            
            # Scroll down
            size = driver.get_window_size()
            start_y = size['height'] * 0.7
            end_y = size['height'] * 0.3
            x = size['width'] * 0.5
            
            driver.swipe(x, start_y, x, end_y, 500)
            time.sleep(1)
        
        return None
    
    @staticmethod
    def wait_for_loading_complete(driver, timeout=30):
        """Wait for loading indicators to disappear."""
        loading_indicators = [
            "android:id/progress",
            "com.multisensor.recording:id/progress_bar",
            "com.multisensor.recording:id/loading_indicator"
        ]
        
        start_time = time.time()
        while time.time() - start_time < timeout:
            loading_found = False
            for indicator_id in loading_indicators:
                try:
                    indicator = driver.find_element(AppiumBy.ID, indicator_id)
                    if indicator.is_displayed():
                        loading_found = True
                        break
                except NoSuchElementException:
                    continue
            
            if not loading_found:
                return True
            
            time.sleep(0.5)
        
        return False
    
    @staticmethod
    def capture_element_bounds(element):
        """Capture element bounds for interaction."""
        try:
            return element.rect
        except Exception:
            return None
    
    @staticmethod
    def perform_swipe_gesture(driver, direction="up", distance_ratio=0.5):
        """Perform swipe gesture in specified direction."""
        size = driver.get_window_size()
        width = size['width']
        height = size['height']
        
        center_x = width * 0.5
        center_y = height * 0.5
        
        if direction == "up":
            start_y = height * (0.5 + distance_ratio/2)
            end_y = height * (0.5 - distance_ratio/2)
            driver.swipe(center_x, start_y, center_x, end_y, 500)
        elif direction == "down":
            start_y = height * (0.5 - distance_ratio/2)
            end_y = height * (0.5 + distance_ratio/2)
            driver.swipe(center_x, start_y, center_x, end_y, 500)
        elif direction == "left":
            start_x = width * (0.5 + distance_ratio/2)
            end_x = width * (0.5 - distance_ratio/2)
            driver.swipe(start_x, center_y, end_x, center_y, 500)
        elif direction == "right":
            start_x = width * (0.5 - distance_ratio/2)
            end_x = width * (0.5 + distance_ratio/2)
            driver.swipe(start_x, center_y, end_x, center_y, 500)
    
    @staticmethod
    def verify_element_properties(element, expected_properties: Dict):
        """Verify element has expected properties."""
        for prop, expected_value in expected_properties.items():
            try:
                if prop == "text":
                    actual_value = element.text
                elif prop == "enabled":
                    actual_value = element.is_enabled()
                elif prop == "displayed":
                    actual_value = element.is_displayed()
                elif prop == "selected":
                    actual_value = element.is_selected()
                else:
                    actual_value = element.get_attribute(prop)
                
                if actual_value != expected_value:
                    return False, f"Property {prop}: expected {expected_value}, got {actual_value}"
            except Exception as e:
                return False, f"Error checking property {prop}: {e}"
        
        return True, "All properties match"


class AndroidDeviceSimulator:
    """Simulate device conditions for testing."""
    
    def __init__(self, driver):
        self.driver = driver
        self.original_connection = None
    
    def simulate_network_disconnection(self):
        """Simulate network disconnection."""
        try:
            self.original_connection = self.driver.network_connection
            self.driver.set_network_connection(1)  # Airplane mode
            return True
        except Exception:
            return False
    
    def restore_network_connection(self):
        """Restore original network connection."""
        try:
            if self.original_connection:
                self.driver.set_network_connection(self.original_connection)
            return True
        except Exception:
            return False
    
    def simulate_low_battery(self):
        """Simulate low battery condition."""
        # This would require special test builds or emulator settings
        # For now, just return success
        return True
    
    def simulate_memory_pressure(self):
        """Simulate memory pressure."""
        # Start and stop multiple apps to create memory pressure
        try:
            system_apps = [
                "com.android.settings",
                "com.android.calculator2",
                "com.android.camera2"
            ]
            
            for app in system_apps:
                try:
                    self.driver.activate_app(app)
                    time.sleep(0.5)
                except Exception:
                    continue
            
            # Return to our app
            self.driver.activate_app("com.multisensor.recording")
            return True
        except Exception:
            return False
    
    def simulate_orientation_change(self, orientation="LANDSCAPE"):
        """Simulate device orientation change."""
        try:
            original_orientation = self.driver.orientation
            self.driver.orientation = orientation
            time.sleep(2)  # Allow UI to adapt
            return original_orientation
        except Exception:
            return None
    
    def restore_orientation(self, original_orientation):
        """Restore original orientation."""
        try:
            if original_orientation:
                self.driver.orientation = original_orientation
                time.sleep(2)
            return True
        except Exception:
            return False


@pytest.fixture
def android_device_simulator(android_driver):
    """Device simulator for testing various conditions."""
    return AndroidDeviceSimulator(android_driver)


def create_android_test_data():
    """Create test data specifically for Android testing."""
    return {
        "valid_inputs": [
            "test@example.com",
            "192.168.1.100",
            "device_001",
            "Test Session Name"
        ],
        "invalid_inputs": [
            "",  # Empty
            " ",  # Whitespace only
            "a" * 1000,  # Too long
            "invalid@",  # Invalid email
            "999.999.999.999",  # Invalid IP
            "<script>alert('xss')</script>",  # XSS attempt
            "'; DROP TABLE users; --"  # SQL injection attempt
        ],
        "device_ids": [
            "shimmer_001",
            "thermal_cam_001", 
            "android_001"
        ],
        "test_settings": {
            "sampling_rate": 100,
            "buffer_size": 1000,
            "auto_connect": True,
            "save_location": "/sdcard/test_data"
        }
    }


# Common Android test configuration
ANDROID_TEST_CONFIG = {
    "default_timeout": 30,
    "short_timeout": 5,
    "long_timeout": 60,
    "swipe_duration": 500,
    "interaction_delay": 0.5,
    "screenshot_on_failure": True,
    "max_retry_attempts": 3,
    "element_wait_timeout": 10
}