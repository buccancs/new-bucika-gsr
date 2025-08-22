"""
Cross-Browser Compatibility Tests
=================================

Comprehensive cross-browser testing for the Web dashboard using Playwright.
Tests functionality, performance, and accessibility across Chrome, Firefox,
Safari, and Edge browsers.

Requirements Coverage:
- FR6: User Interface consistency across browser engines
- NFR6: Accessibility compliance in different browsers  
- NFR1: Performance consistency across browsers
- NFR5: Security behavior validation across browsers
"""

import pytest
import asyncio
import time
import os
import sys
from typing import Dict, List, Optional, Tuple, Any
from pathlib import Path

# Playwright imports
try:
    from playwright.async_api import async_playwright, Browser, BrowserContext, Page
    from playwright.sync_api import sync_playwright
    PLAYWRIGHT_AVAILABLE = True
except ImportError:
    PLAYWRIGHT_AVAILABLE = False
    async_playwright = None
    Browser = None
    BrowserContext = None
    Page = None

# Add PythonApp to path
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'PythonApp'))

try:
    from PythonApp.web_ui.web_dashboard import WebDashboardServer
    WEB_AVAILABLE = True
except ImportError:
    WEB_AVAILABLE = False
    WebDashboardServer = None


class BrowserTestConfig:
    """Configuration for cross-browser testing."""
    
    def __init__(self):
        self.base_url = "http://localhost:5000"
        self.timeout_ms = 30000
        self.screenshot_dir = Path(__file__).parent / "screenshots"
        self.browsers = ["chromium", "firefox", "webkit"]  # webkit = Safari engine
        self.devices = ["Desktop", "Tablet", "Mobile"]
        
        # Ensure screenshot directory exists
        self.screenshot_dir.mkdir(exist_ok=True)
    
    def get_browser_config(self, browser_name: str) -> Dict:
        """Get browser-specific configuration."""
        configs = {
            "chromium": {
                "headless": True,
                "args": ["--no-sandbox", "--disable-dev-shm-usage"],
                "ignoreDefaultArgs": ["--disable-extensions"]
            },
            "firefox": {
                "headless": True,
                "firefoxUserPrefs": {
                    "dom.webnotifications.enabled": False,
                    "media.navigator.permission.disabled": True
                }
            },
            "webkit": {
                "headless": True
            }
        }
        return configs.get(browser_name, {})
    
    def get_device_config(self, device_type: str) -> Dict:
        """Get device-specific viewport configuration."""
        configs = {
            "Desktop": {"width": 1920, "height": 1080},
            "Tablet": {"width": 768, "height": 1024},
            "Mobile": {"width": 375, "height": 667}
        }
        return configs.get(device_type, configs["Desktop"])


class CrossBrowserTester:
    """Handles cross-browser testing operations."""
    
    def __init__(self, config: BrowserTestConfig):
        self.config = config
        self.test_results = {}
        
        if not PLAYWRIGHT_AVAILABLE:
            raise ImportError("Playwright required for browser testing. Install with: pip install playwright")
    
    async def run_browser_test(self, browser_name: str, test_function, **kwargs) -> Dict:
        """Run a test function across a specific browser."""
        async with async_playwright() as p:
            browser_config = self.config.get_browser_config(browser_name)
            
            # Launch browser
            if browser_name == "chromium":
                browser = await p.chromium.launch(**browser_config)
            elif browser_name == "firefox":
                browser = await p.firefox.launch(**browser_config)
            elif browser_name == "webkit":
                browser = await p.webkit.launch(**browser_config)
            else:
                raise ValueError(f"Unsupported browser: {browser_name}")
            
            try:
                # Create context and page
                context = await browser.new_context(
                    viewport=self.config.get_device_config("Desktop")
                )
                page = await context.new_page()
                
                # Set timeouts
                page.set_default_timeout(self.config.timeout_ms)
                
                # Run test function
                result = await test_function(page, browser_name, **kwargs)
                
                return {
                    "browser": browser_name,
                    "success": True,
                    "result": result,
                    "error": None
                }
                
            except Exception as e:
                return {
                    "browser": browser_name,
                    "success": False,
                    "result": None,
                    "error": str(e)
                }
            
            finally:
                await browser.close()
    
    async def run_cross_browser_test(self, test_function, **kwargs) -> Dict[str, Dict]:
        """Run a test function across all configured browsers."""
        tasks = []
        
        for browser_name in self.config.browsers:
            task = asyncio.create_task(
                self.run_browser_test(browser_name, test_function, **kwargs)
            )
            tasks.append(task)
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        # Organize results by browser
        browser_results = {}
        for result in results:
            if isinstance(result, Exception):
                continue
            browser_results[result["browser"]] = result
        
        return browser_results
    
    async def capture_cross_browser_screenshots(self, url_path: str = "/") -> Dict[str, str]:
        """Capture screenshots across all browsers."""
        async def screenshot_test(page: Page, browser_name: str) -> str:
            url = f"{self.config.base_url}{url_path}"
            await page.goto(url)
            await page.wait_for_load_state("networkidle")
            
            screenshot_path = self.config.screenshot_dir / f"{browser_name}_{url_path.replace('/', '_')}.png"
            await page.screenshot(path=str(screenshot_path), full_page=True)
            
            return str(screenshot_path)
        
        results = await self.run_cross_browser_test(screenshot_test)
        
        screenshots = {}
        for browser, result in results.items():
            if result["success"]:
                screenshots[browser] = result["result"]
        
        return screenshots


