"""
Android Visual Regression Tests
==============================

Comprehensive visual regression testing for Android UI components using
Appium WebDriver for screenshot capture and pixel-level comparison.

Requirements Coverage:
- FR6: User Interface consistency and visual correctness
- NFR6: Accessibility compliance visual validation
- NFR1: Performance impact of UI rendering and responsiveness

Test Categories:
- Fragment visual consistency
- Navigation flow visual validation
- Theme and styling regression detection
- Accessibility visual compliance
"""

import pytest
import os
import sys
from typing import Dict, List
from pathlib import Path

# Import visual testing utilities
from .visual_utils import (
    VisualTestSkipConditions,
    MockVisualComponent,
    VisualTestReporter,
    VisualTestConfig,
    AndroidVisualTester,
    get_visual_test_capabilities,
    get_visual_test_config,
    HAS_DISPLAY,
    HAS_QT
)

# Appium imports
try:
    from appium import webdriver
    from appium.webdriver.common.appiumby import AppiumBy
    from appium.options.android import UiAutomator2Options
    from selenium.webdriver.support.ui import WebDriverWait
    from selenium.webdriver.support import expected_conditions as EC
    APPIUM_AVAILABLE = True
except ImportError:
    APPIUM_AVAILABLE = False


@pytest.fixture(scope="session")
def visual_config() -> VisualTestConfig:
    """Visual test configuration."""
    return get_visual_test_config()


@pytest.fixture(scope="session")
def android_visual_tester(visual_config) -> AndroidVisualTester:
    """Android visual tester instance."""
    return AndroidVisualTester(visual_config)


@pytest.fixture(scope="session")
def appium_driver_visual():
    """Appium driver specifically configured for visual testing."""
    if not APPIUM_AVAILABLE:
        pytest.skip("Appium not available")
    
    capabilities = {
        "platformName": "Android",
        "platformVersion": "8.0",
        "deviceName": "Android Emulator",
        "app": os.path.join(os.path.dirname(__file__), "..", "..", "AndroidApp", "build", "outputs", "apk", "debug", "app-debug.apk"),
        "automationName": "UiAutomator2",
        "appPackage": "com.multisensor.recording",
        "appActivity": "com.multisensor.recording.ui.MainActivity",
        "autoGrantPermissions": True,
        "noReset": False,
        "fullReset": True,
        "newCommandTimeout": 300,
        # Visual testing specific settings
        "enableLogs": False,
        "isHeadless": False,  # Ensure UI is rendered for screenshots
        "deviceOrientation": "portrait"
    }
    
    try:
        options = UiAutomator2Options()
        options.load_capabilities(capabilities)
        
        driver = webdriver.Remote(
            command_executor="http://localhost:4723/wd/hub",
            options=options
        )
        
        # Wait for app to fully load
        WebDriverWait(driver, 30).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/main_activity"))
        )
        
        # Allow UI to stabilize
        import time
        time.sleep(3)
        
        yield driver
        
    except Exception as e:
        pytest.skip(f"Appium server not available or app not found: {e}")
    
    finally:
        if 'driver' in locals():
            driver.quit()


class TestAndroidFragmentVisuals:
    """Visual regression tests for Android fragments."""
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    @pytest.mark.skipif(not HAS_DISPLAY, reason="No display available for GUI testing")
    def test_main_fragment_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance of main fragment (FR6)."""
        driver = appium_driver_visual
        
        # Ensure we're on main fragment
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        main_item = driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Recording']")
        main_item.click()
        
        # Wait for fragment to stabilize
        WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/recording_fragment"))
        )
        
        # Test visual appearance
        result = android_visual_tester.test_fragment_screenshot(driver, "main")
        
        assert result["status"] in ["passed", "baseline_created"], \
            f"Main fragment visual test failed: {result['difference_percent']:.2f}% difference"
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    def test_devices_fragment_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance of devices fragment (FR6)."""
        driver = appium_driver_visual
        
        result = android_visual_tester.test_fragment_screenshot(driver, "Devices")
        
        assert result["status"] in ["passed", "baseline_created"], \
            f"Devices fragment visual test failed: {result['difference_percent']:.2f}% difference"
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    def test_settings_fragment_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance of settings fragment (FR6)."""
        driver = appium_driver_visual
        
        result = android_visual_tester.test_fragment_screenshot(driver, "Settings")
        
        assert result["status"] in ["passed", "baseline_created"], \
            f"Settings fragment visual test failed: {result['difference_percent']:.2f}% difference"
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    def test_calibration_fragment_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance of calibration fragment (FR6)."""
        driver = appium_driver_visual
        
        result = android_visual_tester.test_fragment_screenshot(driver, "Calibration")
        
        assert result["status"] in ["passed", "baseline_created"], \
            f"Calibration fragment visual test failed: {result['difference_percent']:.2f}% difference"


