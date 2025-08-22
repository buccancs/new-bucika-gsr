"""
Enhanced security layer with TLS/SSL support and authentication.

This module provides comprehensive security features including TLS encryption,
token-based authentication, and secure communication protocols for the
multi-sensor recording system.
"""

import ssl
import socket
import logging
import secrets
import hashlib
import hmac
import json
import time
import threading
from typing import Optional, Dict, Tuple, List, Any, Callable
from dataclasses import dataclass
from pathlib import Path
import base64
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography import x509
from cryptography.x509.oid import NameOID, ExtensionOID
import datetime
import ipaddress


@dataclass
class SecurityConfig:
    """Security configuration parameters."""
    use_tls: bool = True
    tls_version: str = "TLSv1.3"
    cert_file: Optional[str] = None
    key_file: Optional[str] = None
    ca_file: Optional[str] = None
    require_client_cert: bool = False
    token_length: int = 32
    token_expiry_hours: int = 24
    max_token_age_seconds: int = 86400  # 24 hours
    min_token_length: int = 16
    allowed_ciphers: List[str] = None
    verify_mode: str = "CERT_REQUIRED"
    
    def __post_init__(self):
        if self.allowed_ciphers is None:
            self.allowed_ciphers = [
                "ECDHE+AESGCM",
                "ECDHE+CHACHA20",
                "DHE+AESGCM",
                "DHE+CHACHA20",
                "!aNULL",
                "!MD5",
                "!DSS"
            ]


@dataclass
class AuthToken:
    """Authentication token with metadata."""
    token: str
    device_id: str
    issued_at: float
    expires_at: float
    permissions: List[str]
    
    def is_valid(self) -> bool:
        """Check if token is still valid."""
        return time.time() < self.expires_at
    
    def has_permission(self, permission: str) -> bool:
        """Check if token has specific permission."""
        return permission in self.permissions or "admin" in self.permissions


class CertificateManager:
    """Manages SSL/TLS certificates for secure communication."""
    
    def __init__(self, logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
    
    def generate_self_signed_cert(self, hostname: str, cert_file: str, key_file: str, 
                                 days_valid: int = 365) -> bool:
        """Generate a self-signed certificate for development/testing."""
        try:
            # Generate private key
            private_key = rsa.generate_private_key(
                public_exponent=65537,
                key_size=2048,
            )
            
            # Create certificate
            subject = issuer = x509.Name([
                x509.NameAttribute(NameOID.COUNTRY_NAME, "UK"),
                x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "London"),
                x509.NameAttribute(NameOID.LOCALITY_NAME, "London"),
                x509.NameAttribute(NameOID.ORGANIZATION_NAME, "UCL Multi-Sensor System"),
                x509.NameAttribute(NameOID.COMMON_NAME, hostname),
            ])
            
            # Add subject alternative names
            san_list = [x509.DNSName(hostname)]
            try:
                # Try to parse as IP address
                ip = ipaddress.ip_address(hostname)
                san_list.append(x509.IPAddress(ip))
            except ValueError:
                # Not an IP address, just use DNS name
                pass
            
            cert = x509.CertificateBuilder().subject_name(
                subject
            ).issuer_name(
                issuer
            ).public_key(
                private_key.public_key()
            ).serial_number(
                x509.random_serial_number()
            ).not_valid_before(
                datetime.datetime.utcnow()
            ).not_valid_after(
                datetime.datetime.utcnow() + datetime.timedelta(days=days_valid)
            ).add_extension(
                x509.SubjectAlternativeName(san_list),
                critical=False,
            ).add_extension(
                x509.KeyUsage(
                    digital_signature=True,
                    key_encipherment=True,
                    key_agreement=False,
                    key_cert_sign=False,
                    crl_sign=False,
                    content_commitment=False,
                    data_encipherment=False,
                    encipher_only=False,
                    decipher_only=False
                ),
                critical=True,
            ).add_extension(
                x509.ExtendedKeyUsage([
                    x509.oid.ExtendedKeyUsageOID.SERVER_AUTH,
                    x509.oid.ExtendedKeyUsageOID.CLIENT_AUTH,
                ]),
                critical=True,
            ).sign(private_key, hashes.SHA256())
            
            # Write private key
            with open(key_file, "wb") as f:
                f.write(private_key.private_bytes(
                    encoding=serialization.Encoding.PEM,
                    format=serialization.PrivateFormat.PKCS8,
                    encryption_algorithm=serialization.NoEncryption()
                ))
            
            # Write certificate
            with open(cert_file, "wb") as f:
                f.write(cert.public_bytes(serialization.Encoding.PEM))
            
            self.logger.info(f"Generated self-signed certificate for {hostname}")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to generate self-signed certificate: {e}")
            return False
    
    def verify_certificate(self, cert_file: str) -> bool:
        """Verify certificate validity."""
        try:
            with open(cert_file, "rb") as f:
                cert_data = f.read()
            
            cert = x509.load_pem_x509_certificate(cert_data)
            
            # Check if certificate is still valid
            now = datetime.datetime.utcnow()
            if now < cert.not_valid_before or now > cert.not_valid_after:
                self.logger.warning(f"Certificate {cert_file} is expired or not yet valid")
                return False
            
            self.logger.info(f"Certificate {cert_file} is valid")
            return True
            
        except Exception as e:
            self.logger.error(f"Certificate verification failed: {e}")
            return False