@pytest.fixture(scope="session")
def web_dashboard():
    """Start Web dashboard server for browser testing."""
    if not WEB_AVAILABLE:
        pytest.skip("Web dashboard not available")
    
    server = WebDashboardServer(port=5000, debug=False)
    server.start()
    time.sleep(3)  # Allow server to start
    
    yield server
    
    server.stop()


@pytest.fixture(scope="session")
def browser_config():
    """Browser test configuration."""
    return BrowserTestConfig()


@pytest.fixture(scope="session")
def cross_browser_tester(browser_config):
    """Cross-browser tester instance."""
    return CrossBrowserTester(browser_config)


class TestBasicBrowserCompatibility:
    """Basic compatibility tests across browsers."""
    
    @pytest.mark.browser
    @pytest.mark.network
    def test_page_loading_compatibility(self, web_dashboard, cross_browser_tester):
        """Test basic page loading across browsers (FR6)."""
        async def page_load_test(page: Page, browser_name: str) -> Dict:
            start_time = time.time()
            
            # Navigate to main page
            response = await page.goto(f"{cross_browser_tester.config.base_url}/")
            load_time = (time.time() - start_time) * 1000
            
            # Check response status
            assert response.status == 200, f"Page failed to load in {browser_name}"
            
            # Wait for page to be ready
            await page.wait_for_load_state("domcontentloaded")
            
            # Check for critical elements
            title = await page.title()
            assert "Multi-Sensor" in title, f"Incorrect page title in {browser_name}: {title}"
            
            # Check for navigation elements
            nav_elements = await page.query_selector_all("nav, .navbar, .navigation")
            assert len(nav_elements) > 0, f"No navigation elements found in {browser_name}"
            
            return {
                "load_time_ms": load_time,
                "title": title,
                "nav_elements": len(nav_elements)
            }
        
        results = asyncio.run(cross_browser_tester.run_cross_browser_test(page_load_test))
        
        # Verify all browsers loaded successfully
        for browser, result in results.items():
            assert result["success"], f"Page loading failed in {browser}: {result['error']}"
            
            # Performance check
            load_time = result["result"]["load_time_ms"]
            assert load_time < 5000, f"Page load too slow in {browser}: {load_time:.1f}ms"
    
    @pytest.mark.browser
    @pytest.mark.network
    def test_javascript_functionality_compatibility(self, web_dashboard, cross_browser_tester):
        """Test JavaScript functionality across browsers (FR6)."""
        async def js_test(page: Page, browser_name: str) -> Dict:
            await page.goto(f"{cross_browser_tester.config.base_url}/")
            await page.wait_for_load_state("domcontentloaded")
            
            # Test JavaScript execution
            js_result = await page.evaluate("() => { return window.navigator.userAgent; }")
            assert js_result is not None, f"JavaScript execution failed in {browser_name}"
            
            # Test DOM manipulation
            button_count = await page.evaluate("() => { return document.querySelectorAll('button').length; }")
            assert button_count > 0, f"No buttons found in {browser_name} - DOM manipulation issue"
            
            # Test event handling
            if button_count > 0:
                # Click first button and check for response
                await page.click("button")
                await page.wait_for_timeout(1000)  # Allow for event processing
            
            # Test console for errors
            console_errors = []
            page.on("console", lambda msg: console_errors.append(msg.text) if msg.type == "error" else None)
            
            await page.reload()
            await page.wait_for_load_state("domcontentloaded")
            
            return {
                "user_agent": js_result,
                "button_count": button_count,
                "console_errors": len(console_errors)
            }
        
        results = asyncio.run(cross_browser_tester.run_cross_browser_test(js_test))
        
        # Verify JavaScript functionality in all browsers
        for browser, result in results.items():
            assert result["success"], f"JavaScript test failed in {browser}: {result['error']}"
            
            js_data = result["result"]
            assert js_data["button_count"] > 0, f"No interactive elements in {browser}"
            assert js_data["console_errors"] < 5, f"Too many console errors in {browser}: {js_data['console_errors']}"
    
    @pytest.mark.browser
    @pytest.mark.network
    def test_css_rendering_compatibility(self, web_dashboard, cross_browser_tester):
        """Test CSS rendering consistency across browsers (FR6)."""
        async def css_test(page: Page, browser_name: str) -> Dict:
            await page.goto(f"{cross_browser_tester.config.base_url}/")
            await page.wait_for_load_state("domcontentloaded")
            
            # Check for critical CSS elements
            body_color = await page.evaluate("() => { return window.getComputedStyle(document.body).color; }")
            body_background = await page.evaluate("() => { return window.getComputedStyle(document.body).backgroundColor; }")
            
            # Check for responsive design
            viewport_width = await page.evaluate("() => { return window.innerWidth; }")
            viewport_height = await page.evaluate("() => { return window.innerHeight; }")
            
            # Check for flexbox/grid support
            flex_support = await page.evaluate("""() => { 
                const div = document.createElement('div');
                div.style.display = 'flex';
                return div.style.display === 'flex';
            }""")
            
            # Test media queries
            is_desktop = await page.evaluate("() => { return window.matchMedia('(min-width: 1024px)').matches; }")
            
            return {
                "body_color": body_color,
                "body_background": body_background,
                "viewport": {"width": viewport_width, "height": viewport_height},
                "flex_support": flex_support,
                "is_desktop": is_desktop
            }
        
        results = asyncio.run(cross_browser_tester.run_cross_browser_test(css_test))
        
        # Verify CSS rendering consistency
        baseline_result = None
        for browser, result in results.items():
            assert result["success"], f"CSS test failed in {browser}: {result['error']}"
            
            css_data = result["result"]
            assert css_data["flex_support"], f"Flexbox not supported in {browser}"
            
            if baseline_result is None:
                baseline_result = css_data
            else:
                # Compare with baseline (allow some variation)
                assert css_data["viewport"]["width"] == baseline_result["viewport"]["width"], \
                    f"Viewport width mismatch in {browser}"


