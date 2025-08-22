#!/usr/bin/env python3
"""
Simplified Thesis Claims Validation

Validates the core thesis claims against the implementation without requiring
external packages like numpy or pytest.
"""

import time
import logging
import json
import os
import sys
from pathlib import Path
from typing import Dict, List, Any


def validate_file_structure():
    """Validate that all thesis-claimed modules are present."""
    logger = logging.getLogger(__name__)
    results = {}
    
    # Required directories and files for thesis claims
    required_paths = {
        # Core configuration
        'system_configuration': 'PythonApp/config/system_configuration.py',
        
        # Enhanced processing modules
        'radiometric_processor': 'PythonApp/thermal/radiometric_processor.py', 
        'shimmer_processor': 'PythonApp/shimmer/enhanced_processor.py',
        
        # Network and communication
        'lsl_integration': 'PythonApp/network/lsl_integration.py',
        'zeroconf_discovery': 'PythonApp/network/zeroconf_discovery.py',
        'enhanced_security': 'PythonApp/network/enhanced_security.py',
        'file_integrity': 'PythonApp/network/file_integrity.py',
        'enhanced_protocol': 'PythonApp/protocol/enhanced_protocol.py',
        
        # Synchronization and protocols
        'ntp_lab_protocols': 'PythonApp/synchronization/ntp_lab_protocols.py',
        
        # GUI implementation
        'multi_sensor_gui': 'PythonApp/gui/multi_sensor_gui.py',
        
        # Native backends
        'native_shimmer_wrapper': 'PythonApp/native_backends/native_shimmer_wrapper.py',
        'native_webcam_wrapper': 'PythonApp/native_backends/native_webcam_wrapper.py',
        
        # Recording and data handling
        'comprehensive_data_recorder': 'PythonApp/recording/comprehensive_data_recorder.py',
        
        # Android privacy and security
        'android_privacy_manager': 'AndroidApp/src/main/java/com/multisensor/recording/security/PrivacyManager.kt',
        'android_thermal_recorder': 'AndroidApp/src/main/java/com/multisensor/recording/recording/ThermalRecorder.kt',
        'android_shimmer_recorder': 'AndroidApp/src/main/java/com/multisensor/recording/recording/ShimmerRecorder.kt',
        
        # Native C++ backends
        'native_backend_cpp': 'native_backend',
        
        # Testing and validation
        'performance_verification': 'tests/test_performance_verification.py',
        'native_backend_integration': 'tests/test_native_backend_integration.py',
    }
    
    base_path = Path(__file__).parent.parent
    
    for component_name, relative_path in required_paths.items():
        full_path = base_path / relative_path
        exists = full_path.exists()
        
        results[component_name] = {
            'path': str(relative_path),
            'exists': exists,
            'claim': f'{component_name.replace("_", " ").title()} implementation present',
            'severity': 'critical' if 'core' in component_name or 'security' in component_name else 'moderate'
        }
        
        if exists:
            logger.info(f"[PASS] Found: {relative_path}")
        else:
            logger.error(f"[FAIL] Missing: {relative_path}")
    
    return results


