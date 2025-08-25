"""
Session manager for handling GSR recording sessions and data storage.
Maintains compatibility with the existing session management features.
"""

import asyncio
import csv
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Dict, List, Optional, Any
import aiofiles
from loguru import logger

from .protocol import GSRSample


class SessionState(str, Enum):
    """Session lifecycle states"""
    NEW = "NEW"
    ARMED = "ARMED" 
    RECORDING = "RECORDING"
    FINALISING = "FINALISING"
    DONE = "DONE"
    FAILED = "FAILED"


class Session:
    """Represents a recording session"""
    
    def __init__(self, session_id: str, session_name: str, device_id: str,
                 participant_id: Optional[str] = None, metadata: Optional[Dict[str, Any]] = None):
        self.session_id = session_id
        self.session_name = session_name
        self.device_id = device_id
        self.participant_id = participant_id
        self.metadata = metadata or {}
        
        self.state = SessionState.NEW
        self.created_at = datetime.now()
        self.started_at: Optional[datetime] = None
        self.ended_at: Optional[datetime] = None
        
        # GSR data storage
        self.gsr_samples: List[GSRSample] = []
        self.gsr_file_path: Optional[Path] = None
        
        # Uploaded files
        self.uploaded_files: List[str] = []
        
        # Sync marks for synchronization events
        self.sync_marks: List[Dict[str, Any]] = []
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert session to dictionary for serialization"""
        return {
            "session_id": self.session_id,
            "session_name": self.session_name,
            "device_id": self.device_id,
            "participant_id": self.participant_id,
            "metadata": self.metadata,
            "state": self.state.value,
            "created_at": self.created_at.isoformat(),
            "started_at": self.started_at.isoformat() if self.started_at else None,
            "ended_at": self.ended_at.isoformat() if self.ended_at else None,
            "gsr_samples_count": len(self.gsr_samples),
            "uploaded_files": self.uploaded_files,
            "sync_marks": self.sync_marks
        }


class SessionManager:
    """Manages recording sessions and data storage"""
    
    def __init__(self, base_path: Optional[Path] = None):
        self.base_path = base_path or Path("./sessions")
        self.base_path.mkdir(exist_ok=True)
        
        # Active sessions by device
        self.active_sessions: Dict[str, Session] = {}
        
        # All sessions (for history/monitoring)
        self.all_sessions: Dict[str, Session] = {}
        
        # GSR file writers for active sessions
        self.gsr_files: Dict[str, Any] = {}
    
    async def start_session(self, session_name: str, device_id: str,
                           participant_id: Optional[str] = None,
                           metadata: Optional[Dict[str, Any]] = None) -> str:
        """Start a new recording session"""
        # Generate session ID
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        session_id = f"{device_id}_{timestamp}"
        
        # Create session
        session = Session(
            session_id=session_id,
            session_name=session_name,
            device_id=device_id,
            participant_id=participant_id,
            metadata=metadata
        )
        
        # Create session directory
        session_dir = self.base_path / session_id
        session_dir.mkdir(exist_ok=True)
        
        # Set up GSR data file
        gsr_filename = f"gsr_data_{timestamp}.csv"
        session.gsr_file_path = session_dir / gsr_filename
        
        # Initialize GSR CSV file
        await self._init_gsr_file(session)
        
        # Update session state
        session.state = SessionState.RECORDING
        session.started_at = datetime.now()
        
        # Register session
        self.active_sessions[device_id] = session
        self.all_sessions[session_id] = session
        
        logger.info(f"Started session {session_id} for device {device_id}")
        
        return session_id
    
    async def stop_session(self, device_id: str):
        """Stop the active session for a device"""
        session = self.active_sessions.get(device_id)
        if not session:
            logger.warning(f"No active session found for device {device_id}")
            return
        
        # Update session state
        session.state = SessionState.FINALISING
        session.ended_at = datetime.now()
        
        # Close GSR file
        await self._close_gsr_file(device_id)
        
        # Finalize session
        session.state = SessionState.DONE
        
        # Remove from active sessions
        del self.active_sessions[device_id]
        
        logger.info(f"Stopped session {session.session_id}")
    
    async def store_gsr_samples(self, device_id: str, samples: List[GSRSample]):
        """Store GSR samples for an active session"""
        session = self.active_sessions.get(device_id)
        if not session:
            logger.warning(f"No active session found for device {device_id}")
            return
        
        # Add to session samples
        session.gsr_samples.extend(samples)
        
        # Write to CSV file
        await self._write_gsr_samples(device_id, samples)
        
        logger.debug(f"Stored {len(samples)} GSR samples for session {session.session_id}")
    
    async def record_sync_mark(self, device_id: str, mark_id: str, description: Optional[str] = None):
        """Record a synchronization mark for an active session"""
        session = self.active_sessions.get(device_id)
        if not session:
            logger.warning(f"No active session found for device {device_id}")
            return
        
        # Create sync mark record
        sync_mark = {
            "mark_id": mark_id,
            "timestamp": datetime.now().isoformat(),
            "timestamp_ns": int(datetime.now().timestamp() * 1_000_000_000),
            "description": description,
            "session_id": session.session_id
        }
        
        # Add to session sync marks
        session.sync_marks.append(sync_mark)
        
        # Also write to a separate sync marks file for the session
        await self._write_sync_mark(session, sync_mark)
        
        logger.info(f"Recorded sync mark '{mark_id}' for session {session.session_id}")
    
    
    async def save_uploaded_file(self, device_id: str, filename: str, file_data: bytes):
        """Save an uploaded file for the session"""
        session = self.active_sessions.get(device_id)
        if session:
            session_dir = self.base_path / session.session_id
        else:
            # If no active session, create a generic uploads directory
            session_dir = self.base_path / f"{device_id}_uploads"
        
        session_dir.mkdir(exist_ok=True)
        file_path = session_dir / filename
        
        # Save file
        async with aiofiles.open(file_path, 'wb') as f:
            await f.write(file_data)
        
        # Track in session if available
        if session:
            session.uploaded_files.append(filename)
        
        logger.info(f"Saved uploaded file: {filename} ({len(file_data)} bytes)")
    
    async def _init_gsr_file(self, session: Session):
        """Initialize GSR CSV file for a session"""
        if not session.gsr_file_path:
            return
        
        # Open file for writing
        file_handle = await aiofiles.open(session.gsr_file_path, 'w', newline='')
        
        # Write header
        header = [
            'Timestamp_ns',
            'DateTime_UTC', 
            'Sequence',
            'GSR_Raw_uS',
            'GSR_Filtered_uS',
            'Temperature_C',
            'Flag_Spike',
            'Flag_Saturation',
            'Flag_Dropout'
        ]
        
        # Write header as CSV line
        header_line = ','.join(header) + '\n'
        await file_handle.write(header_line)
        await file_handle.flush()
        
        # Store references
        self.gsr_files[session.device_id] = file_handle
    
    async def _write_gsr_samples(self, device_id: str, samples: List[GSRSample]):
        """Write GSR samples to CSV file"""
        file_handle = self.gsr_files.get(device_id)
        
        if not file_handle:
            logger.warning(f"No GSR file writer found for device {device_id}")
            return
        
        try:
            for sample in samples:
                # Convert timestamp to human-readable format
                dt = datetime.fromtimestamp(sample.t_utc_ns / 1_000_000_000)
                dt_str = dt.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]  # microsecond precision
                
                row = [
                    sample.t_utc_ns,
                    dt_str,
                    sample.seq,
                    sample.gsr_raw_uS,
                    sample.gsr_filt_uS,
                    sample.temp_C,
                    sample.flag_spike,
                    sample.flag_sat,
                    sample.flag_dropout
                ]
                
                # Write CSV row
                csv_row = ','.join(str(field) for field in row) + '\n'
                await file_handle.write(csv_row)
            
            await file_handle.flush()
            
        except Exception as e:
            logger.error(f"Failed to write GSR samples: {e}")
    
    async def _write_sync_mark(self, session: Session, sync_mark: Dict[str, Any]):
        """Write sync mark to session's sync marks file"""
        try:
            # Create sync marks file path
            session_dir = self.base_path / session.session_id
            sync_marks_file = session_dir / "sync_marks.csv"
            
            # Check if file exists to decide on header
            file_exists = sync_marks_file.exists()
            
            # Write sync mark 
            async with aiofiles.open(sync_marks_file, 'a', newline='') as f:
                # Write header if new file
                if not file_exists:
                    header = "Mark_ID,Timestamp_ns,DateTime_UTC,Description,Session_ID\n"
                    await f.write(header)
                
                # Convert timestamp to human-readable format
                dt = datetime.fromisoformat(sync_mark["timestamp"])
                dt_str = dt.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
                
                # Write sync mark row
                row = [
                    sync_mark["mark_id"],
                    sync_mark["timestamp_ns"],
                    dt_str,
                    sync_mark["description"] or "",
                    sync_mark["session_id"]
                ]
                
                csv_row = ','.join(f'"{field}"' if isinstance(field, str) and ',' in field else str(field) for field in row) + '\n'
                await f.write(csv_row)
                await f.flush()
                
        except Exception as e:
            logger.error(f"Failed to write sync mark: {e}")
    
    
    async def _close_gsr_file(self, device_id: str):
        """Close GSR file for a device"""
        file_handle = self.gsr_files.get(device_id)
        if file_handle:
            try:
                await file_handle.close()
                del self.gsr_files[device_id]
            except Exception as e:
                logger.error(f"Error closing GSR file: {e}")
    
    def get_active_session(self, device_id: str) -> Optional[Session]:
        """Get the active session for a device"""
        return self.active_sessions.get(device_id)
    
    def get_all_sessions(self) -> Dict[str, Session]:
        """Get all sessions"""
        return self.all_sessions.copy()
    
    def get_session_by_id(self, session_id: str) -> Optional[Session]:
        """Get a session by ID"""
        return self.all_sessions.get(session_id)
    
    def get_active_sessions(self) -> Dict[str, Session]:
        """Get all active sessions"""
        return self.active_sessions.copy()