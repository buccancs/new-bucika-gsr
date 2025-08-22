"""
File integrity and secure transfer utilities with SHA-256 checksums.

This module provides secure file transfer capabilities with integrity checking,
chunked transfers, error recovery, and progress monitoring for the multi-sensor
recording system.
"""

import hashlib
import os
import json
import time
import socket
import struct
import logging
import threading
from typing import Optional, Dict, List, Callable, BinaryIO, Any, Tuple
from dataclasses import dataclass, asdict
from pathlib import Path
import base64
import zlib


@dataclass
class FileMetadata:
    """Metadata for file transfers."""
    filename: str
    file_size: int
    sha256_hash: str
    chunk_size: int
    total_chunks: int
    compression: str = "none"
    timestamp: float = 0.0
    
    def __post_init__(self):
        if self.timestamp == 0.0:
            self.timestamp = time.time()
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)
    
    def to_json(self) -> str:
        return json.dumps(self.to_dict())
    
    @classmethod
    def from_json(cls, json_str: str) -> 'FileMetadata':
        data = json.loads(json_str)
        return cls(**data)


@dataclass
class TransferProgress:
    """Progress tracking for file transfers."""
    filename: str
    total_size: int
    transferred_size: int
    chunks_completed: int
    total_chunks: int
    start_time: float
    last_update: float
    speed_bps: float = 0.0
    
    @property
    def progress_percent(self) -> float:
        if self.total_size == 0:
            return 0.0
        return (self.transferred_size / self.total_size) * 100.0
    
    @property
    def estimated_time_remaining(self) -> float:
        if self.speed_bps <= 0:
            return float('inf')
        remaining_bytes = self.total_size - self.transferred_size
        return remaining_bytes / self.speed_bps
    
    def update(self, bytes_transferred: int):
        """Update transfer progress."""
        self.transferred_size += bytes_transferred
        self.chunks_completed += 1
        current_time = time.time()
        
        # Calculate transfer speed
        if current_time > self.start_time:
            self.speed_bps = self.transferred_size / (current_time - self.start_time)
        
        self.last_update = current_time


class FileHasher:
    """Utility for computing file hashes."""
    
    @staticmethod
    def compute_sha256(file_path: str, chunk_size: int = 8192) -> str:
        """Compute SHA-256 hash of a file."""
        sha256_hash = hashlib.sha256()
        
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(chunk_size), b""):
                sha256_hash.update(chunk)
        
        return sha256_hash.hexdigest()
    
    @staticmethod
    def compute_sha256_from_data(data: bytes) -> str:
        """Compute SHA-256 hash of data in memory."""
        sha256_hash = hashlib.sha256()
        sha256_hash.update(data)
        return sha256_hash.hexdigest()
    
    @staticmethod
    def verify_file_integrity(file_path: str, expected_hash: str) -> bool:
        """Verify file integrity using SHA-256 hash."""
        actual_hash = FileHasher.compute_sha256(file_path)
        return actual_hash.lower() == expected_hash.lower()