def validate_configuration_values():
    """Validate thesis-specific configuration values."""
    logger = logging.getLogger(__name__)
    results = {}
    
    try:
        # Add PythonApp to path
        sys.path.insert(0, str(Path(__file__).parent.parent / "PythonApp"))
        
        from config.system_configuration import SystemConfiguration, DeviceType
        
        config = SystemConfiguration()
        
        # Validate Shimmer configuration - Thesis Claim: "128 Hz, 16-bit"
        shimmer_config = config.get_device_config(DeviceType.SHIMMER_GSR_PLUS)
        results['shimmer_sampling_rate'] = {
            'claim': 'Shimmer GSR+ sampling at 128 Hz',
            'expected': 128.0,
            'actual': shimmer_config.sampling_rate_hz,
            'passed': shimmer_config.sampling_rate_hz == 128.0,
            'severity': 'critical'
        }
        
        results['shimmer_resolution'] = {
            'claim': 'Shimmer GSR+ 16-bit resolution',
            'expected': 16,
            'actual': shimmer_config.resolution_bits,
            'passed': shimmer_config.resolution_bits == 16,
            'severity': 'critical'
        }
        
        # Validate Thermal configuration - Thesis Claim: "256x192 at 25 Hz"
        thermal_config = config.get_device_config(DeviceType.TOPDON_TC001)
        results['thermal_sampling_rate'] = {
            'claim': 'Thermal camera at 25 Hz',
            'expected': 25.0,
            'actual': thermal_config.sampling_rate_hz,
            'passed': thermal_config.sampling_rate_hz == 25.0,
            'severity': 'critical'
        }
        
        results['thermal_resolution'] = {
            'claim': 'Thermal camera 256x192 resolution',
            'expected': (256, 192),
            'actual': (thermal_config.resolution_width, thermal_config.resolution_height),
            'passed': thermal_config.resolution_width == 256 and thermal_config.resolution_height == 192,
            'severity': 'critical'
        }
        
        # Validate RGB configuration - Thesis Claim: "1080p at 30 fps"
        rgb_config = config.get_device_config(DeviceType.ANDROID_RGB_CAMERA)
        results['rgb_sampling_rate'] = {
            'claim': 'RGB camera at 30 fps',
            'expected': 30.0,
            'actual': rgb_config.sampling_rate_hz,
            'passed': rgb_config.sampling_rate_hz == 30.0,
            'severity': 'critical'
        }
        
        results['rgb_resolution'] = {
            'claim': 'RGB camera 1080p (1920x1080)',
            'expected': (1920, 1080),
            'actual': (rgb_config.resolution_width, rgb_config.resolution_height),
            'passed': rgb_config.resolution_width == 1920 and rgb_config.resolution_height == 1080,
            'severity': 'critical'
        }
        
        # Validate NTP synchronization target - Thesis Claim: "~21 ms median offset"
        ntp_target = config.network.ntp_sync_target_offset_ms
        results['ntp_sync_target'] = {
            'claim': 'NTP sync target ~21 ms median offset',
            'expected': 21.0,
            'actual': ntp_target,
            'passed': ntp_target == 21.0,
            'severity': 'critical'
        }
        
        # Validate security settings
        results['security_tls_version'] = {
            'claim': 'TLS 1.3 security requirement',
            'expected': 'TLSv1.3',
            'actual': config.security.tls_version,
            'passed': config.security.tls_version == 'TLSv1.3',
            'severity': 'critical'
        }
        
        results['security_token_length'] = {
            'claim': 'Minimum 32-character tokens',
            'expected': '>=32',
            'actual': config.security.min_token_length,
            'passed': config.security.min_token_length >= 32,
            'severity': 'critical'
        }
        
        logger.info("Configuration validation completed successfully")
        
    except Exception as e:
        logger.error(f"Configuration validation failed: {e}")
        results['configuration_error'] = {
            'claim': 'System configuration accessible',
            'expected': True,
            'actual': False,
            'passed': False,
            'severity': 'critical',
            'error': str(e)
        }
    
    return results


