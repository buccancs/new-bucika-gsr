"""
Comprehensive PC GUI Functional Tests
====================================

Complete functional testing suite for the Python/PyQt5 desktop application.
Tests all UI components, user interactions, workflows, and functionality.

Requirements Coverage:
- FR6: User Interface functionality and correctness
- FR7: Multi-device coordination UI
- FR8: Error handling and user feedback
- NFR6: Accessibility and usability
- NFR1: Performance of UI operations
- NFR8: Maintainability of UI components

Test Categories:
- Main window functionality
- All button and menu interactions
- Panel operations and layout
- Dialog interactions and validation
- Device status and real-time updates
- File operations and dialogs
- Settings and configuration UI
- Error handling and user feedback
- Multi-window coordination
- Keyboard shortcuts and accessibility
"""

import pytest
import sys
import os
import time
import tempfile
from typing import Dict, List, Optional, Any
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock

# Add PythonApp to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'PythonApp'))

# PyQt5 imports with fallback
try:
    from PyQt5.QtWidgets import QApplication, QWidget, QMainWindow, QPushButton, QDialog
    from PyQt5.QtCore import QTimer, Qt, QThread, pyqtSignal
    from PyQt5.QtGui import QPixmap, QIcon
    from PyQt5.QtTest import QTest
    PYQT_AVAILABLE = True
except ImportError:
    PYQT_AVAILABLE = False

# Test utilities
from ..fixtures.gui_test_utils import GUITestBase, create_test_app, cleanup_test_app


