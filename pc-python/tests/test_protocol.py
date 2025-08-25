#!/usr/bin/env python3
"""
Test suite for protocol message handling
"""

import unittest
from datetime import datetime
from src.bucika_gsr_pc.protocol import (
    MessageType, MessageEnvelope, HelloPayload, RegisterPayload,
    StartPayload, SyncMarkPayload, GSRSamplePayload, GSRSample,
    parse_message_payload
)


class TestProtocol(unittest.TestCase):
    """Test protocol message handling"""
    
    def test_hello_payload_creation(self):
        """Test HelloPayload creation and serialization"""
        payload = HelloPayload(
            deviceName="Test Device",
            capabilities=["GSR", "THERMAL", "VIDEO"],
            batteryLevel=85,
            version="1.0.0"
        )
        
        self.assertEqual(payload.version, "1.0.0")
        self.assertEqual(len(payload.capabilities), 3)
        self.assertIn("GSR", payload.capabilities)
        self.assertEqual(payload.batteryLevel, 85)
    
    def test_register_payload_creation(self):
        """Test RegisterPayload creation"""
        payload = RegisterPayload(
            accepted=True,
            reason="Device registered successfully",
            syncPort=9123
        )
        
        self.assertTrue(payload.accepted)
        self.assertEqual(payload.reason, "Device registered successfully")
        self.assertEqual(payload.syncPort, 9123)
    
    def test_start_payload_creation(self):
        """Test StartPayload creation"""
        payload = StartPayload(
            sessionName="Test Session",
            participantId="P001",
            metadata={"experiment": "stress_test"}
        )
        
        self.assertEqual(payload.sessionName, "Test Session")
        self.assertEqual(payload.participantId, "P001")
        self.assertEqual(payload.metadata["experiment"], "stress_test")
    
    def test_sync_mark_payload_creation(self):
        """Test SyncMarkPayload creation"""
        payload = SyncMarkPayload(
            markId="MARK001",
            description="Start of stimulus"
        )
        
        self.assertEqual(payload.markId, "MARK001")
        self.assertEqual(payload.description, "Start of stimulus")
    
    def test_gsr_sample_payload_creation(self):
        """Test GSRSamplePayload creation"""
        sample = GSRSample(
            t_mono_ns=1234567890123456789,
            t_utc_ns=1234567890123456789,
            seq=100,
            gsr_raw_uS=2.5,
            gsr_filt_uS=2.48,
            temp_C=32.1,
            flag_spike=False,
            flag_sat=False,
            flag_dropout=False
        )
        
        payload = GSRSamplePayload(samples=[sample])
        
        self.assertEqual(len(payload.samples), 1)
        self.assertEqual(payload.samples[0].seq, 100)
        self.assertEqual(payload.samples[0].gsr_raw_uS, 2.5)
    
    def test_message_envelope_creation(self):
        """Test MessageEnvelope creation"""
        payload = HelloPayload(
            deviceName="Test Device",
            capabilities=["GSR"],
            batteryLevel=85,
            version="1.0.0"
        )
        
        envelope = MessageEnvelope.create(
            msg_id="test-123",
            msg_type=MessageType.HELLO,
            device_id="device-456",
            payload=payload
        )
        
        self.assertEqual(envelope.id, "test-123")
        self.assertEqual(envelope.type, MessageType.HELLO)
        self.assertEqual(envelope.deviceId, "device-456")
        self.assertIsInstance(envelope.ts, int)
        self.assertGreater(envelope.ts, 0)


if __name__ == "__main__":
    unittest.main()