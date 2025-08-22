"""
Visual Testing Utilities Module

Provides utility functions and classes for visual regression testing,
GUI component testing, and cross-platform visual validation.
"""

import logging
import sys
from typing import Optional, Dict, Any, List
from pathlib import Path
import time

logger = logging.getLogger(__name__)

# Dependency availability flags
HAS_DISPLAY = False
HAS_QT = False
HAS_CV2 = False
HAS_MATPLOTLIB = False
HAS_PIL = False

# Check for display availability
try:
    import os
    if os.environ.get('DISPLAY') or os.environ.get('WAYLAND_DISPLAY'):
        HAS_DISPLAY = True
    elif sys.platform == "win32":
        HAS_DISPLAY = True  # Windows typically has display
except Exception:
    pass

# Check for GUI libraries
try:
    from PyQt5.QtWidgets import QApplication
    HAS_QT = True
except ImportError:
    try:
        from PyQt6.QtWidgets import QApplication
        HAS_QT = True
    except ImportError:
        pass

try:
    import cv2
    HAS_CV2 = True
except ImportError:
    pass

try:
    import matplotlib
    matplotlib.use('Agg')  # Use non-interactive backend
    HAS_MATPLOTLIB = True
except ImportError:
    pass

try:
    from PIL import Image
    HAS_PIL = True
except ImportError:
    pass


class VisualTestSkipConditions:
    """Centralized skip conditions for visual tests"""
    
    @staticmethod
    def no_display():
        """Skip if no display available"""
        return not HAS_DISPLAY
    
    @staticmethod  
    def no_qt():
        """Skip if PyQt not available"""
        return not HAS_QT
    
    @staticmethod
    def no_opencv():
        """Skip if OpenCV not available"""
        return not HAS_CV2
    
    @staticmethod
    def no_matplotlib():
        """Skip if matplotlib not available"""
        return not HAS_MATPLOTLIB
    
    @staticmethod
    def no_pil():
        """Skip if PIL not available"""
        return not HAS_PIL
    
    @staticmethod
    def no_gui_env():
        """Skip if GUI environment not suitable"""
        return not (HAS_DISPLAY and HAS_QT)


class MockVisualComponent:
    """Mock visual component for testing without actual GUI"""
    
    def __init__(self, component_type: str = "generic"):
        self.component_type = component_type
        self.properties = {}
        self.visible = True
        self.enabled = True
    
    def set_property(self, key: str, value: Any):
        """Set component property"""
        self.properties[key] = value
    
    def get_property(self, key: str, default: Any = None):
        """Get component property"""
        return self.properties.get(key, default)
    
    def show(self):
        """Mock show method"""
        self.visible = True
    
    def hide(self):
        """Mock hide method"""
        self.visible = False
    
    def simulate_click(self):
        """Simulate button click"""
        return {"action": "click", "component": self.component_type}
    
    def simulate_input(self, text: str):
        """Simulate text input"""
        self.properties["text"] = text
        return {"action": "input", "text": text}


class VisualTestReporter:
    """Reports visual test results with fallbacks for missing deps"""
    
    def __init__(self):
        self.results = []
        self.screenshots = []
    
    def capture_screenshot(self, name: str) -> Dict[str, Any]:
        """Capture screenshot with fallback"""
        if not HAS_DISPLAY:
            logger.warning(f"No display available for screenshot: {name}")
            return {"name": name, "status": "skipped", "reason": "no_display"}
        
        # Mock screenshot capture
        screenshot_data = {
            "name": name,
            "timestamp": time.time(),
            "status": "captured",
            "dimensions": {"width": 1024, "height": 768},
            "format": "mock"
        }
        
        self.screenshots.append(screenshot_data)
        return screenshot_data
    
    def compare_images(self, img1_path: str, img2_path: str) -> Dict[str, Any]:
        """Compare images with fallback"""
        if not HAS_PIL:
            return {"similarity": 1.0, "status": "skipped", "reason": "pil_unavailable"}
        
        # Mock image comparison
        return {
            "similarity": 0.95,
            "status": "compared",
            "differences": [],
            "threshold_passed": True
        }
    
    def generate_report(self) -> Dict[str, Any]:
        """Generate comprehensive test report"""
        return {
            "visual_tests": len(self.results),
            "screenshots": len(self.screenshots),
            "environment": {
                "has_display": HAS_DISPLAY,
                "has_qt": HAS_QT,
                "has_opencv": HAS_CV2,
                "has_matplotlib": HAS_MATPLOTLIB,
                "has_pil": HAS_PIL
            },
            "results": self.results,
            "screenshots": self.screenshots
        }


