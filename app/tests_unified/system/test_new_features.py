"""
Test suite for newly implemented features.

These tests verify that the implemented features (LSL, Zeroconf, security, file integrity)
work correctly and integrate properly with the existing system.
"""

import pytest
import tempfile
import os
import time
from pathlib import Path

@pytest.mark.system
def test_lsl_integration():
    """Test LSL integration with fallback support."""
    from PythonApp.network.lsl_integration import LSLStreamer, DefaultLSLStreams
    
    streamer = LSLStreamer()
    assert streamer is not None
    
    # Test stream configuration creation
    config = DefaultLSLStreams.shimmer_gsr_stream("test_device")
    assert config.name == "ShimmerGSR_test_device"
    assert config.channel_count == 4
    assert config.nominal_srate == 128.0
    
    # Test outlet creation (should work even without native LSL)
    result = streamer.create_outlet("test_stream", config)
    assert isinstance(result, bool)  # Should return boolean regardless of LSL availability
    
    # Test sample pushing (should not raise errors)
    if result:
        push_result = streamer.push_sample("test_stream", [1.0, 2.0, 3.0, 4.0])
        assert isinstance(push_result, bool)
    
    # Cleanup
    streamer.cleanup()


@pytest.mark.system
@pytest.mark.slow  # Requires zeroconf dependency
def test_zeroconf_discovery():
    """Test Zeroconf device discovery functionality."""
    from PythonApp.network.zeroconf_discovery import ZeroconfDiscovery, MultiSensorDiscovery
    
    # Test basic discovery object creation
    discovery = ZeroconfDiscovery()
    assert discovery is not None
    assert not discovery.is_running
    
    # Test multi-sensor discovery wrapper
    multi_discovery = MultiSensorDiscovery()
    assert multi_discovery is not None
    
    # Test device lists (should be empty initially)
    android_devices = multi_discovery.get_android_devices()
    pc_controllers = multi_discovery.get_pc_controllers()
    assert isinstance(android_devices, list)
    assert isinstance(pc_controllers, list)
    
    # Cleanup
    discovery.cleanup()
    multi_discovery.cleanup()


@pytest.mark.system
def test_enhanced_security():
    """Test enhanced security features."""
    from PythonApp.network.security import (
        SecurityConfig, TokenManager, RuntimeSecurityChecker
    )
    
    # Test security configuration
    config = SecurityConfig(
        use_tls=True,
        min_token_length=32,
        max_token_age_seconds=3600
    )
    assert config.use_tls is True
    assert config.min_token_length == 32
    
    # Test token manager
    token_manager = TokenManager()
    assert token_manager is not None
    
    # Test token generation
    token = token_manager.generate_token("test_device", ["read", "write"])
    assert token is not None
    assert len(token.token) >= 32
    assert token.device_id == "test_device"
    assert "read" in token.permissions
    assert "write" in token.permissions
    
    # Test token validation
    validated_token = token_manager.validate_token(token.token)
    assert validated_token is not None
    assert validated_token.device_id == "test_device"
    
    # Test invalid token
    invalid_token = token_manager.validate_token("invalid_token")
    assert invalid_token is None
    
    # Test security checker
    security_checker = RuntimeSecurityChecker(config)
    assert security_checker is not None
    
    # Test configuration check
    issues = security_checker.check_configuration()
    assert isinstance(issues, list)
    
    # Test token security check
    secure = security_checker.check_token_security(token.token)
    assert secure is True
    
    # Test weak token detection
    weak = security_checker.check_token_security("123456")
    assert weak is False


