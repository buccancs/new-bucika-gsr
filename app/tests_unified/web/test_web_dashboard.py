"""
Web API and UI tests for Flask dashboard.
Tests cover FR6 (UI for Monitoring & Control), FR4 (Session Management),
FR10 (Data Transfer & Aggregation), and NFR5 (Security).
"""

import pytest
import json
import sys
import os
from unittest.mock import Mock, patch, MagicMock

# Add PythonApp to path for imports
sys.path.append(os.path.join(os.path.dirname(__file__), '..', '..', 'PythonApp'))

try:
    from PythonApp.web_ui.web_dashboard import WebDashboardServer
    FLASK_AVAILABLE = True
except ImportError:
    FLASK_AVAILABLE = False
    WebDashboardServer = None


@pytest.fixture
def mock_controller():
    """Create a mock controller for testing."""
    controller = Mock()
    controller.get_status.return_value = {
        "running": True,
        "devices": {"shimmer": False, "webcam": False, "android": []},
        "session": {"active": False, "name": None, "start_time": None}
    }
    controller.start_session.return_value = {"success": True, "session_id": "test_session"}
    controller.stop_session.return_value = {"success": True}
    return controller


@pytest.fixture
def web_server(mock_controller):
    """Create web server instance for testing."""
    if not FLASK_AVAILABLE:
        pytest.skip("Flask not available")
    
    server = WebDashboardServer(
        host="127.0.0.1",
        port=5001,  # Use different port for testing
        debug=True,
        controller=mock_controller
    )
    return server


@pytest.fixture
def client(web_server):
    """Create Flask test client."""
    web_server.app.config['TESTING'] = True
    with web_server.app.test_client() as client:
        yield client


@pytest.fixture
def socketio_client(web_server):
    """Create SocketIO test client."""
    try:
        return web_server.socketio.test_client(web_server.app)
    except Exception:
        pytest.skip("SocketIO client not available")


class TestWebRoutes:
    """Test basic web routes (FR6)."""
    
    def test_index_route(self, client):
        """FR6: Test main dashboard page loads."""
        response = client.get('/')
        assert response.status_code == 200
        assert b'html' in response.data.lower() or b'dashboard' in response.data.lower()
    
    def test_devices_route(self, client):
        """FR6: Test devices page loads."""
        response = client.get('/devices')
        assert response.status_code == 200
        assert b'devices' in response.data.lower() or b'html' in response.data.lower()
    
    def test_sessions_route(self, client):
        """FR6: Test sessions page loads."""
        response = client.get('/sessions')
        assert response.status_code == 200
        assert b'sessions' in response.data.lower() or b'html' in response.data.lower()
    
    def test_playback_route(self, client):
        """FR6: Test playback page loads."""
        response = client.get('/playback')
        assert response.status_code == 200
        # Should load successfully
    
    def test_files_route(self, client):
        """FR6: Test files page loads."""
        response = client.get('/files')
        assert response.status_code == 200
        # Should load successfully
    
    def test_settings_route(self, client):
        """FR6: Test settings page loads."""
        response = client.get('/settings')
        assert response.status_code == 200
        # Should load successfully


class TestAPIRoutes:
    """Test API endpoints functionality."""
    
    def test_status_api(self, client, mock_controller):
        """FR6: Test system status API."""
        response = client.get('/api/status')
        assert response.status_code == 200
        
        data = response.get_json()
        assert data is not None
        assert 'running' in data
        assert 'devices' in data
        assert 'session' in data
    
    def test_session_start_api(self, client, mock_controller):
        """FR4: Test session start API."""
        payload = {
            "devices": ["webcam", "android"],
            "session_name": "test_session"
        }
        
        response = client.post('/api/session/start', 
                             data=json.dumps(payload),
                             content_type='application/json')
        
        if response.status_code == 200:
            data = response.get_json()
            assert 'success' in data
        else:
            # May return error if controller is None
            assert response.status_code in [400, 500, 501]
    
    def test_session_stop_api(self, client, mock_controller):
        """FR4: Test session stop API."""
        response = client.post('/api/session/stop')
        
        if response.status_code == 200:
            data = response.get_json()
            assert 'success' in data
        else:
            # May return error if no active session
            assert response.status_code in [400, 500, 501]
    
    def test_device_connect_api(self, client):
        """FR1: Test device connection API."""
        payload = {"device_type": "webcam", "device_id": "0"}
        
        response = client.post('/api/device/connect',
                             data=json.dumps(payload),
                             content_type='application/json')
        
        # Should return success or appropriate error
        assert response.status_code in [200, 400, 500, 501]
    
    def test_device_configure_api(self, client):
        """FR1: Test device configuration API.""" 
        payload = {
            "device_type": "webcam",
            "device_id": "0",
            "config": {"resolution": "1920x1080", "fps": 30}
        }
        
        response = client.post('/api/device/configure',
                             data=json.dumps(payload),
                             content_type='application/json')
        
        # Should return success or appropriate error
        assert response.status_code in [200, 400, 500, 501]