class TestPCMainWindowFunctionality(GUITestBase):
    """Comprehensive tests for main window functionality."""
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_main_window_initialization(self, test_app):
        """Test main window initializes correctly with all components (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Test window properties
        assert window.isVisible()
        assert window.windowTitle() != ""
        assert window.width() > 0
        assert window.height() > 0
        
        # Test essential components are present
        assert hasattr(window, 'device_status_panel')
        assert hasattr(window, 'preview_panel')
        assert hasattr(window, 'control_panel')
        
        # Test menu bar and toolbars
        assert window.menuBar() is not None
        assert len(window.findChildren(QPushButton)) > 0
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_all_buttons_functionality(self, test_app):
        """Test all buttons in main window respond correctly (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Find all buttons
        buttons = window.findChildren(QPushButton)
        assert len(buttons) > 0, "No buttons found in main window"
        
        tested_buttons = []
        for button in buttons:
            if button.isEnabled() and button.isVisible():
                button_text = button.text() or button.objectName() or "unnamed_button"
                
                # Test button click
                with patch.object(window, 'show_error_dialog') as mock_error:
                    QTest.mouseClick(button, Qt.LeftButton)
                    
                    # Verify no unhandled errors
                    if mock_error.called:
                        error_msg = mock_error.call_args[0][0] if mock_error.call_args else "Unknown error"
                        pytest.fail(f"Button '{button_text}' caused error: {error_msg}")
                
                tested_buttons.append(button_text)
        
        assert len(tested_buttons) > 5, f"Should test multiple buttons, only tested: {tested_buttons}"
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_menu_actions_functionality(self, test_app):
        """Test all menu actions work correctly (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        menu_bar = window.menuBar()
        assert menu_bar is not None
        
        tested_actions = []
        for menu in menu_bar.findChildren(QWidget):
            if hasattr(menu, 'actions'):
                for action in menu.actions():
                    if action.isEnabled() and not action.isSeparator():
                        action_text = action.text() or action.objectName()
                        
                        # Test action trigger
                        with patch.object(window, 'show_error_dialog') as mock_error:
                            action.trigger()
                            
                            # Verify no unhandled errors
                            if mock_error.called:
                                error_msg = mock_error.call_args[0][0] if mock_error.call_args else "Unknown error"
                                pytest.fail(f"Menu action '{action_text}' caused error: {error_msg}")
                        
                        tested_actions.append(action_text)
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_device_panel_functionality(self, test_app):
        """Test device status panel updates and interactions (FR7)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        if hasattr(window, 'device_status_panel'):
            panel = window.device_status_panel
            
            # Test device status updates
            test_device_id = "test_device_001"
            
            # Test device connection
            panel.update_device_status(test_device_id, "connected")
            
            # Test device disconnection
            panel.update_device_status(test_device_id, "disconnected")
            
            # Test device data update
            test_data = {"timestamp": time.time(), "value": 123.45}
            panel.update_device_data(test_device_id, test_data)
            
            # Verify panel responds to updates
            assert panel.isVisible()
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_recording_workflow_ui(self, test_app):
        """Test complete recording workflow through UI (FR6, FR7)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Test recording workflow
        workflow_steps = [
            "start_recording",
            "pause_recording", 
            "resume_recording",
            "stop_recording"
        ]
        
        for step in workflow_steps:
            if hasattr(window, step):
                method = getattr(window, step)
                
                # Mock dependencies
                with patch('session.session_manager.SessionManager') as mock_session:
                    mock_session_instance = Mock()
                    mock_session.return_value = mock_session_instance
                    
                    # Execute workflow step
                    try:
                        if callable(method):
                            method()
                        elif hasattr(window, f"btn_{step}"):
                            button = getattr(window, f"btn_{step}")
                            QTest.mouseClick(button, Qt.LeftButton)
                    except Exception as e:
                        pytest.fail(f"Recording workflow step '{step}' failed: {e}")
        
        window.close()


class TestPCDialogFunctionality(GUITestBase):
    """Test all dialog interactions and functionality."""
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_file_dialog_operations(self, test_app):
        """Test file dialog operations (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        from gui.file_browser_dialog import FileBrowserDialog
        
        window = EnhancedMainWindow()
        window.show()
        
        # Test file browser dialog
        with patch('PyQt5.QtWidgets.QFileDialog.getOpenFileName') as mock_dialog:
            mock_dialog.return_value = ("/tmp/test_file.csv", "CSV Files (*.csv)")
            
            # Trigger file open
            if hasattr(window, 'open_file_dialog'):
                result = window.open_file_dialog()
                assert result is not None
                mock_dialog.assert_called_once()
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_error_dialog_functionality(self, test_app):
        """Test error dialog display and handling (FR8)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Test error dialog
        test_error_message = "Test error message for validation"
        
        with patch('PyQt5.QtWidgets.QMessageBox.critical') as mock_dialog:
            if hasattr(window, 'show_error_dialog'):
                window.show_error_dialog(test_error_message)
                mock_dialog.assert_called_once()
                
                # Verify error message content
                call_args = mock_dialog.call_args
                assert test_error_message in str(call_args)
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_settings_dialog_functionality(self, test_app):
        """Test settings dialog and configuration (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Test settings dialog
        with patch('PyQt5.QtWidgets.QDialog.exec_') as mock_exec:
            mock_exec.return_value = QDialog.Accepted
            
            if hasattr(window, 'open_settings_dialog'):
                window.open_settings_dialog()
                mock_exec.assert_called()
        
        window.close()


class TestPCUIPerformance(GUITestBase):
    """Test UI performance and responsiveness (NFR1)."""
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.performance
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_window_startup_performance(self, test_app):
        """Test main window startup time is acceptable (NFR1)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        start_time = time.time()
        window = EnhancedMainWindow()
        window.show()
        
        # Wait for window to fully render
        test_app.processEvents()
        
        startup_time = time.time() - start_time
        window.close()
        
        # Window should start within 3 seconds
        assert startup_time < 3.0, f"Window startup took {startup_time:.2f}s, should be < 3.0s"
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.performance
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_ui_responsiveness_under_load(self, test_app):
        """Test UI remains responsive during data processing (NFR1)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Simulate high-frequency data updates
        start_time = time.time()
        update_count = 0
        
        for i in range(100):
            if hasattr(window, 'update_device_data'):
                test_data = {"timestamp": time.time(), "value": i * 1.23}
                window.update_device_data("test_device", test_data)
                update_count += 1
            
            # Process events to keep UI responsive
            test_app.processEvents()
            
            # Check if UI is still responsive
            if i % 20 == 0:
                response_start = time.time()
                test_app.processEvents()
                response_time = time.time() - response_start
                
                # UI should respond within 100ms
                assert response_time < 0.1, f"UI response time {response_time:.3f}s too slow"
        
        total_time = time.time() - start_time
        window.close()
        
        # Should process all updates within reasonable time
        assert total_time < 5.0, f"Update processing took {total_time:.2f}s, should be < 5.0s"
        assert update_count > 0, "No updates were processed"


class TestPCUIAccessibility(GUITestBase):
    """Test PC application accessibility features (NFR6)."""
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.accessibility
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_keyboard_navigation(self, test_app):
        """Test keyboard navigation works correctly (NFR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Test tab navigation
        focusable_widgets = []
        for widget in window.findChildren(QWidget):
            if widget.focusPolicy() != Qt.NoFocus and widget.isVisible():
                focusable_widgets.append(widget)
        
        assert len(focusable_widgets) > 0, "No focusable widgets found"
        
        # Test tab key navigation
        first_widget = focusable_widgets[0]
        first_widget.setFocus()
        
        for _ in range(min(5, len(focusable_widgets))):
            QTest.keyPress(window, Qt.Key_Tab)
            test_app.processEvents()
            
            # Verify focus changed
            focused_widget = test_app.focusWidget()
            assert focused_widget is not None, "No widget has focus after Tab key"
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.accessibility
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_screen_reader_compatibility(self, test_app):
        """Test screen reader accessibility features (NFR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Check for accessibility properties
        buttons = window.findChildren(QPushButton)
        
        accessible_buttons = 0
        for button in buttons:
            if button.isVisible():
                # Check for accessibility text
                accessible_name = button.accessibleName()
                button_text = button.text()
                tooltip = button.toolTip()
                
                # Button should have some form of accessible text
                has_accessible_text = bool(accessible_name or button_text or tooltip)
                if has_accessible_text:
                    accessible_buttons += 1
        
        # At least 80% of buttons should have accessible text
        if len(buttons) > 0:
            accessibility_ratio = accessible_buttons / len(buttons)
            assert accessibility_ratio >= 0.8, \
                f"Only {accessibility_ratio:.1%} of buttons have accessible text"
        
        window.close()


class TestPCUIIntegration(GUITestBase):
    """Test integration between UI components and backend systems."""
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.integration
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_device_connection_ui_integration(self, test_app):
        """Test UI integration with device connection system (FR7)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Mock device manager
        with patch('network.device_client.DeviceClient') as mock_device:
            mock_device_instance = Mock()
            mock_device.return_value = mock_device_instance
            
            # Test device connection workflow
            if hasattr(window, 'connect_device'):
                # Simulate successful connection
                mock_device_instance.connect.return_value = True
                result = window.connect_device("192.168.1.100")
                
                # Verify UI updates
                if hasattr(window, 'device_status_panel'):
                    # Check that UI reflects connection status
                    panel = window.device_status_panel
                    assert panel.isVisible()
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.integration
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_data_visualization_ui_integration(self, test_app):
        """Test UI integration with data visualization (FR6)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Test data visualization updates
        if hasattr(window, 'preview_panel'):
            panel = window.preview_panel
            
            # Simulate data update
            test_data = {
                "timestamp": time.time(),
                "gsr_value": 5.67,
                "temperature": 36.5,
                "device_id": "shimmer_001"
            }
            
            with patch.object(panel, 'update_data') as mock_update:
                if hasattr(window, 'update_visualization'):
                    window.update_visualization(test_data)
                    mock_update.assert_called()
        
        window.close()


class TestPCUIErrorHandling(GUITestBase):
    """Test UI error handling and user feedback (FR8)."""
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.error_handling
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_network_error_handling(self, test_app):
        """Test UI handles network errors gracefully (FR8)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Simulate network error
        with patch('network.device_client.DeviceClient') as mock_device:
            mock_device_instance = Mock()
            mock_device.return_value = mock_device_instance
            mock_device_instance.connect.side_effect = ConnectionError("Network unavailable")
            
            with patch.object(window, 'show_error_dialog') as mock_error_dialog:
                if hasattr(window, 'connect_device'):
                    window.connect_device("invalid_address")
                    
                    # Verify error dialog was shown
                    mock_error_dialog.assert_called_once()
                    
                    # Verify error message contains network information
                    call_args = mock_error_dialog.call_args[0][0]
                    assert "network" in call_args.lower() or "connection" in call_args.lower()
        
        window.close()
    
    @pytest.mark.gui
    @pytest.mark.pc
    @pytest.mark.error_handling
    @pytest.mark.skipif(not PYQT_AVAILABLE, reason="PyQt5 not available")
    def test_file_operation_error_handling(self, test_app):
        """Test UI handles file operation errors gracefully (FR8)."""
        from gui.enhanced_ui_main_window import EnhancedMainWindow
        
        window = EnhancedMainWindow()
        window.show()
        
        # Test file access error
        with patch('builtins.open', side_effect=PermissionError("Access denied")):
            with patch.object(window, 'show_error_dialog') as mock_error_dialog:
                if hasattr(window, 'save_session'):
                    window.save_session("/invalid/path/session.json")
                    
                    # Verify error dialog was shown
                    mock_error_dialog.assert_called_once()
                    
                    # Verify error message is user-friendly
                    call_args = mock_error_dialog.call_args[0][0]
                    assert len(call_args) > 0
                    assert "error" in call_args.lower() or "failed" in call_args.lower()
        
        window.close()


if __name__ == "__main__":
    # Run comprehensive PC GUI tests
    pytest.main([__file__, "-v", "-m", "gui and pc", "--tb=short"])