class TestResponsiveDesignCompatibility:
    """Test responsive design across browsers and devices."""
    
    @pytest.mark.browser
    @pytest.mark.network
    def test_mobile_responsiveness(self, web_dashboard, cross_browser_tester):
        """Test mobile responsiveness across browsers (FR6, NFR6)."""
        async def mobile_test(page: Page, browser_name: str) -> Dict:
            # Set mobile viewport
            await page.set_viewport_size(width=375, height=667)
            await page.goto(f"{cross_browser_tester.config.base_url}/")
            await page.wait_for_load_state("domcontentloaded")
            
            # Check mobile-specific elements
            hamburger_menu = await page.query_selector(".hamburger, .mobile-menu, .menu-toggle")
            mobile_nav = await page.query_selector(".mobile-nav, .nav-mobile")
            
            # Check touch-friendly button sizes
            buttons = await page.query_selector_all("button")
            small_buttons = 0
            
            for button in buttons:
                bbox = await button.bounding_box()
                if bbox and (bbox["width"] < 44 or bbox["height"] < 44):
                    small_buttons += 1
            
            # Test horizontal scrolling
            body_width = await page.evaluate("() => { return document.body.scrollWidth; }")
            viewport_width = await page.evaluate("() => { return window.innerWidth; }")
            
            return {
                "has_hamburger_menu": hamburger_menu is not None,
                "has_mobile_nav": mobile_nav is not None,
                "small_buttons_count": small_buttons,
                "horizontal_scroll": body_width > viewport_width,
                "viewport_width": viewport_width,
                "body_width": body_width
            }
        
        results = asyncio.run(cross_browser_tester.run_cross_browser_test(mobile_test))
        
        # Verify mobile responsiveness
        for browser, result in results.items():
            assert result["success"], f"Mobile responsiveness test failed in {browser}: {result['error']}"
            
            mobile_data = result["result"]
            assert not mobile_data["horizontal_scroll"], \
                f"Horizontal scrolling detected in {browser} mobile view"
            
            assert mobile_data["small_buttons_count"] < 3, \
                f"Too many small buttons in {browser} mobile view: {mobile_data['small_buttons_count']}"
    
    @pytest.mark.browser
    @pytest.mark.network
    def test_tablet_responsiveness(self, web_dashboard, cross_browser_tester):
        """Test tablet responsiveness across browsers (FR6)."""
        async def tablet_test(page: Page, browser_name: str) -> Dict:
            # Set tablet viewport
            await page.set_viewport_size(width=768, height=1024)
            await page.goto(f"{cross_browser_tester.config.base_url}/")
            await page.wait_for_load_state("domcontentloaded")
            
            # Check tablet-specific layout
            sidebar = await page.query_selector(".sidebar, .nav-sidebar")
            main_content = await page.query_selector("main, .main-content")
            
            # Check layout structure
            is_two_column = await page.evaluate("""() => {
                const sidebar = document.querySelector('.sidebar, .nav-sidebar');
                const main = document.querySelector('main, .main-content');
                if (sidebar && main) {
                    const sidebarRect = sidebar.getBoundingClientRect();
                    const mainRect = main.getBoundingClientRect();
                    return sidebarRect.left < mainRect.left;
                }
                return false;
            }""")
            
            return {
                "has_sidebar": sidebar is not None,
                "has_main_content": main_content is not None,
                "is_two_column_layout": is_two_column
            }
        
        results = asyncio.run(cross_browser_tester.run_cross_browser_test(tablet_test))
        
        # Verify tablet layout
        for browser, result in results.items():
            assert result["success"], f"Tablet responsiveness test failed in {browser}: {result['error']}"
            
            tablet_data = result["result"]
            assert tablet_data["has_main_content"], f"No main content area in {browser} tablet view"