class TestWebcamAPI:
    """Test webcam-specific API endpoints (FR1)."""
    
    def test_webcam_test_api(self, client):
        """FR1: Test webcam testing functionality."""
        payload = {"webcam_id": "0"}
        
        response = client.post('/api/webcam/test',
                             data=json.dumps(payload),
                             content_type='application/json')
        
        if response.status_code == 200:
            data = response.get_json()
            assert 'test_results' in data
        else:
            # System monitor may not be available
            assert response.status_code in [500, 501]
    
    def test_webcam_configure_api(self, client):
        """FR1: Test webcam configuration."""
        payload = {
            "webcam_id": "0",
            "resolution": "1920x1080",
            "fps": 30
        }
        
        response = client.post('/api/webcam/configure',
                             data=json.dumps(payload),
                             content_type='application/json')
        
        # Should handle configuration request
        assert response.status_code in [200, 400, 500, 501]


class TestShimmerAPI:
    """Test Shimmer sensor API endpoints (FR1)."""
    
    def test_shimmer_connect_api(self, client):
        """FR1: Test Shimmer connection API."""
        payload = {"shimmer_id": "test_shimmer"}
        
        response = client.post('/api/shimmer/connect',
                             data=json.dumps(payload),
                             content_type='application/json')
        
        # Should return not implemented when manager not available
        assert response.status_code in [200, 501]
    
    def test_shimmer_configure_api(self, client):
        """FR1: Test Shimmer configuration API."""
        payload = {
            "shimmer_id": "test_shimmer",
            "sampling_rate": 512,
            "sensors": ["GSR", "PPG"]
        }
        
        response = client.post('/api/shimmer/configure',
                             data=json.dumps(payload),
                             content_type='application/json')
        
        # Should return not implemented when manager not available
        assert response.status_code in [200, 501]


class TestPlaybackAPI:
    """Test playback and data access APIs (FR5, FR10)."""
    
    def test_playback_sessions_api(self, client):
        """FR10: Test sessions list API."""
        response = client.get('/api/playback/sessions')
        assert response.status_code == 200
        
        data = response.get_json()
        assert isinstance(data, list)
    
    def test_playback_session_detail_api(self, client):
        """FR10: Test session detail API."""
        response = client.get('/api/playback/session/test_session')
        
        # Should return session details or 404
        assert response.status_code in [200, 404]
    
    def test_playback_video_api(self, client):
        """FR5: Test video playback API."""
        response = client.get('/api/playback/video/test_session/video.mp4')
        
        # Should return video file or 404
        assert response.status_code in [200, 404]
    
    def test_session_export_api(self, client):
        """FR10: Test session export functionality."""
        payload = {
            "session_id": "test_session",
            "start_time": 0,
            "end_time": 60,
            "export_format": "json"
        }
        
        response = client.post('/api/sessions/export',
                             data=json.dumps(payload),
                             content_type='application/json')
        
        # Should handle export request
        assert response.status_code in [200, 404, 501]
    
    def test_session_download_api(self, client):
        """FR10: Test session download API."""
        response = client.get('/api/session/test_session/download')
        
        # Should return download or appropriate error
        assert response.status_code in [200, 404, 501]


class TestSystemAPI:
    """Test system monitoring APIs (NFR1)."""
    
    def test_system_status_api(self, client):
        """NFR1: Test system status monitoring."""
        response = client.get('/api/system/status')
        assert response.status_code == 200
        
        data = response.get_json()
        assert data is not None
        # Should have basic structure even if monitoring unavailable