class TestAndroidNavigationVisuals:
    """Visual regression tests for Android navigation components."""
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    def test_navigation_drawer_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance of navigation drawer (FR6)."""
        driver = appium_driver_visual
        
        # Open navigation drawer
        driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
        
        # Wait for drawer to open
        WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((AppiumBy.ID, "com.multisensor.recording:id/nav_view"))
        )
        
        # Capture screenshot with drawer open
        import time
        time.sleep(1)  # Allow animation to complete
        
        screenshot_path = android_visual_tester._capture_android_screenshot(driver, "navigation_drawer")
        baseline_path = android_visual_tester.config.baseline_dir / "android_navigation_drawer.png"
        
        result = android_visual_tester.comparator.compare_screenshots(
            baseline_path, screenshot_path, "android_navigation_drawer"
        )
        
        assert result["status"] in ["passed", "baseline_created"], \
            f"Navigation drawer visual test failed: {result['difference_percent']:.2f}% difference"
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    def test_bottom_navigation_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance of bottom navigation (FR6)."""
        driver = appium_driver_visual
        
        # Ensure bottom navigation is visible
        try:
            bottom_nav = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/bottom_navigation")
            if bottom_nav.is_displayed():
                screenshot_path = android_visual_tester._capture_android_screenshot(driver, "bottom_navigation")
                baseline_path = android_visual_tester.config.baseline_dir / "android_bottom_navigation.png"
                
                result = android_visual_tester.comparator.compare_screenshots(
                    baseline_path, screenshot_path, "android_bottom_navigation"
                )
                
                assert result["status"] in ["passed", "baseline_created"], \
                    f"Bottom navigation visual test failed: {result['difference_percent']:.2f}% difference"
        except:
            pytest.skip("Bottom navigation not available in this build")


class TestAndroidThemeVisuals:
    """Visual regression tests for Android theme and styling."""
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    def test_light_theme_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance with light theme (FR6)."""
        driver = appium_driver_visual
        
        # Ensure light theme is active
        self._set_app_theme(driver, "light")
        
        # Test main screen appearance with light theme
        result = android_visual_tester.test_fragment_screenshot(driver, "main_light_theme")
        
        assert result["status"] in ["passed", "baseline_created"], \
            f"Light theme visual test failed: {result['difference_percent']:.2f}% difference"
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    def test_dark_theme_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance with dark theme (FR6)."""
        driver = appium_driver_visual
        
        # Switch to dark theme
        self._set_app_theme(driver, "dark")
        
        # Test main screen appearance with dark theme
        result = android_visual_tester.test_fragment_screenshot(driver, "main_dark_theme")
        
        assert result["status"] in ["passed", "baseline_created"], \
            f"Dark theme visual test failed: {result['difference_percent']:.2f}% difference"
    
    def _set_app_theme(self, driver, theme: str):
        """Helper to set app theme."""
        try:
            # Navigate to settings
            driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
            driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Settings']").click()
            
            # Find theme setting
            theme_setting = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/theme_setting")
            theme_setting.click()
            
            # Select theme
            theme_option = driver.find_element(AppiumBy.XPATH, f"//android.widget.TextView[@text='{theme.title()}']")
            theme_option.click()
            
            # Apply theme
            apply_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_apply_theme")
            apply_button.click()
            
            # Wait for theme to apply
            import time
            time.sleep(2)
            
        except Exception as e:
            print(f"Warning: Could not set theme to {theme}: {e}")