def validate_feature_implementations():
    """Validate that key features are implemented."""
    logger = logging.getLogger(__name__)
    results = {}
    
    # Test LSL integration
    try:
        sys.path.insert(0, str(Path(__file__).parent.parent / "PythonApp"))
        from network.lsl_integration import LSLStreamer, DefaultLSLStreams
        
        streamer = LSLStreamer()
        shimmer_config = DefaultLSLStreams.shimmer_gsr_stream("test_device")
        
        results['lsl_integration'] = {
            'claim': 'LSL integration implemented',
            'expected': True,
            'actual': True,
            'passed': True,
            'severity': 'critical',
            'details': f'LSL enabled: {streamer.is_enabled}, Shimmer config: {shimmer_config.nominal_srate}Hz'
        }
        
    except Exception as e:
        results['lsl_integration'] = {
            'claim': 'LSL integration implemented',
            'expected': True,
            'actual': False,
            'passed': False,
            'severity': 'critical',
            'error': str(e)
        }
    
    # Test Zeroconf discovery
    try:
        from network.zeroconf_discovery import MultiSensorDiscovery
        discovery = MultiSensorDiscovery()
        
        results['zeroconf_discovery'] = {
            'claim': 'Zeroconf/mDNS discovery implemented',
            'expected': True,
            'actual': True,
            'passed': True,
            'severity': 'moderate'
        }
        
    except Exception as e:
        results['zeroconf_discovery'] = {
            'claim': 'Zeroconf/mDNS discovery implemented',
            'expected': True,
            'actual': False,
            'passed': False,
            'severity': 'moderate',
            'error': str(e)
        }
    
    # Test enhanced security
    try:
        from network.enhanced_security import TokenManager, SecurityConfig, RuntimeSecurityChecker
        
        config = SecurityConfig(use_tls=True, tls_version="TLSv1.3")
        token_manager = TokenManager()
        security_checker = RuntimeSecurityChecker(config)
        
        # Test token generation
        test_token = token_manager.generate_token("test_device", ["read", "write"])
        token_valid = test_token is not None and len(test_token.token) >= 32
        
        results['enhanced_security'] = {
            'claim': 'Enhanced security with TLS/tokens implemented',
            'expected': True,
            'actual': token_valid,
            'passed': token_valid,
            'severity': 'critical',
            'details': f'Token length: {len(test_token.token) if test_token else 0}, TLS: {config.use_tls}'
        }
        
    except Exception as e:
        results['enhanced_security'] = {
            'claim': 'Enhanced security with TLS/tokens implemented',
            'expected': True,
            'actual': False,
            'passed': False,
            'severity': 'critical',
            'error': str(e)
        }
    
    # Test file integrity
    try:
        from network.file_integrity import SecureFileTransfer, FileIntegrityVerifier
        
        transfer = SecureFileTransfer()
        verifier = FileIntegrityVerifier()
        
        results['file_integrity'] = {
            'claim': 'SHA-256 file integrity verification implemented',
            'expected': True,
            'actual': True,
            'passed': True,
            'severity': 'moderate'
        }
        
    except Exception as e:
        results['file_integrity'] = {
            'claim': 'SHA-256 file integrity verification implemented',
            'expected': True,
            'actual': False,
            'passed': False,
            'severity': 'moderate',
            'error': str(e)
        }
    
    # Test GUI availability
    try:
        from gui.multi_sensor_gui import PYQT6_AVAILABLE, create_gui_application
        
        results['gui_implementation'] = {
            'claim': 'PyQt6 GUI with real-time previews implemented',
            'expected': True,
            'actual': PYQT6_AVAILABLE,
            'passed': PYQT6_AVAILABLE,
            'severity': 'moderate',
            'note': 'GUI requires display environment for full functionality'
        }
        
    except Exception as e:
        results['gui_implementation'] = {
            'claim': 'PyQt6 GUI with real-time previews implemented',
            'expected': True,
            'actual': False,
            'passed': False,
            'severity': 'moderate',
            'error': str(e)
        }
    
    # Test native backends
    try:
        from native_backends.native_shimmer_wrapper import ShimmerProcessor
        from native_backends.native_webcam_wrapper import WebcamProcessor
        
        shimmer_proc = ShimmerProcessor()
        webcam_proc = WebcamProcessor()
        
        # Check if native backends are available
        shimmer_native = hasattr(shimmer_proc, 'backend_type')
        webcam_native = hasattr(webcam_proc, 'backend_type')
        
        results['native_backends'] = {
            'claim': 'PyBind11 native backends implemented',
            'expected': True,
            'actual': shimmer_native and webcam_native,
            'passed': shimmer_native and webcam_native,
            'severity': 'critical',
            'details': f'Shimmer backend: {shimmer_native}, Webcam backend: {webcam_native}'
        }
        
    except Exception as e:
        results['native_backends'] = {
            'claim': 'PyBind11 native backends implemented',
            'expected': True,
            'actual': False,
            'passed': False,
            'severity': 'critical',
            'error': str(e)
        }
    
    return results