class TestAccessibilityCompatibility:
    """Test accessibility compliance across browsers."""
    
    @pytest.mark.browser
    @pytest.mark.network
    def test_keyboard_navigation_compatibility(self, web_dashboard, cross_browser_tester):
        """Test keyboard navigation across browsers (NFR6)."""
        async def keyboard_test(page: Page, browser_name: str) -> Dict:
            await page.goto(f"{cross_browser_tester.config.base_url}/")
            await page.wait_for_load_state("domcontentloaded")
            
            # Test tab navigation
            focusable_elements = await page.evaluate("""() => {
                const focusable = document.querySelectorAll(
                    'a[href], button, input, select, textarea, [tabindex]:not([tabindex="-1"])'
                );
                return focusable.length;
            }""")
            
            # Test tab sequence
            tab_sequence = []
            for i in range(min(10, focusable_elements)):  # Test first 10 elements
                await page.keyboard.press("Tab")
                focused_element = await page.evaluate("() => { return document.activeElement.tagName.toLowerCase(); }")
                tab_sequence.append(focused_element)
            
            # Test escape key handling
            await page.keyboard.press("Escape")
            
            return {
                "focusable_elements": focusable_elements,
                "tab_sequence": tab_sequence,
                "tab_sequence_length": len(set(tab_sequence))
            }
        
        results = asyncio.run(cross_browser_tester.run_cross_browser_test(keyboard_test))
        
        # Verify keyboard navigation
        for browser, result in results.items():
            assert result["success"], f"Keyboard navigation test failed in {browser}: {result['error']}"
            
            keyboard_data = result["result"]
            assert keyboard_data["focusable_elements"] > 0, f"No focusable elements in {browser}"
            assert keyboard_data["tab_sequence_length"] > 1, f"Tab navigation not working in {browser}"
    
    @pytest.mark.browser
    @pytest.mark.network
    def test_screen_reader_compatibility(self, web_dashboard, cross_browser_tester):
        """Test screen reader compatibility across browsers (NFR6)."""
        async def screen_reader_test(page: Page, browser_name: str) -> Dict:
            await page.goto(f"{cross_browser_tester.config.base_url}/")
            await page.wait_for_load_state("domcontentloaded")
            
            # Check for semantic HTML
            headings = await page.query_selector_all("h1, h2, h3, h4, h5, h6")
            landmarks = await page.query_selector_all("main, nav, aside, section, article")
            
            # Check for alt text on images
            images = await page.query_selector_all("img")
            images_with_alt = 0
            
            for img in images:
                alt_text = await img.get_attribute("alt")
                if alt_text:
                    images_with_alt += 1
            
            # Check for aria labels
            aria_labeled_elements = await page.query_selector_all("[aria-label], [aria-labelledby]")
            
            # Check for form labels
            form_inputs = await page.query_selector_all("input, select, textarea")
            labeled_inputs = 0
            
            for input_elem in form_inputs:
                label = await page.query_selector(f"label[for='{await input_elem.get_attribute('id')}']")
                aria_label = await input_elem.get_attribute("aria-label")
                if label or aria_label:
                    labeled_inputs += 1
            
            return {
                "headings_count": len(headings),
                "landmarks_count": len(landmarks),
                "images_total": len(images),
                "images_with_alt": images_with_alt,
                "aria_labeled_elements": len(aria_labeled_elements),
                "form_inputs_total": len(form_inputs),
                "labeled_inputs": labeled_inputs
            }
        
        results = asyncio.run(cross_browser_tester.run_cross_browser_test(screen_reader_test))
        
        # Verify screen reader compatibility
        for browser, result in results.items():
            assert result["success"], f"Screen reader test failed in {browser}: {result['error']}"
            
            sr_data = result["result"]
            assert sr_data["headings_count"] > 0, f"No heading structure in {browser}"
            assert sr_data["landmarks_count"] > 0, f"No semantic landmarks in {browser}"
            
            if sr_data["images_total"] > 0:
                alt_ratio = sr_data["images_with_alt"] / sr_data["images_total"]
                assert alt_ratio >= 0.8, f"Too many images without alt text in {browser}: {alt_ratio:.1%}"
            
            if sr_data["form_inputs_total"] > 0:
                label_ratio = sr_data["labeled_inputs"] / sr_data["form_inputs_total"]
                assert label_ratio >= 0.9, f"Too many unlabeled form inputs in {browser}: {label_ratio:.1%}"