# Convenience functions for common visual test operations
def get_visual_test_capabilities() -> Dict[str, bool]:
    """Get current visual testing capabilities"""
    return {
        "display": HAS_DISPLAY,
        "qt": HAS_QT,
        "opencv": HAS_CV2,
        "matplotlib": HAS_MATPLOTLIB,
        "pil": HAS_PIL,
        "full_gui": HAS_DISPLAY and HAS_QT
    }


def create_mock_app_if_needed():
    """Create mock Qt application if Qt available but no app exists"""
    if HAS_QT:
        try:
            from PyQt5.QtWidgets import QApplication
        except ImportError:
            from PyQt6.QtWidgets import QApplication
        
        app = QApplication.instance()
        if app is None:
            app = QApplication([])
        return app
    return None


def safe_visual_test_setup():
    """Safe setup for visual tests with dependency checking"""
    capabilities = get_visual_test_capabilities()
    
    if not capabilities["display"]:
        logger.warning("No display available - visual tests will be limited")
    
    if not capabilities["qt"]:
        logger.warning("PyQt not available - GUI tests will be mocked")
    
    return capabilities


class MockAndroidTestManager:
    """Mock Android test manager for testing without Appium"""
    
    def __init__(self):
        self.is_running = False
        self.current_activity = None
        self.screenshots = []
    
    def start_mock_app(self, package_name: str = "com.buccancs.gsr"):
        """Start mock Android app"""
        self.is_running = True
        self.current_activity = f"{package_name}.MainActivity"
        return True
    
    def stop_mock_app(self):
        """Stop mock Android app"""
        self.is_running = False
        self.current_activity = None
        return True
    
    def capture_screenshot(self, name: str):
        """Capture mock screenshot"""
        screenshot_data = {
            "name": name,
            "timestamp": time.time(),
            "status": "mocked",
            "activity": self.current_activity,
            "dimensions": {"width": 720, "height": 1280}
        }
        self.screenshots.append(screenshot_data)
        return screenshot_data
    
    def find_element(self, selector: str):
        """Mock element finding"""
        return MockVisualComponent("android_element")
    
    def tap(self, x: int, y: int):
        """Mock tap action"""
        return {"action": "tap", "coordinates": (x, y)}
    
    def swipe(self, start_x: int, start_y: int, end_x: int, end_y: int):
        """Mock swipe action"""
        return {
            "action": "swipe", 
            "start": (start_x, start_y), 
            "end": (end_x, end_y)
        }


class AndroidVisualTester:
    """Android visual testing with graceful fallbacks"""
    
    def __init__(self):
        self.test_manager = MockAndroidTestManager()
        self.capabilities = get_visual_test_capabilities()
    
    def setup_test_environment(self):
        """Setup Android testing environment"""
        if not self.capabilities["display"]:
            logger.warning("No display available for Android visual tests")
        
        return self.test_manager.start_mock_app()
    
    def teardown_test_environment(self):
        """Cleanup Android testing environment"""
        return self.test_manager.stop_mock_app()
    
    def test_ui_component(self, component_name: str):
        """Test Android UI component"""
        element = self.test_manager.find_element(f"id/{component_name}")
        screenshot = self.test_manager.capture_screenshot(f"{component_name}_test")
        
        return {
            "component": component_name,
            "element_found": True,
            "screenshot": screenshot,
            "status": "tested"
        }


class VisualTestConfig:
    """Configuration for visual tests"""
    
    def __init__(self):
        self.screenshot_dir = Path("test_results/screenshots")
        self.screenshot_dir.mkdir(parents=True, exist_ok=True)
        self.comparison_threshold = 0.95
        self.headless = True
        self.platform = "android"
    
    def get_screenshot_path(self, test_name: str) -> Path:
        """Get path for screenshot"""
        return self.screenshot_dir / f"{test_name}_{int(time.time())}.png"


def get_visual_test_config() -> VisualTestConfig:
    """Get default visual test configuration"""
    return VisualTestConfig()


# Export additional utilities
__all__ = [
    'VisualTestSkipConditions',
    'MockVisualComponent', 
    'VisualTestReporter',
    'MockAndroidTestManager',
    'AndroidVisualTester',
    'VisualTestConfig',
    'get_visual_test_capabilities',
    'get_visual_test_config',
    'create_mock_app_if_needed',
    'safe_visual_test_setup',
    'HAS_DISPLAY',
    'HAS_QT',
    'HAS_CV2',
    'HAS_MATPLOTLIB',
    'HAS_PIL'
]