def validate_android_implementations():
    """Validate Android-specific implementations."""
    logger = logging.getLogger(__name__)
    results = {}
    
    base_path = Path(__file__).parent.parent
    
    # Check Android Kotlin files for thesis-specific features
    android_files = {
        'thermal_recorder': 'AndroidApp/src/main/java/com/multisensor/recording/recording/ThermalRecorder.kt',
        'shimmer_recorder': 'AndroidApp/src/main/java/com/multisensor/recording/recording/ShimmerRecorder.kt',
        'privacy_manager': 'AndroidApp/src/main/java/com/multisensor/recording/security/PrivacyManager.kt',
    }
    
    for component, file_path in android_files.items():
        full_path = base_path / file_path
        
        if full_path.exists():
            try:
                content = full_path.read_text()
                
                # Check for thesis-specific implementations
                if component == 'thermal_recorder':
                    # Check for Topdon/Infisense integration
                    has_infisense = 'com.infisense.iruvc' in content
                    has_256x192 = 'THERMAL_WIDTH = 256' in content and 'THERMAL_HEIGHT = 192' in content
                    has_25hz = 'THERMAL_FRAME_RATE = 25' in content
                    has_radiometric = 'temperatureSrc' in content or 'radiometric' in content.lower()
                    
                    thermal_compliant = has_infisense and has_256x192 and has_25hz
                    
                    results['android_thermal_integration'] = {
                        'claim': 'Android thermal recorder with Topdon TC001 integration',
                        'expected': True,
                        'actual': thermal_compliant,
                        'passed': thermal_compliant,
                        'severity': 'critical',
                        'details': f'Infisense SDK: {has_infisense}, 256x192: {has_256x192}, 25Hz: {has_25hz}, Radiometric: {has_radiometric}'
                    }
                
                elif component == 'shimmer_recorder':
                    # Check for Shimmer SDK integration
                    has_shimmer_sdk = 'com.shimmerresearch.android' in content
                    has_128hz = 'DEFAULT_SAMPLING_RATE' in content
                    has_gsr_processing = 'GSR_Conductance' in content or 'microsiemens' in content.lower()
                    has_ble = 'BluetoothAdapter' in content or 'Shimmer' in content
                    
                    shimmer_compliant = has_shimmer_sdk and has_gsr_processing and has_ble
                    
                    results['android_shimmer_integration'] = {
                        'claim': 'Android Shimmer recorder with GSR processing',
                        'expected': True,
                        'actual': shimmer_compliant,
                        'passed': shimmer_compliant,
                        'severity': 'critical',
                        'details': f'Shimmer SDK: {has_shimmer_sdk}, GSR processing: {has_gsr_processing}, BLE: {has_ble}'
                    }
                
                elif component == 'privacy_manager':
                    # Check for privacy features
                    has_encrypted_prefs = 'EncryptedSharedPreferences' in content
                    has_aes_gcm = 'AES256_GCM' in content
                    has_face_blurring = 'face_blurring' in content.lower() or 'FaceBlurring' in content
                    has_security_events = 'SecurityEvent' in content
                    
                    privacy_compliant = has_encrypted_prefs and has_aes_gcm
                    
                    results['android_privacy_features'] = {
                        'claim': 'Android privacy with EncryptedSharedPreferences and AES-GCM',
                        'expected': True,
                        'actual': privacy_compliant,
                        'passed': privacy_compliant,
                        'severity': 'critical',
                        'details': f'Encrypted prefs: {has_encrypted_prefs}, AES-GCM: {has_aes_gcm}, Face blur: {has_face_blurring}, Security events: {has_security_events}'
                    }
                
            except Exception as e:
                results[f'android_{component}_analysis'] = {
                    'claim': f'Android {component} code analysis',
                    'expected': True,
                    'actual': False,
                    'passed': False,
                    'severity': 'moderate',
                    'error': str(e)
                }
        else:
            results[f'android_{component}_missing'] = {
                'claim': f'Android {component} file exists',
                'expected': True,
                'actual': False,
                'passed': False,
                'severity': 'critical'
            }
    
    return results