class TestBrowserPerformance:
    """Test performance consistency across browsers."""
    
    @pytest.mark.browser
    @pytest.mark.network
    @pytest.mark.performance
    def test_load_performance_consistency(self, web_dashboard, cross_browser_tester):
        """Test load performance consistency across browsers (NFR1)."""
        async def performance_test(page: Page, browser_name: str) -> Dict:
            start_time = time.time()
            
            # Navigate and measure total load time
            await page.goto(f"{cross_browser_tester.config.base_url}/")
            await page.wait_for_load_state("networkidle")
            
            total_load_time = (time.time() - start_time) * 1000
            
            # Get performance metrics
            metrics = await page.evaluate("""() => {
                const perfData = performance.getEntriesByType('navigation')[0];
                return {
                    domContentLoaded: perfData.domContentLoadedEventEnd - perfData.domContentLoadedEventStart,
                    loadComplete: perfData.loadEventEnd - perfData.loadEventStart,
                    firstPaint: performance.getEntriesByType('paint').find(p => p.name === 'first-paint')?.startTime || 0,
                    firstContentfulPaint: performance.getEntriesByType('paint').find(p => p.name === 'first-contentful-paint')?.startTime || 0
                };
            }""")
            
            return {
                "total_load_time_ms": total_load_time,
                "dom_content_loaded_ms": metrics["domContentLoaded"],
                "load_complete_ms": metrics["loadComplete"],
                "first_paint_ms": metrics["firstPaint"],
                "first_contentful_paint_ms": metrics["firstContentfulPaint"]
            }
        
        results = asyncio.run(cross_browser_tester.run_cross_browser_test(performance_test))
        
        # Analyze performance consistency
        load_times = []
        for browser, result in results.items():
            assert result["success"], f"Performance test failed in {browser}: {result['error']}"
            
            perf_data = result["result"]
            load_times.append(perf_data["total_load_time_ms"])
            
            # Individual browser performance assertions
            assert perf_data["total_load_time_ms"] < 10000, \
                f"Load time too slow in {browser}: {perf_data['total_load_time_ms']:.1f}ms"
            
            assert perf_data["first_contentful_paint_ms"] < 3000, \
                f"First contentful paint too slow in {browser}: {perf_data['first_contentful_paint_ms']:.1f}ms"
        
        # Cross-browser consistency check
        if len(load_times) > 1:
            max_load_time = max(load_times)
            min_load_time = min(load_times)
            variance_ratio = (max_load_time - min_load_time) / min_load_time
            
            assert variance_ratio < 1.0, \
                f"Load time variance too high across browsers: {variance_ratio:.1%}"


if __name__ == "__main__":
    # Run browser compatibility tests
    pytest.main([__file__, "-v", "-m", "browser", "--tb=short"])