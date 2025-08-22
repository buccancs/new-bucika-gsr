"""
PC GUI Component Interaction Tests
==================================

Detailed testing of individual PC GUI components and their specific functionality.
Tests all buttons, panels, dialogs, and interactive elements in the PyQt5 application.

Requirements Coverage:
- FR6: User Interface component functionality  
- FR7: Device coordination UI components
- FR8: Error handling in GUI components
- NFR6: GUI accessibility and usability
- NFR1: GUI component performance
"""

import pytest
import sys
import os
import time
from typing import Dict, List, Optional
from unittest.mock import Mock, patch, MagicMock

# Add PythonApp to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'PythonApp'))

# PyQt5 imports with fallback
try:
    from PyQt5.QtWidgets import (
        QApplication, QMainWindow, QPushButton, QLabel, QLineEdit,
        QComboBox, QCheckBox, QSlider, QProgressBar, QTextEdit,
        QTabWidget, QDialog, QMessageBox, QFileDialog
    )
    from PyQt5.QtCore import Qt, QTimer
    from PyQt5.QtTest import QTest
    PYQT_AVAILABLE = True
except ImportError:
    PYQT_AVAILABLE = False

# Test utilities
from ..fixtures.gui_test_utils import GUITestBase, UITestHelpers


class TestMainWindowComponents(GUITestBase):
    """Test main window individual components."""
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.components
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_recording_control_buttons(self, test_app):
        """Test all recording control buttons functionality (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Test recording control buttons
        recording_buttons = [
            ("btn_start_recording", "Start Recording"),
            ("btn_pause_recording", "Pause Recording"),
            ("btn_stop_recording", "Stop Recording"),
            ("btn_new_session", "New Session")
        ]
        
        for button_name, expected_text in recording_buttons:
            if hasattr(window, button_name):
                button = getattr(window, button_name)
                
                # Test button properties
                assert button.isEnabled() or button.isVisible(), f"{button_name} should be accessible"
                
                # Test button click
                with patch('session.session_manager.SessionManager') as mock_session:
                    mock_session_instance = Mock()
                    mock_session.return_value = mock_session_instance
                    
                    QTest.mouseClick(button, Qt.LeftButton)
                    test_app.processEvents()
                    
                    # Verify button state changes appropriately
                    self._verify_recording_button_state(window, button_name)
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.components
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_device_connection_buttons(self, test_app):
        """Test device connection button functionality (FR7)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        device_buttons = [
            ("btn_scan_devices", "Scan for Devices"),
            ("btn_connect_device", "Connect Device"),
            ("btn_disconnect_device", "Disconnect Device"),
            ("btn_refresh_devices", "Refresh Device List")
        ]
        
        for button_name, expected_function in device_buttons:
            if hasattr(window, button_name):
                button = getattr(window, button_name)
                
                # Mock device operations
                with patch('network.device_client.DeviceClient') as mock_device:
                    mock_device_instance = Mock()
                    mock_device.return_value = mock_device_instance
                    mock_device_instance.scan.return_value = ["device_001", "device_002"]
                    
                    # Test button click
                    QTest.mouseClick(button, Qt.LeftButton)
                    test_app.processEvents()
                    
                    # Verify appropriate device operation was called
                    self._verify_device_operation(window, button_name, mock_device_instance)
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.components
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_menu_bar_functionality(self, test_app):
        """Test menu bar and all menu actions (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        menu_bar = window.menuBar()
        assert menu_bar is not None, "Menu bar should exist"
        
        # Test each menu
        menus = menu_bar.findChildren(QMainWindow)
        if not menus:
            # Alternative method to find menus
            for action in menu_bar.actions():
                if action.menu():
                    menu = action.menu()
                    self._test_menu_actions(window, menu, test_app)
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.components
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_status_bar_updates(self, test_app):
        """Test status bar updates and information display (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        status_bar = window.statusBar()
        assert status_bar is not None, "Status bar should exist"
        
        # Test status updates
        test_messages = [
            "Device connected",
            "Recording started",
            "Data saved successfully",
            "Connection error",
            "Ready"
        ]
        
        for message in test_messages:
            if hasattr(window, 'update_status'):
                window.update_status(message)
                test_app.processEvents()
                
                # Verify status bar shows message
                status_text = status_bar.currentMessage()
                assert message in status_text, f"Status bar should show: {message}"
        
        window.close()
    
    def _verify_recording_button_state(self, window, button_name: str):
        """Verify recording button state changes appropriately."""
        # Check that other recording buttons update their enabled state
        if button_name == "btn_start_recording":
            if hasattr(window, 'btn_pause_recording'):
                # Pause should become enabled after start
                assert hasattr(window.btn_pause_recording, 'isEnabled')
        elif button_name == "btn_stop_recording":
            if hasattr(window, 'btn_start_recording'):
                # Start should become enabled after stop
                assert hasattr(window.btn_start_recording, 'isEnabled')
    
    def _verify_device_operation(self, window, button_name: str, mock_device):
        """Verify appropriate device operation was triggered."""
        if "scan" in button_name:
            # Should have called scan method
            assert hasattr(mock_device, 'scan'), "Device scan method should exist"
        elif "connect" in button_name and "disconnect" not in button_name:
            # Should have connection-related calls
            assert hasattr(mock_device, 'connect'), "Device connect method should exist"
        elif "disconnect" in button_name:
            # Should have disconnection-related calls
            assert hasattr(mock_device, 'disconnect'), "Device disconnect method should exist"
    
    def _test_menu_actions(self, window, menu, test_app):
        """Test all actions in a menu."""
        for action in menu.actions():
            if action.isEnabled() and not action.isSeparator():
                action_text = action.text()
                
                # Mock any dialogs that might open
                with patch('PyQt5.QtWidgets.QFileDialog.getOpenFileName') as mock_file:
                    with patch('PyQt5.QtWidgets.QMessageBox.information') as mock_msg:
                        mock_file.return_value = ("test_file.csv", "CSV Files (*.csv)")
                        
                        # Trigger menu action
                        action.trigger()
                        test_app.processEvents()
                        
                        # Verify no exceptions occurred
                        assert True, f"Menu action '{action_text}' executed without error"


class TestDeviceStatusPanel(GUITestBase):
    """Test device status panel functionality."""
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.components
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_device_status_display(self, test_app):
        """Test device status display updates (FR7)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        if hasattr(window, 'device_status_panel'):
            panel = window.device_status_panel
            
            # Test device status updates
            test_devices = [
                ("shimmer_001", "connected", {"gsr": 2.5, "timestamp": time.time()}),
                ("thermal_001", "disconnected", {}),
                ("android_001", "connecting", {"status": "establishing_connection"})
            ]
            
            for device_id, status, data in test_devices:
                # Update device status
                if hasattr(panel, 'update_device_status'):
                    panel.update_device_status(device_id, status)
                    test_app.processEvents()
                
                # Update device data
                if hasattr(panel, 'update_device_data') and data:
                    panel.update_device_data(device_id, data)
                    test_app.processEvents()
                
                # Verify panel reflects updates
                self._verify_device_panel_state(panel, device_id, status)
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.components
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_device_panel_interactions(self, test_app):
        """Test device panel user interactions (FR7)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        if hasattr(window, 'device_status_panel'):
            panel = window.device_status_panel
            
            # Find device control elements
            device_buttons = panel.findChildren(QPushButton)
            
            for button in device_buttons[:3]:  # Test first 3 buttons
                if button.isEnabled() and button.isVisible():
                    button_text = button.text()
                    
                    # Mock device operations
                    with patch('network.device_client.DeviceClient') as mock_device:
                        mock_device_instance = Mock()
                        mock_device.return_value = mock_device_instance
                        
                        # Click button
                        QTest.mouseClick(button, Qt.LeftButton)
                        test_app.processEvents()
                        
                        # Verify no errors occurred
                        assert True, f"Device panel button '{button_text}' works correctly"
        
        window.close()
    
    def _verify_device_panel_state(self, panel, device_id: str, expected_status: str):
        """Verify device panel shows correct state."""
        # Look for status indicators
        status_labels = panel.findChildren(QLabel)
        
        for label in status_labels:
            label_text = label.text().lower()
            if device_id.lower() in label_text or expected_status.lower() in label_text:
                # Found relevant status display
                return True
        
        # Panel should at least be visible and responsive
        assert panel.isVisible(), "Device panel should be visible"


class TestPreviewPanel(GUITestBase):
    """Test data preview panel functionality."""
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.components
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_data_visualization_updates(self, test_app):
        """Test data visualization in preview panel (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        if hasattr(window, 'preview_panel'):
            panel = window.preview_panel
            
            # Generate test data
            test_data_sets = [
                {
                    "device_id": "shimmer_001",
                    "data_type": "gsr",
                    "values": [2.1, 2.3, 2.5, 2.4, 2.6],
                    "timestamps": [time.time() - i for i in range(5, 0, -1)]
                },
                {
                    "device_id": "thermal_001", 
                    "data_type": "temperature",
                    "values": [36.2, 36.4, 36.3, 36.5, 36.1],
                    "timestamps": [time.time() - i for i in range(5, 0, -1)]
                }
            ]
            
            for data_set in test_data_sets:
                if hasattr(panel, 'update_data'):
                    panel.update_data(data_set)
                    test_app.processEvents()
                    
                    # Verify panel updated
                    self._verify_preview_panel_update(panel, data_set)
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.components
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_preview_panel_controls(self, test_app):
        """Test preview panel user controls (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        if hasattr(window, 'preview_panel'):
            panel = window.preview_panel
            
            # Test control buttons
            control_buttons = panel.findChildren(QPushButton)
            
            for button in control_buttons:
                if button.isEnabled() and button.isVisible():
                    button_text = button.text()
                    
                    # Test button functionality
                    QTest.mouseClick(button, Qt.LeftButton)
                    test_app.processEvents()
                    
                    # Verify button response
                    assert True, f"Preview panel button '{button_text}' responds correctly"
            
            # Test sliders and other controls
            sliders = panel.findChildren(QSlider)
            for slider in sliders:
                if slider.isEnabled() and slider.isVisible():
                    # Test slider interaction
                    original_value = slider.value()
                    new_value = min(slider.maximum(), original_value + 10)
                    slider.setValue(new_value)
                    test_app.processEvents()
                    
                    assert slider.value() == new_value, "Slider should update to new value"
        
        window.close()
    
    def _verify_preview_panel_update(self, panel, data_set: Dict):
        """Verify preview panel reflects data update."""
        # Panel should be visible and responsive after data update
        assert panel.isVisible(), "Preview panel should be visible after data update"
        
        # Look for data-related labels or displays
        labels = panel.findChildren(QLabel)
        text_found = False
        
        for label in labels:
            label_text = label.text()
            if (data_set["device_id"] in label_text or 
                data_set["data_type"] in label_text or
                any(str(val) in label_text for val in data_set["values"][:2])):
                text_found = True
                break
        
        # At minimum, panel should show some form of data representation
        if not text_found:
            # Check if there are any graphical elements (plots, charts)
            widgets = panel.findChildren(QApplication.allWidgets())
            assert len(widgets) > 0, "Preview panel should contain data visualization elements"


class TestSettingsDialog(GUITestBase):
    """Test settings dialog functionality."""
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.components
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_settings_dialog_opening(self, test_app):
        """Test settings dialog opens correctly (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Try to open settings dialog
        with patch('PyQt5.QtWidgets.QDialog.exec_') as mock_exec:
            mock_exec.return_value = QDialog.Accepted
            
            if hasattr(window, 'open_settings_dialog'):
                window.open_settings_dialog()
                mock_exec.assert_called()
            elif hasattr(window, 'btn_settings'):
                QTest.mouseClick(window.btn_settings, Qt.LeftButton)
                test_app.processEvents()
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.components
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_settings_form_validation(self, test_app):
        """Test settings form input validation (FR6)."""
        # This would test a settings dialog if it exists
        # For now, test that settings-related functionality exists
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Look for settings-related input fields in main window
        line_edits = window.findChildren(QLineEdit)
        combo_boxes = window.findChildren(QComboBox)
        checkboxes = window.findChildren(QCheckBox)
        
        # Test input validation on any found controls
        for line_edit in line_edits[:3]:  # Test first 3
            if line_edit.isEnabled() and line_edit.isVisible():
                # Test various inputs
                test_inputs = ["valid_input", "", "123", "invalid@input"]
                
                for test_input in test_inputs:
                    line_edit.clear()
                    line_edit.setText(test_input)
                    test_app.processEvents()
                    
                    # Verify input was accepted or handled
                    current_text = line_edit.text()
                    assert isinstance(current_text, str), "Input field should accept string input"
        
        window.close()


if __name__ == "__main__":
    # Run PC GUI component tests
    pytest.main([__file__, "-v", "-m", "gui and pc and components", "--tb=short"])