class SecureFileTransfer:
    """Secure file transfer with integrity checking and error recovery."""
    
    def __init__(self, chunk_size: int = 64 * 1024, logger: Optional[logging.Logger] = None):
        self.chunk_size = chunk_size
        self.logger = logger or logging.getLogger(__name__)
        self.progress_callbacks: List[Callable[[TransferProgress], None]] = []
        self.active_transfers: Dict[str, TransferProgress] = {}
        self.transfer_lock = threading.Lock()
    
    def add_progress_callback(self, callback: Callable[[TransferProgress], None]):
        """Add callback for transfer progress updates."""
        self.progress_callbacks.append(callback)
    
    def prepare_file_for_transfer(self, file_path: str, compression: str = "none") -> Optional[FileMetadata]:
        """Prepare file metadata for transfer."""
        try:
            file_path = Path(file_path)
            if not file_path.exists():
                self.logger.error(f"File not found: {file_path}")
                return None
            
            file_size = file_path.stat().st_size
            
            # Compute SHA-256 hash
            self.logger.info(f"Computing SHA-256 hash for {file_path.name}")
            sha256_hash = FileHasher.compute_sha256(str(file_path))
            
            # Calculate chunks
            total_chunks = (file_size + self.chunk_size - 1) // self.chunk_size
            
            metadata = FileMetadata(
                filename=file_path.name,
                file_size=file_size,
                sha256_hash=sha256_hash,
                chunk_size=self.chunk_size,
                total_chunks=total_chunks,
                compression=compression
            )
            
            self.logger.info(f"Prepared file for transfer: {file_path.name} "
                           f"({file_size} bytes, {total_chunks} chunks, hash: {sha256_hash[:16]}...)")
            
            return metadata
            
        except Exception as e:
            self.logger.error(f"Failed to prepare file for transfer: {e}")
            return None
    
    def send_file(self, file_path: str, socket_conn: socket.socket, 
                 compression: str = "none") -> bool:
        """Send file over socket with integrity checking."""
        try:
            # Prepare file metadata
            metadata = self.prepare_file_for_transfer(file_path, compression)
            if not metadata:
                return False
            
            # Send metadata
            metadata_json = metadata.to_json().encode('utf-8')
            metadata_length = len(metadata_json)
            
            # Send metadata length and data
            socket_conn.sendall(struct.pack('!I', metadata_length))
            socket_conn.sendall(metadata_json)
            
            # Initialize progress tracking
            progress = TransferProgress(
                filename=metadata.filename,
                total_size=metadata.file_size,
                transferred_size=0,
                chunks_completed=0,
                total_chunks=metadata.total_chunks,
                start_time=time.time(),
                last_update=time.time()
            )
            
            with self.transfer_lock:
                self.active_transfers[metadata.filename] = progress
            
            # Send file chunks
            with open(file_path, 'rb') as file:
                chunk_number = 0
                
                while chunk_number < metadata.total_chunks:
                    # Read chunk
                    chunk_data = file.read(self.chunk_size)
                    if not chunk_data:
                        break
                    
                    # Compress if requested
                    if compression == "gzip":
                        chunk_data = zlib.compress(chunk_data)
                    
                    # Send chunk header (chunk number and size)
                    chunk_header = struct.pack('!II', chunk_number, len(chunk_data))
                    socket_conn.sendall(chunk_header)
                    
                    # Send chunk data
                    socket_conn.sendall(chunk_data)
                    
                    # Update progress
                    original_chunk_size = len(chunk_data) if compression == "none" else self.chunk_size
                    progress.update(original_chunk_size)
                    
                    # Notify progress callbacks
                    for callback in self.progress_callbacks:
                        try:
                            callback(progress)
                        except Exception as e:
                            self.logger.error(f"Error in progress callback: {e}")
                    
                    chunk_number += 1
                    
                    # Small delay to prevent overwhelming the network
                    time.sleep(0.001)
            
            # Send end marker
            socket_conn.sendall(struct.pack('!II', 0xFFFFFFFF, 0))
            
            # Clean up progress tracking
            with self.transfer_lock:
                if metadata.filename in self.active_transfers:
                    del self.active_transfers[metadata.filename]
            
            self.logger.info(f"File transfer completed: {metadata.filename}")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to send file: {e}")
            return False
    
    def receive_file(self, socket_conn: socket.socket, output_dir: str, 
                    verify_integrity: bool = True) -> Optional[str]:
        """Receive file over socket with integrity checking."""
        try:
            # Receive metadata length
            metadata_length_data = self._recv_exact(socket_conn, 4)
            if not metadata_length_data:
                self.logger.error("Failed to receive metadata length")
                return None
            
            metadata_length = struct.unpack('!I', metadata_length_data)[0]
            
            # Receive metadata
            metadata_json_data = self._recv_exact(socket_conn, metadata_length)
            if not metadata_json_data:
                self.logger.error("Failed to receive metadata")
                return None
            
            metadata_json = metadata_json_data.decode('utf-8')
            metadata = FileMetadata.from_json(metadata_json)
            
            self.logger.info(f"Receiving file: {metadata.filename} "
                           f"({metadata.file_size} bytes, {metadata.total_chunks} chunks)")
            
            # Prepare output file
            output_path = Path(output_dir) / metadata.filename
            output_path.parent.mkdir(parents=True, exist_ok=True)
            
            # Initialize progress tracking
            progress = TransferProgress(
                filename=metadata.filename,
                total_size=metadata.file_size,
                transferred_size=0,
                chunks_completed=0,
                total_chunks=metadata.total_chunks,
                start_time=time.time(),
                last_update=time.time()
            )
            
            with self.transfer_lock:
                self.active_transfers[metadata.filename] = progress
            
            # Receive file chunks
            with open(output_path, 'wb') as output_file:
                chunks_received = 0
                
                while chunks_received < metadata.total_chunks:
                    # Receive chunk header
                    chunk_header_data = self._recv_exact(socket_conn, 8)
                    if not chunk_header_data:
                        break
                    
                    chunk_number, chunk_size = struct.unpack('!II', chunk_header_data)
                    
                    # Check for end marker
                    if chunk_number == 0xFFFFFFFF and chunk_size == 0:
                        break
                    
                    # Receive chunk data
                    chunk_data = self._recv_exact(socket_conn, chunk_size)
                    if not chunk_data:
                        break
                    
                    # Decompress if needed
                    if metadata.compression == "gzip":
                        chunk_data = zlib.decompress(chunk_data)
                    
                    # Write chunk to file
                    output_file.write(chunk_data)
                    
                    # Update progress
                    progress.update(len(chunk_data))
                    
                    # Notify progress callbacks
                    for callback in self.progress_callbacks:
                        try:
                            callback(progress)
                        except Exception as e:
                            self.logger.error(f"Error in progress callback: {e}")
                    
                    chunks_received += 1
            
            # Clean up progress tracking
            with self.transfer_lock:
                if metadata.filename in self.active_transfers:
                    del self.active_transfers[metadata.filename]
            
            # Verify file integrity
            if verify_integrity:
                self.logger.info(f"Verifying integrity of {metadata.filename}")
                if FileHasher.verify_file_integrity(str(output_path), metadata.sha256_hash):
                    self.logger.info(f"File integrity verified: {metadata.filename}")
                else:
                    self.logger.error(f"File integrity verification failed: {metadata.filename}")
                    output_path.unlink()  # Delete corrupted file
                    return None
            
            self.logger.info(f"File transfer completed: {output_path}")
            return str(output_path)
            
        except Exception as e:
            self.logger.error(f"Failed to receive file: {e}")
            return None
    
    def _recv_exact(self, sock: socket.socket, length: int) -> Optional[bytes]:
        """Receive exact number of bytes from socket."""
        data = b""
        while len(data) < length:
            chunk = sock.recv(length - len(data))
            if not chunk:
                return None
            data += chunk
        return data
    
    def get_transfer_progress(self, filename: str) -> Optional[TransferProgress]:
        """Get progress for an active transfer."""
        with self.transfer_lock:
            return self.active_transfers.get(filename)
    
    def get_active_transfers(self) -> Dict[str, TransferProgress]:
        """Get all active transfers."""
        with self.transfer_lock:
            return self.active_transfers.copy()