class TokenManager:
    """Manages authentication tokens with secure generation and validation."""
    
    def __init__(self, secret_key: Optional[str] = None, logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
        self.secret_key = secret_key or secrets.token_hex(32)
        self.active_tokens: Dict[str, AuthToken] = {}
        self.device_tokens: Dict[str, str] = {}  # device_id -> token
        self.lock = threading.RLock()
    
    def generate_token(self, device_id: str, permissions: List[str], 
                      expiry_hours: int = 24, min_length: int = 16) -> Optional[AuthToken]:
        """Generate a new authentication token."""
        try:
            # Validate minimum token length
            token_length = max(min_length, 32)
            
            # Generate secure random token
            token = secrets.token_urlsafe(token_length)
            
            # Create token object
            now = time.time()
            auth_token = AuthToken(
                token=token,
                device_id=device_id,
                issued_at=now,
                expires_at=now + (expiry_hours * 3600),
                permissions=permissions
            )
            
            # Store token
            with self.lock:
                self.active_tokens[token] = auth_token
                self.device_tokens[device_id] = token
            
            self.logger.info(f"Generated token for device {device_id} (expires in {expiry_hours}h)")
            return auth_token
            
        except Exception as e:
            self.logger.error(f"Failed to generate token for {device_id}: {e}")
            return None
    
    def validate_token(self, token: str) -> Optional[AuthToken]:
        """Validate an authentication token."""
        with self.lock:
            auth_token = self.active_tokens.get(token)
            
            if not auth_token:
                self.logger.warning(f"Invalid token: {token[:8]}...")
                return None
            
            if not auth_token.is_valid():
                self.logger.warning(f"Expired token for device {auth_token.device_id}")
                self.revoke_token(token)
                return None
            
            return auth_token
    
    def revoke_token(self, token: str) -> bool:
        """Revoke an authentication token."""
        with self.lock:
            auth_token = self.active_tokens.get(token)
            if auth_token:
                del self.active_tokens[token]
                if auth_token.device_id in self.device_tokens:
                    del self.device_tokens[auth_token.device_id]
                self.logger.info(f"Revoked token for device {auth_token.device_id}")
                return True
        return False
    
    def revoke_device_tokens(self, device_id: str) -> bool:
        """Revoke all tokens for a specific device."""
        with self.lock:
            if device_id in self.device_tokens:
                token = self.device_tokens[device_id]
                return self.revoke_token(token)
        return False
    
    def cleanup_expired_tokens(self):
        """Remove expired tokens from memory."""
        with self.lock:
            current_time = time.time()
            expired_tokens = [
                token for token, auth_token in self.active_tokens.items()
                if current_time >= auth_token.expires_at
            ]
            
            for token in expired_tokens:
                auth_token = self.active_tokens[token]
                del self.active_tokens[token]
                if auth_token.device_id in self.device_tokens:
                    del self.device_tokens[auth_token.device_id]
            
            if expired_tokens:
                self.logger.info(f"Cleaned up {len(expired_tokens)} expired tokens")
    
    def get_active_devices(self) -> List[str]:
        """Get list of devices with active tokens."""
        with self.lock:
            return list(self.device_tokens.keys())


class SecureSocketWrapper:
    """Wraps sockets with TLS encryption and authentication."""
    
    def __init__(self, config: SecurityConfig, token_manager: TokenManager, 
                 logger: Optional[logging.Logger] = None):
        self.config = config
        self.token_manager = token_manager
        self.logger = logger or logging.getLogger(__name__)
        self.cert_manager = CertificateManager(logger)
    
    def create_server_context(self) -> Optional[ssl.SSLContext]:
        """Create SSL context for server-side connections."""
        if not self.config.use_tls:
            return None
        
        try:
            # Create SSL context
            if self.config.tls_version == "TLSv1.3":
                context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
                context.minimum_version = ssl.TLSVersion.TLSv1_3
            else:
                context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
                context.minimum_version = ssl.TLSVersion.TLSv1_2
            
            # Set cipher suites
            if self.config.allowed_ciphers:
                context.set_ciphers(":".join(self.config.allowed_ciphers))
            
            # Configure certificate verification
            if self.config.verify_mode == "CERT_REQUIRED":
                context.verify_mode = ssl.CERT_REQUIRED
            elif self.config.verify_mode == "CERT_OPTIONAL":
                context.verify_mode = ssl.CERT_OPTIONAL
            else:
                context.verify_mode = ssl.CERT_NONE
            
            # Load certificates
            if self.config.cert_file and self.config.key_file:
                if not Path(self.config.cert_file).exists():
                    self.logger.warning("Certificate file not found, generating self-signed certificate")
                    hostname = socket.gethostname()
                    self.cert_manager.generate_self_signed_cert(
                        hostname, self.config.cert_file, self.config.key_file
                    )
                
                context.load_cert_chain(self.config.cert_file, self.config.key_file)
                self.logger.info("Loaded server certificate and key")
            
            # Load CA certificates
            if self.config.ca_file and Path(self.config.ca_file).exists():
                context.load_verify_locations(self.config.ca_file)
                self.logger.info("Loaded CA certificates")
            
            return context
            
        except Exception as e:
            self.logger.error(f"Failed to create server SSL context: {e}")
            return None
    
    def create_client_context(self) -> Optional[ssl.SSLContext]:
        """Create SSL context for client-side connections."""
        if not self.config.use_tls:
            return None
        
        try:
            # Create SSL context
            if self.config.tls_version == "TLSv1.3":
                context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
                context.minimum_version = ssl.TLSVersion.TLSv1_3
            else:
                context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
                context.minimum_version = ssl.TLSVersion.TLSv1_2
            
            # Set cipher suites
            if self.config.allowed_ciphers:
                context.set_ciphers(":".join(self.config.allowed_ciphers))
            
            # Configure certificate verification
            if self.config.verify_mode == "CERT_REQUIRED":
                context.check_hostname = True
                context.verify_mode = ssl.CERT_REQUIRED
            else:
                context.check_hostname = False
                context.verify_mode = ssl.CERT_NONE
            
            # Load CA certificates
            if self.config.ca_file and Path(self.config.ca_file).exists():
                context.load_verify_locations(self.config.ca_file)
            else:
                context.load_default_certs()
            
            return context
            
        except Exception as e:
            self.logger.error(f"Failed to create client SSL context: {e}")
            return None
    
    def wrap_server_socket(self, sock: socket.socket) -> Optional[ssl.SSLSocket]:
        """Wrap a server socket with TLS."""
        if not self.config.use_tls:
            return sock
        
        context = self.create_server_context()
        if not context:
            return None
        
        try:
            return context.wrap_socket(sock, server_side=True)
        except Exception as e:
            self.logger.error(f"Failed to wrap server socket with TLS: {e}")
            return None
    
    def wrap_client_socket(self, sock: socket.socket, hostname: str) -> Optional[ssl.SSLSocket]:
        """Wrap a client socket with TLS."""
        if not self.config.use_tls:
            return sock
        
        context = self.create_client_context()
        if not context:
            return None
        
        try:
            return context.wrap_socket(sock, server_hostname=hostname)
        except Exception as e:
            self.logger.error(f"Failed to wrap client socket with TLS: {e}")
            return None


class RuntimeSecurityChecker:
    """Performs runtime security checks and monitoring."""
    
    def __init__(self, config: SecurityConfig, logger: Optional[logging.Logger] = None):
        self.config = config
        self.logger = logger or logging.getLogger(__name__)
        self.security_events: List[Dict[str, Any]] = []
        self.lock = threading.Lock()
    
    def check_configuration(self) -> List[str]:
        """Check security configuration for issues."""
        issues = []
        
        # Check TLS configuration
        if not self.config.use_tls:
            issues.append("TLS encryption is disabled")
        
        # Check TLS version
        if self.config.tls_version not in ["TLSv1.2", "TLSv1.3"]:
            issues.append(f"Insecure TLS version: {self.config.tls_version}")
        
        # Check token configuration
        if self.config.min_token_length < 16:
            issues.append(f"Minimum token length too short: {self.config.min_token_length}")
        
        if self.config.max_token_age_seconds > 7 * 24 * 3600:  # 7 days
            issues.append("Token expiry time too long (>7 days)")
        
        # Check certificate files
        if self.config.use_tls:
            if self.config.cert_file and not Path(self.config.cert_file).exists():
                issues.append(f"Certificate file not found: {self.config.cert_file}")
            
            if self.config.key_file and not Path(self.config.key_file).exists():
                issues.append(f"Private key file not found: {self.config.key_file}")
        
        return issues
    
    def log_security_event(self, event_type: str, description: str, severity: str = "info"):
        """Log a security event."""
        event = {
            "timestamp": time.time(),
            "type": event_type,
            "description": description,
            "severity": severity
        }
        
        with self.lock:
            self.security_events.append(event)
            
            # Keep only last 1000 events
            if len(self.security_events) > 1000:
                self.security_events = self.security_events[-1000:]
        
        # Log to standard logger
        log_func = getattr(self.logger, severity, self.logger.info)
        log_func(f"Security event [{event_type}]: {description}")
    
    def check_token_security(self, token: str) -> bool:
        """Check if a token meets security requirements."""
        if len(token) < self.config.min_token_length:
            self.log_security_event("token_check", f"Token too short: {len(token)}", "warning")
            return False
        
        # Check for common weak patterns
        if token.lower() in ["admin", "password", "123456", "test"]:
            self.log_security_event("token_check", "Weak token detected", "critical")
            return False
        
        return True
    
    def get_security_events(self, since: Optional[float] = None) -> List[Dict[str, Any]]:
        """Get security events since a specific time."""
        with self.lock:
            if since is None:
                return self.security_events.copy()
            
            return [event for event in self.security_events if event["timestamp"] >= since]


# Convenience function for easy integration
def create_secure_server(host: str, port: int, config: SecurityConfig, 
                        token_manager: TokenManager) -> Tuple[socket.socket, Optional[ssl.SSLContext]]:
    """Create a secure server socket with TLS and authentication."""
    logger = logging.getLogger(__name__)
    
    try:
        # Create socket
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        
        # Wrap with TLS if enabled
        security_wrapper = SecureSocketWrapper(config, token_manager, logger)
        
        if config.use_tls:
            ssl_context = security_wrapper.create_server_context()
            if ssl_context:
                server_socket = ssl_context.wrap_socket(server_socket, server_side=True)
                logger.info(f"Created secure server on {host}:{port} with TLS {config.tls_version}")
            else:
                logger.error("Failed to create TLS context")
                return None, None
        else:
            logger.warning(f"Created INSECURE server on {host}:{port} (TLS disabled)")
        
        server_socket.bind((host, port))
        return server_socket, ssl_context if config.use_tls else None
        
    except Exception as e:
        logger.error(f"Failed to create secure server: {e}")
        return None, None


if __name__ == "__main__":
    # Example usage
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    
    # Create security configuration
    config = SecurityConfig(
        use_tls=True,
        tls_version="TLSv1.3",
        cert_file="server.crt",
        key_file="server.key",
        min_token_length=32
    )
    
    # Create token manager
    token_manager = TokenManager(logger=logger)
    
    # Create security checker
    security_checker = RuntimeSecurityChecker(config, logger)
    
    # Check configuration
    issues = security_checker.check_configuration()
    if issues:
        logger.warning("Security configuration issues:")
        for issue in issues:
            logger.warning(f"  - {issue}")
    
    # Generate test token
    token = token_manager.generate_token("test_device", ["read", "write"], expiry_hours=1)
    if token:
        logger.info(f"Generated token: {token.token[:16]}...")
        
        # Validate token
        validated = token_manager.validate_token(token.token)
        if validated:
            logger.info("Token validation successful")
        
        # Check token security
        if security_checker.check_token_security(token.token):
            logger.info("Token meets security requirements")
    
    logger.info("Security test completed")