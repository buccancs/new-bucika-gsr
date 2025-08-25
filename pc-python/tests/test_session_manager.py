#!/usr/bin/env python3
"""
Test suite for session management
"""

import unittest
import asyncio
import tempfile
import shutil
from pathlib import Path
from src.bucika_gsr_pc.session_manager import SessionManager, SessionState
from src.bucika_gsr_pc.protocol import GSRSample


class TestSessionManager(unittest.TestCase):
    """Test session management functionality"""
    
    def setUp(self):
        """Set up test environment"""
        self.temp_dir = tempfile.mkdtemp()
        self.session_manager = SessionManager(base_path=Path(self.temp_dir))
    
    def tearDown(self):
        """Clean up test environment"""
        shutil.rmtree(self.temp_dir)
    
    def test_start_session(self):
        """Test session creation"""
        async def run_test():
            session_id = await self.session_manager.start_session(
                session_name="Test Session",
                device_id="device-123",
                participant_id="P001",
                metadata={"test": "data"}
            )
            
            # Verify session was created
            self.assertIsNotNone(session_id)
            self.assertIn("device-123", self.session_manager.active_sessions)
            
            session = self.session_manager.get_active_session("device-123")
            self.assertIsNotNone(session)
            self.assertEqual(session.session_name, "Test Session")
            self.assertEqual(session.participant_id, "P001")
            self.assertEqual(session.state, SessionState.RECORDING)
        
        asyncio.run(run_test())
    
    def test_stop_session(self):
        """Test session stopping"""
        async def run_test():
            # Start session
            session_id = await self.session_manager.start_session(
                session_name="Test Session", 
                device_id="device-123"
            )
            
            # Stop session
            await self.session_manager.stop_session("device-123")
            
            # Verify session is no longer active
            self.assertNotIn("device-123", self.session_manager.active_sessions)
            
            # But should still exist in all sessions
            session = self.session_manager.get_session_by_id(session_id)
            self.assertIsNotNone(session)
            self.assertEqual(session.state, SessionState.DONE)
        
        asyncio.run(run_test())
    
    def test_gsr_sample_storage(self):
        """Test GSR sample storage"""
        async def run_test():
            # Start session
            await self.session_manager.start_session(
                session_name="GSR Test", 
                device_id="device-123"
            )
            
            # Create test samples
            samples = [
                GSRSample(
                    t_mono_ns=1234567890123456789,
                    t_utc_ns=1234567890123456789,
                    seq=1,
                    gsr_raw_uS=2.5,
                    gsr_filt_uS=2.48,
                    temp_C=32.1,
                    flag_spike=False,
                    flag_sat=False,
                    flag_dropout=False
                ),
                GSRSample(
                    t_mono_ns=1234567890223456789,
                    t_utc_ns=1234567890223456789,
                    seq=2,
                    gsr_raw_uS=2.6,
                    gsr_filt_uS=2.58,
                    temp_C=32.2,
                    flag_spike=False,
                    flag_sat=False,
                    flag_dropout=False
                )
            ]
            
            # Store samples
            await self.session_manager.store_gsr_samples("device-123", samples)
            
            # Verify samples were stored
            session = self.session_manager.get_active_session("device-123")
            self.assertEqual(len(session.gsr_samples), 2)
            self.assertEqual(session.gsr_samples[0].seq, 1)
            self.assertEqual(session.gsr_samples[1].seq, 2)
        
        asyncio.run(run_test())
    
    def test_sync_mark_recording(self):
        """Test sync mark recording"""
        async def run_test():
            # Start session
            await self.session_manager.start_session(
                session_name="Sync Test",
                device_id="device-123"
            )
            
            # Record sync mark
            await self.session_manager.record_sync_mark(
                device_id="device-123",
                mark_id="STIM_START",
                description="Stimulus presentation started"
            )
            
            # Verify sync mark was recorded
            session = self.session_manager.get_active_session("device-123")
            self.assertEqual(len(session.sync_marks), 1)
            
            sync_mark = session.sync_marks[0]
            self.assertEqual(sync_mark["mark_id"], "STIM_START")
            self.assertEqual(sync_mark["description"], "Stimulus presentation started")
            self.assertIsNotNone(sync_mark["timestamp"])
            self.assertIsInstance(sync_mark["timestamp_ns"], int)
        
        asyncio.run(run_test())
    
    def test_multiple_sync_marks(self):
        """Test recording multiple sync marks"""
        async def run_test():
            # Start session
            await self.session_manager.start_session(
                session_name="Multi Sync Test",
                device_id="device-123"
            )
            
            # Record multiple sync marks
            marks = [
                ("BASELINE_START", "Baseline recording started"),
                ("STIM_START", "Stimulus presentation"),
                ("STIM_END", "Stimulus ended"),
                ("RECOVERY_START", "Recovery period")
            ]
            
            for mark_id, description in marks:
                await self.session_manager.record_sync_mark(
                    device_id="device-123",
                    mark_id=mark_id,
                    description=description
                )
            
            # Verify all sync marks were recorded
            session = self.session_manager.get_active_session("device-123")
            self.assertEqual(len(session.sync_marks), 4)
            
            # Check order is preserved
            for i, (expected_id, expected_desc) in enumerate(marks):
                self.assertEqual(session.sync_marks[i]["mark_id"], expected_id)
                self.assertEqual(session.sync_marks[i]["description"], expected_desc)
        
        asyncio.run(run_test())


if __name__ == "__main__":
    unittest.main()