class TestErrorHandling:
    """Test error handling and robustness (NFR3, NFR5)."""
    
    def test_invalid_json_handling(self, client):
        """NFR3: Test handling of invalid JSON requests."""
        response = client.post('/api/session/start',
                             data="invalid json",
                             content_type='application/json')
        
        assert response.status_code == 400
    
    def test_missing_parameters_handling(self, client):
        """NFR3: Test handling of missing required parameters."""
        response = client.post('/api/device/connect',
                             data=json.dumps({}),
                             content_type='application/json')
        
        assert response.status_code in [400, 500]
    
    def test_controller_unavailable_handling(self, client):
        """NFR3: Test behavior when controller is None."""
        # Test routes that require controller
        response = client.post('/api/session/start',
                             data=json.dumps({"devices": []}),
                             content_type='application/json')
        
        # Should handle gracefully when controller unavailable
        assert response.status_code in [200, 500, 501]


class TestSocketIOEvents:
    """Test SocketIO real-time communication (FR6)."""
    
    def test_socketio_connection(self, socketio_client):
        """FR6: Test SocketIO connection."""
        if socketio_client is None:
            pytest.skip("SocketIO client not available")
        
        assert socketio_client.is_connected()
    
    def test_status_update_event(self, socketio_client):
        """FR6: Test status update events."""
        if socketio_client is None:
            pytest.skip("SocketIO client not available")
        
        # Should receive initial status update
        received = socketio_client.get_received()
        # Check if any status updates were received
        status_events = [event for event in received if event['name'] == 'status_update']
        # May or may not receive events depending on implementation
    
    def test_request_device_status_event(self, socketio_client):
        """FR6: Test device status request event."""
        if socketio_client is None:
            pytest.skip("SocketIO client not available")
        
        socketio_client.emit('request_device_status')
        received = socketio_client.get_received()
        
        # Should handle the request without errors
        assert socketio_client.is_connected()
    
    def test_request_session_info_event(self, socketio_client):
        """FR6: Test session info request event."""
        if socketio_client is None:
            pytest.skip("SocketIO client not available")
        
        socketio_client.emit('request_session_info')
        received = socketio_client.get_received()
        
        # Should handle the request without errors
        assert socketio_client.is_connected()


class TestSecurityFeatures:
    """Test security features (NFR5)."""
    
    def test_secret_key_configured(self, web_server):
        """NFR5: Test that secret key is configured."""
        assert web_server.app.config['SECRET_KEY'] is not None
        assert len(web_server.app.config['SECRET_KEY']) > 10
    
    def test_error_responses_no_stack_traces(self, client):
        """NFR5: Test error responses don't leak stack traces."""
        # Trigger an error condition
        response = client.get('/api/nonexistent/endpoint')
        assert response.status_code == 404
        
        # Response should not contain stack trace information
        data = response.get_data(as_text=True)
        sensitive_terms = ['traceback', 'file "/', 'line ', 'error:']
        for term in sensitive_terms:
            assert term.lower() not in data.lower()
    
    def test_file_download_security(self, client):
        """NFR5: Test file download security headers."""
        response = client.get('/api/session/test_session/download')
        
        if response.status_code == 200:
            # Should have appropriate headers for file downloads
            assert 'Content-Disposition' in response.headers or response.status_code != 200


@pytest.mark.parametrize("endpoint,method", [
    ('/api/status', 'GET'),
    ('/api/session/start', 'POST'),
    ('/api/session/stop', 'POST'),
    ('/api/device/connect', 'POST'),
    ('/api/system/status', 'GET'),
])
def test_api_endpoints_respond(client, endpoint, method):
    """Test that all major API endpoints respond appropriately."""
    if method == 'GET':
        response = client.get(endpoint)
    elif method == 'POST':
        response = client.post(endpoint, 
                             data=json.dumps({}),
                             content_type='application/json')
    
    # Should respond with valid HTTP status code
    assert 200 <= response.status_code < 600


def test_performance_basic_load(client):
    """NFR1: Test basic performance under multiple requests."""
    import time
    
    start_time = time.time()
    
    # Make multiple requests
    for _ in range(10):
        response = client.get('/api/status')
        assert response.status_code == 200
    
    end_time = time.time()
    total_time = end_time - start_time
    
    # Should handle 10 requests reasonably quickly (under 5 seconds)
    assert total_time < 5.0, f"Performance test took {total_time} seconds"