class FileIntegrityVerifier:
    """Batch file integrity verification utilities."""
    
    def __init__(self, logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
    
    def create_manifest(self, directory: str, output_file: str) -> bool:
        """Create integrity manifest for all files in directory."""
        try:
            directory_path = Path(directory)
            if not directory_path.exists():
                self.logger.error(f"Directory not found: {directory}")
                return False
            
            manifest = {}
            
            # Compute hashes for all files
            for file_path in directory_path.rglob('*'):
                if file_path.is_file():
                    relative_path = file_path.relative_to(directory_path)
                    self.logger.debug(f"Computing hash for {relative_path}")
                    
                    file_hash = FileHasher.compute_sha256(str(file_path))
                    file_size = file_path.stat().st_size
                    
                    manifest[str(relative_path)] = {
                        "sha256": file_hash,
                        "size": file_size,
                        "timestamp": file_path.stat().st_mtime
                    }
            
            # Write manifest
            with open(output_file, 'w') as f:
                json.dump(manifest, f, indent=2)
            
            self.logger.info(f"Created integrity manifest with {len(manifest)} files: {output_file}")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to create integrity manifest: {e}")
            return False
    
    def verify_manifest(self, directory: str, manifest_file: str) -> Tuple[bool, List[str]]:
        """Verify files against integrity manifest."""
        try:
            directory_path = Path(directory)
            
            with open(manifest_file, 'r') as f:
                manifest = json.load(f)
            
            errors = []
            verified_count = 0
            
            for relative_path, file_info in manifest.items():
                file_path = directory_path / relative_path
                
                if not file_path.exists():
                    errors.append(f"Missing file: {relative_path}")
                    continue
                
                # Check file size
                actual_size = file_path.stat().st_size
                expected_size = file_info["size"]
                
                if actual_size != expected_size:
                    errors.append(f"Size mismatch for {relative_path}: "
                                f"expected {expected_size}, got {actual_size}")
                    continue
                
                # Check hash
                actual_hash = FileHasher.compute_sha256(str(file_path))
                expected_hash = file_info["sha256"]
                
                if actual_hash.lower() != expected_hash.lower():
                    errors.append(f"Hash mismatch for {relative_path}")
                    continue
                
                verified_count += 1
            
            success = len(errors) == 0
            self.logger.info(f"Manifest verification: {verified_count} files verified, "
                           f"{len(errors)} errors")
            
            return success, errors
            
        except Exception as e:
            self.logger.error(f"Failed to verify manifest: {e}")
            return False, [str(e)]


def create_secure_transfer_client(host: str, port: int, logger: Optional[logging.Logger] = None) -> Optional[SecureFileTransfer]:
    """Create a secure file transfer client."""
    logger = logger or logging.getLogger(__name__)
    
    try:
        # Create socket connection
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((host, port))
        
        # Create transfer manager
        transfer = SecureFileTransfer(logger=logger)
        
        logger.info(f"Connected to secure file transfer server at {host}:{port}")
        return transfer
        
    except Exception as e:
        logger.error(f"Failed to create secure transfer client: {e}")
        return None


if __name__ == "__main__":
    # Example usage
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    
    # Create test file
    test_file = "/tmp/test_file.txt"
    with open(test_file, 'w') as f:
        f.write("This is a test file for secure transfer.\n" * 100)
    
    # Test file hashing
    logger.info("Testing file hashing...")
    hash_value = FileHasher.compute_sha256(test_file)
    logger.info(f"SHA-256 hash: {hash_value}")
    
    # Test file transfer (simulation)
    transfer = SecureFileTransfer(logger=logger)
    
    # Add progress callback
    def progress_callback(progress: TransferProgress):
        logger.info(f"Transfer progress: {progress.filename} - "
                   f"{progress.progress_percent:.1f}% "
                   f"({progress.speed_bps/1024:.1f} KB/s)")
    
    transfer.add_progress_callback(progress_callback)
    
    # Prepare file for transfer
    metadata = transfer.prepare_file_for_transfer(test_file)
    if metadata:
        logger.info(f"File prepared for transfer: {metadata.to_json()}")
    
    # Test integrity verification
    logger.info("Testing integrity verification...")
    if FileHasher.verify_file_integrity(test_file, hash_value):
        logger.info("File integrity verification passed")
    else:
        logger.error("File integrity verification failed")
    
    # Test manifest creation
    logger.info("Testing manifest creation...")
    verifier = FileIntegrityVerifier(logger)
    if verifier.create_manifest("/tmp", "/tmp/manifest.json"):
        logger.info("Manifest created successfully")
        
        # Test manifest verification
        success, errors = verifier.verify_manifest("/tmp", "/tmp/manifest.json")
        if success:
            logger.info("Manifest verification passed")
        else:
            logger.error(f"Manifest verification failed: {errors}")
    
    # Clean up
    os.unlink(test_file)
    if os.path.exists("/tmp/manifest.json"):
        os.unlink("/tmp/manifest.json")
    
    logger.info("File integrity test completed")