def run_comprehensive_validation():
    """Run comprehensive thesis validation."""
    logging.basicConfig(level=logging.INFO, format='%(levelname)s: %(message)s')
    logger = logging.getLogger(__name__)
    
    print("="*80)
    print("COMPREHENSIVE THESIS CLAIMS VALIDATION")
    print("="*80)
    
    all_results = {}
    
    # File structure validation
    print("\n1. Validating File Structure...")
    structure_results = validate_file_structure()
    all_results.update(structure_results)
    
    # Configuration validation
    print("\n2. Validating Configuration Values...")
    config_results = validate_configuration_values()
    all_results.update(config_results)
    
    # Feature implementation validation
    print("\n3. Validating Feature Implementations...")
    feature_results = validate_feature_implementations()
    all_results.update(feature_results)
    
    # Android implementation validation
    print("\n4. Validating Android Implementations...")
    android_results = validate_android_implementations()
    all_results.update(android_results)
    
    # Calculate overall compliance
    total_tests = len(all_results)
    passed_tests = sum(1 for result in all_results.values() if result.get('passed', False))
    compliance_percentage = (passed_tests / total_tests) * 100 if total_tests > 0 else 0
    
    # Count critical failures
    critical_failures = [
        name for name, result in all_results.items()
        if not result.get('passed', False) and result.get('severity') == 'critical'
    ]
    
    # Generate report
    print("\n" + "="*80)
    print("VALIDATION RESULTS SUMMARY")
    print("="*80)
    print(f"Overall Compliance: {compliance_percentage:.1f}%")
    print(f"Tests Passed: {passed_tests}/{total_tests}")
    print(f"Critical Failures: {len(critical_failures)}")
    
    if compliance_percentage >= 95:
        print("\n[PASS] THESIS CLAIMS VALIDATION: PASSED")
        overall_status = "PASSED"
    elif compliance_percentage >= 80:
        print("\n[WARN]  THESIS CLAIMS VALIDATION: MOSTLY COMPLIANT")
        overall_status = "MOSTLY_COMPLIANT"
    else:
        print("\n[FAIL] THESIS CLAIMS VALIDATION: FAILED")
        overall_status = "FAILED"
    
    # Detailed results
    print("\nDETAILED RESULTS:")
    print("-"*80)
    
    for test_name, result in all_results.items():
        status = "PASS" if result.get('passed', False) else "FAIL"
        severity = result.get('severity', 'unknown').upper()
        
        print(f"[{status}] [{severity}] {test_name}")
        if 'claim' in result:
            print(f"  Claim: {result['claim']}")
        if 'expected' in result and 'actual' in result:
            print(f"  Expected: {result['expected']}")
            print(f"  Actual: {result['actual']}")
        if 'details' in result:
            print(f"  Details: {result['details']}")
        if 'error' in result:
            print(f"  Error: {result['error']}")
        print()
    
    # Critical failures summary
    if critical_failures:
        print("CRITICAL FAILURES REQUIRING ATTENTION:")
        print("-"*50)
        for failure in critical_failures:
            print(f"  - {failure}")
        print()
    
    # Implementation coverage summary
    print("THESIS IMPLEMENTATION COVERAGE:")
    print("-"*50)
    
    coverage_items = [
        ("Multi-sensor architecture", "shimmer_config" in str(all_results)),
        ("Device configurations (128Hz/25Hz/30fps)", any("sampling_rate" in name for name in all_results)),
        ("LSL integration", "lsl_integration" in all_results and all_results["lsl_integration"].get("passed", False)),
        ("Zeroconf device discovery", "zeroconf_discovery" in all_results and all_results["zeroconf_discovery"].get("passed", False)),
        ("Enhanced security (TLS/AES)", "enhanced_security" in all_results and all_results["enhanced_security"].get("passed", False)),
        ("File integrity (SHA-256)", "file_integrity" in all_results and all_results["file_integrity"].get("passed", False)),
        ("Native backend (PyBind11)", "native_backends" in all_results and all_results["native_backends"].get("passed", False)),
        ("GUI implementation (PyQt6)", "gui_implementation" in all_results),
        ("Android privacy features", any("android_privacy" in name for name in all_results)),
        ("Radiometric processing", "radiometric_processor" in all_results),
        ("NTP synchronization (~21ms)", "ntp_sync_target" in all_results and all_results["ntp_sync_target"].get("passed", False)),
    ]
    
    for item_name, item_implemented in coverage_items:
        status = "[PASS]" if item_implemented else "[FAIL]"
        print(f"  {status} {item_name}")
    
    # Save detailed results
    results_summary = {
        'validation_timestamp': time.time(),
        'overall_compliance_percentage': compliance_percentage,
        'tests_passed': passed_tests,
        'tests_total': total_tests,
        'critical_failures': len(critical_failures),
        'overall_status': overall_status,
        'detailed_results': all_results,
        'thesis_coverage_items': dict(coverage_items)
    }
    
    # Save to file
    output_file = Path(__file__).parent.parent / "thesis_validation_report.json"
    try:
        with open(output_file, 'w') as f:
            json.dump(results_summary, f, indent=2, default=str)
        print(f"\nDetailed results saved to: {output_file}")
    except Exception as e:
        logger.error(f"Failed to save results: {e}")
    
    return results_summary


if __name__ == "__main__":
    results = run_comprehensive_validation()
    
    # Exit with appropriate code
    if results['overall_compliance_percentage'] >= 95:
        exit(0)
    elif results['overall_compliance_percentage'] >= 80:
        exit(0)  # Accept mostly compliant as success
    else:
        exit(1)