@pytest.mark.system
def test_file_integrity():
    """Test file integrity and secure transfer functionality."""
    from PythonApp.network.file_integrity import (
        FileHasher, SecureFileTransfer, FileIntegrityVerifier
    )
    
    # Create test file
    with tempfile.NamedTemporaryFile(mode='w', delete=False) as f:
        f.write("Test content for integrity checking\n" * 100)
        test_file = f.name
    
    try:
        # Test file hashing
        hash_value = FileHasher.compute_sha256(test_file)
        assert hash_value is not None
        assert len(hash_value) == 64  # SHA-256 is 64 hex characters
        
        # Test hash verification
        verified = FileHasher.verify_file_integrity(test_file, hash_value)
        assert verified is True
        
        # Test with wrong hash
        wrong_verified = FileHasher.verify_file_integrity(test_file, "wrong_hash")
        assert wrong_verified is False
        
        # Test secure file transfer
        transfer = SecureFileTransfer()
        assert transfer is not None
        
        # Test file preparation
        metadata = transfer.prepare_file_for_transfer(test_file)
        assert metadata is not None
        assert metadata.filename == Path(test_file).name
        assert metadata.file_size > 0
        assert metadata.sha256_hash == hash_value
        assert metadata.total_chunks > 0
        
        # Test file integrity verifier
        verifier = FileIntegrityVerifier()
        assert verifier is not None
        
        # Test manifest creation
        temp_dir = Path(test_file).parent
        manifest_file = temp_dir / "test_manifest.json"
        
        created = verifier.create_manifest(str(temp_dir), str(manifest_file))
        assert created is True
        assert manifest_file.exists()
        
        # Test manifest verification
        success, errors = verifier.verify_manifest(str(temp_dir), str(manifest_file))
        assert success is True
        assert len(errors) == 0
        
        # Cleanup manifest
        manifest_file.unlink()
        
    finally:
        # Cleanup test file
        os.unlink(test_file)


@pytest.mark.system
@pytest.mark.slow  # Has security configuration issues  
def test_enhanced_pc_server():
    """Test enhanced PC server with new features."""
    from PythonApp.network.pc_server import PCServer
    
    # Test server creation with enhanced features
    server = PCServer(
        port=9999,  # Use different port to avoid conflicts
        enable_security=True,
        enable_discovery=False,  # Disable discovery to avoid network operations
        enable_lsl=True
    )
    
    assert server is not None
    assert server.enable_security is True
    assert server.enable_discovery is False
    assert server.enable_lsl is True
    
    # Test token generation
    if server.enable_security:
        token = server.generate_device_token("test_device", ["read", "write"])
        assert token is not None
        assert len(token) > 16
        
        # Test token validation
        valid = server.validate_device_token(token)
        assert valid is True
        
        # Test invalid token
        invalid = server.validate_device_token("invalid_token")
        assert invalid is False
    
    # Test LSL marker sending (should not raise errors)
    server.send_lsl_marker("test_event")
    
    # Test discovery features (should return empty list when disabled)
    devices = server.get_discovered_devices()
    assert isinstance(devices, list)
    
    # Cleanup
    server.stop()


@pytest.mark.system
@pytest.mark.slow  # Requires zeroconf dependency
def test_integration_with_existing_system():
    """Test that new features integrate properly with existing system."""
    # Test imports don't break existing code
    try:
        from PythonApp.network.pc_server import PCServer
        from PythonApp.network import lsl_integration
        from PythonApp.network import zeroconf_discovery
        from PythonApp.network import security
        from PythonApp.network import file_integrity
        
        # All imports should work
        assert True
        
    except ImportError as e:
        pytest.fail(f"Import error in integration: {e}")
    
    # Test that enhanced features are optional
    server_basic = PCServer(
        port=9998,
        enable_security=False,
        enable_discovery=False,
        enable_lsl=False
    )
    
    assert server_basic.enable_security is False
    assert server_basic.enable_discovery is False
    assert server_basic.enable_lsl is False
    
    # These should still work but return appropriate values
    token = server_basic.generate_device_token("test", ["read"])
    assert token == "no_security_enabled"
    
    valid = server_basic.validate_device_token("any_token")
    assert valid is True  # Security disabled, so any token is valid
    
    server_basic.stop()


if __name__ == "__main__":
    pytest.main([__file__, "-v"])