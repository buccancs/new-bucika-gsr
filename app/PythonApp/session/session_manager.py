import json
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional
from ..utils.logging_config import get_logger
logger = get_logger(__name__)
class SessionManager:
    def __init__(self, base_recordings_dir: str = "recordings"):
        self.logger = get_logger(__name__)
        self.base_recordings_dir = Path(base_recordings_dir)
        self.current_session: Optional[Dict] = None
        self.session_history: List[Dict] = []
        self.base_recordings_dir.mkdir(parents=True, exist_ok=True)
        logger.info(
            f"session manager initialized with base directory: {self.base_recordings_dir}"
        )
    @staticmethod
    def validate_session_name(session_name: str) -> bool:
        if not session_name:
            return False
        if len(session_name) > 80:
            return False
        import re
        if not re.match("^[a-zA-Z0-9\\s\\-_]+$", session_name):
            return False
        return True
    @staticmethod
    def generate_device_filename(
        device_id: str,
        file_type: str,
        extension: str,
        timestamp: Optional[datetime] = None,
    ) -> str:
        if timestamp is None:
            timestamp = datetime.now()
        timestamp_str = timestamp.strftime("%Y%m%d_%H%M%S")
        return f"{device_id}_{file_type}_{timestamp_str}.{extension}"
    def create_session(self, session_name: Optional[str] = None) -> Dict:
        logger.info(f"creating new session with name: {session_name}")
        timestamp = datetime.now()
        if session_name is None:
            session_id = timestamp.strftime("session_%Y%m%d_%H%M%S")
        else:
            safe_name = "".join(
                c if c.isalnum() or c in ("-", "_") else "_"
                for c in session_name.replace(" ", "_")
            ).strip("_")
            if not safe_name or len(safe_name) == 0:
                safe_name = "session"
            elif len(safe_name) > 50:
                safe_name = safe_name[:50].rstrip("_")
            session_id = f"{safe_name}_{timestamp.strftime('%Y%m%d_%H%M%S')}"
        session_folder = self.base_recordings_dir / session_id
        session_folder.mkdir(parents=True, exist_ok=True)
        session_info = {
            "session_id": session_id,
            "session_name": session_name or session_id,
            "folder_path": str(session_folder),
            "start_time": timestamp.isoformat(),
            "end_time": None,
            "duration": None,
            "devices": {},
            "files": {},
            "status": "active",
        }
        metadata_file = session_folder / "session_metadata.json"
        with open(metadata_file, "w") as f:
            json.dump(session_info, f, indent=2)
        self.current_session = session_info
        logger.info(f"session created: {session_id}")
        return session_info
    def end_session(self) -> Optional[Dict]:
        if not self.current_session:
            logger.warning("no active session to end")
            return None
        end_time = datetime.now()
        start_time = datetime.fromisoformat(self.current_session["start_time"])
        duration = (end_time - start_time).total_seconds()
        self.current_session["end_time"] = end_time.isoformat()
        self.current_session["duration"] = duration
        self.current_session["status"] = "completed"
        metadata_file = (
            Path(self.current_session["folder_path"]) / "session_metadata.json"
        )
        with open(metadata_file, "w") as f:
            json.dump(self.current_session, f, indent=2)
        self.session_history.append(self.current_session.copy())
        session_id = self.current_session["session_id"]
        logger.info(f"session ended: {session_id} (duration: {duration:.1f}s)")
        completed_session = self.current_session
        self.current_session = None
        return completed_session
    def add_device_to_session(
        self, device_id: str, device_type: str, capabilities: List[str]
    ):
        if not self.current_session:
            print("[DEBUG_LOG] No active session to add device to")
            return
        device_info = {
            "device_type": device_type,
            "capabilities": capabilities,
            "added_time": datetime.now().isoformat(),
            "status": "connected",
        }
        self.current_session["devices"][device_id] = device_info
        self._update_session_metadata()
        print(f"[DEBUG_LOG] Device added to session: {device_id} ({device_type})")
    def add_file_to_session(
        self,
        device_id: str,
        file_type: str,
        file_path: str,
        file_size: Optional[int] = None,
    ):
        if not self.current_session:
            print("[DEBUG_LOG] No active session to add file to")
            return
        if device_id not in self.current_session["files"]:
            self.current_session["files"][device_id] = []
        file_info = {
            "file_type": file_type,
            "file_path": file_path,
            "file_size": file_size,
            "created_time": datetime.now().isoformat(),
        }
        self.current_session["files"][device_id].append(file_info)
        self._update_session_metadata()
        print(
            f"[DEBUG_LOG] File added to session: {device_id} - {file_type} ({file_path})"
        )
    def get_session_folder(self, session_id: Optional[str] = None) -> Optional[Path]:
        if session_id is None:
            if self.current_session:
                return Path(self.current_session["folder_path"])
            else:
                return None
        for session in self.session_history:
            if session["session_id"] == session_id:
                return Path(session["folder_path"])
        if self.current_session and self.current_session["session_id"] == session_id:
            return Path(self.current_session["folder_path"])
        return None
    def get_current_session(self) -> Optional[Dict]:
        return self.current_session.copy() if self.current_session else None
    def has_hand_segmentation_available(self) -> bool:
        try:
            import os
            import sys
            sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
            from hand_segmentation import SessionPostProcessor
            return True
        except ImportError:
            return False
    def trigger_post_session_processing(
        self,
        session_id: Optional[str] = None,
        enable_hand_segmentation: bool = True,
        segmentation_method: str = "mediapipe",
    ) -> Dict[str, any]:
        results = {
            "session_id": session_id,
            "hand_segmentation": {
                "enabled": enable_hand_segmentation,
                "available": self.has_hand_segmentation_available(),
                "success": False,
                "results": None,
                "error": None,
            },
        }
        if not enable_hand_segmentation:
            results["hand_segmentation"]["success"] = True
            return results
        if not self.has_hand_segmentation_available():
            results["hand_segmentation"][
                "error"
            ] = "Hand segmentation module not available"
            return results
        try:
            import os
            import sys
            sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
            from hand_segmentation import create_session_post_processor
            processor = create_session_post_processor(str(self.base_recordings_dir))
            target_session = session_id
            if not target_session:
                if self.session_history:
                    target_session = self.session_history[-1]["session_id"]
                elif self.current_session:
                    target_session = self.current_session["session_id"]
            if not target_session:
                results["hand_segmentation"]["error"] = "No session found to process"
                return results
            print(
                f"[INFO] Starting post-session hand segmentation for: {target_session}"
            )
            segmentation_results = processor.process_session(
                target_session,
                method=segmentation_method,
                output_cropped=True,
                output_masks=True,
            )
            results["hand_segmentation"]["success"] = True
            results["hand_segmentation"]["results"] = segmentation_results
            self._update_session_post_processing_status(target_session, True)
            print(f"[INFO] Post-session processing completed for: {target_session}")
        except Exception as e:
            error_msg = f"Error in post-session processing: {str(e)}"
            results["hand_segmentation"]["error"] = error_msg
            print(f"[ERROR] {error_msg}")
        return results
    def _update_session_post_processing_status(self, session_id: str, completed: bool):
        try:
            session_folder = self.get_session_folder(session_id)
            if session_folder:
                metadata_file = session_folder / "session_metadata.json"
                if metadata_file.exists():
                    import json
                    with open(metadata_file, "r") as f:
                        metadata = json.load(f)
                    metadata["post_processing"] = {
                        "hand_segmentation_completed": completed,
                        "hand_segmentation_timestamp": (
                            datetime.now().isoformat() if completed else None
                        ),
                    }
                    with open(metadata_file, "w") as f:
                        json.dump(metadata, f, indent=2)
        except Exception as e:
            print(f"[WARNING] Failed to update post-processing status: {e}")
    def _update_session_metadata(self):
        if not self.current_session:
            return
        metadata_file = (
            Path(self.current_session["folder_path"]) / "session_metadata.json"
        )
        try:
            with open(metadata_file, "w") as f:
                json.dump(self.current_session, f, indent=2)
        except Exception as e:
            print(f"[DEBUG_LOG] Failed to update session metadata: {e}")
if __name__ == "__main__":
    print("[DEBUG_LOG] Testing SessionManager...")
    manager = SessionManager("test_recordings")
    session = manager.create_session("test_session")
    print(f"Created session: {session['session_id']}")
    manager.add_device_to_session("pc_webcam", "pc_webcam", ["video_recording"])
    manager.add_device_to_session(
        "phone_1", "android_phone", ["rgb_video", "thermal_video"]
    )
    manager.add_file_to_session("pc_webcam", "webcam_video", "webcam_test.mp4", 1024000)
    manager.add_file_to_session("phone_1", "rgb_video", "phone1_rgb.mp4", 2048000)
    completed = manager.end_session()
    print(
        f"Completed session: {completed['session_id']} (duration: {completed['duration']:.1f}s)"
    )
    print("[DEBUG_LOG] SessionManager test completed successfully")