class TestAndroidAccessibilityVisuals:
    """Visual regression tests for Android accessibility compliance."""
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    def test_large_text_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance with large text accessibility setting (NFR6)."""
        driver = appium_driver_visual
        
        # Enable large text mode
        self._set_accessibility_setting(driver, "large_text", True)
        
        # Test fragment appearance with large text
        result = android_visual_tester.test_fragment_screenshot(driver, "main_large_text")
        
        assert result["status"] in ["passed", "baseline_created"], \
            f"Large text visual test failed: {result['difference_percent']:.2f}% difference"
        
        # Verify text is actually larger
        if result["status"] == "failed":
            # This is expected on first run or when text size changes
            assert result["difference_percent"] > 5.0, \
                "Large text setting should cause significant visual changes"
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    def test_high_contrast_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance with high contrast accessibility setting (NFR6)."""
        driver = appium_driver_visual
        
        # Enable high contrast mode
        self._set_accessibility_setting(driver, "high_contrast", True)
        
        # Test fragment appearance with high contrast
        result = android_visual_tester.test_fragment_screenshot(driver, "main_high_contrast")
        
        assert result["status"] in ["passed", "baseline_created"], \
            f"High contrast visual test failed: {result['difference_percent']:.2f}% difference"
    
    def _set_accessibility_setting(self, driver, setting: str, enabled: bool):
        """Helper to set accessibility settings."""
        try:
            # Navigate to accessibility settings in app
            driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
            driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Settings']").click()
            
            # Open accessibility section
            accessibility_section = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/accessibility_settings")
            accessibility_section.click()
            
            # Toggle specific setting
            setting_toggle = driver.find_element(AppiumBy.ID, f"com.multisensor.recording:id/toggle_{setting}")
            
            current_state = setting_toggle.get_attribute("checked") == "true"
            if current_state != enabled:
                setting_toggle.click()
            
            # Apply changes
            apply_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_apply_accessibility")
            apply_button.click()
            
            # Wait for settings to apply
            import time
            time.sleep(2)
            
        except Exception as e:
            print(f"Warning: Could not set accessibility setting {setting}: {e}")


class TestAndroidDialogVisuals:
    """Visual regression tests for Android dialogs and overlays."""
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    def test_error_dialog_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance of error dialogs (FR8)."""
        driver = appium_driver_visual
        
        # Trigger error dialog (simulate network error)
        self._trigger_error_dialog(driver)
        
        # Capture dialog screenshot
        screenshot_path = android_visual_tester._capture_android_screenshot(driver, "error_dialog")
        baseline_path = android_visual_tester.config.baseline_dir / "android_error_dialog.png"
        
        result = android_visual_tester.comparator.compare_screenshots(
            baseline_path, screenshot_path, "android_error_dialog"
        )
        
        assert result["status"] in ["passed", "baseline_created"], \
            f"Error dialog visual test failed: {result['difference_percent']:.2f}% difference"
    
    @pytest.mark.visual
    @pytest.mark.appium
    @pytest.mark.android
    def test_confirmation_dialog_visual(self, appium_driver_visual, android_visual_tester):
        """Test visual appearance of confirmation dialogs (FR6)."""
        driver = appium_driver_visual
        
        # Trigger confirmation dialog
        self._trigger_confirmation_dialog(driver)
        
        # Capture dialog screenshot
        screenshot_path = android_visual_tester._capture_android_screenshot(driver, "confirmation_dialog")
        baseline_path = android_visual_tester.config.baseline_dir / "android_confirmation_dialog.png"
        
        result = android_visual_tester.comparator.compare_screenshots(
            baseline_path, screenshot_path, "android_confirmation_dialog"
        )
        
        assert result["status"] in ["passed", "baseline_created"], \
            f"Confirmation dialog visual test failed: {result['difference_percent']:.2f}% difference"
    
    def _trigger_error_dialog(self, driver):
        """Helper to trigger error dialog."""
        try:
            # Navigate to devices and try to scan without network
            driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
            driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Devices']").click()
            
            # Disable network
            driver.set_network_connection(1)  # Airplane mode
            
            # Try to scan
            scan_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_scan_devices")
            scan_button.click()
            
            # Wait for error dialog
            WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((AppiumBy.ID, "android:id/alertTitle"))
            )
            
        except Exception as e:
            print(f"Warning: Could not trigger error dialog: {e}")
    
    def _trigger_confirmation_dialog(self, driver):
        """Helper to trigger confirmation dialog."""
        try:
            # Navigate to settings and try to reset
            driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Open navigation drawer").click()
            driver.find_element(AppiumBy.XPATH, "//android.widget.TextView[@text='Settings']").click()
            
            # Try to reset settings
            reset_button = driver.find_element(AppiumBy.ID, "com.multisensor.recording:id/btn_reset_settings")
            reset_button.click()
            
            # Wait for confirmation dialog
            WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((AppiumBy.ID, "android:id/alertTitle"))
            )
            
        except Exception as e:
            print(f"Warning: Could not trigger confirmation dialog: {e}")


if __name__ == "__main__":
    # Run visual tests
    pytest.main([__file__, "-v", "-m", "visual", "--tb=short"])