import json
import os
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from .segmentation_engine import create_segmentation_engine
from .utils import ProcessingResult, SegmentationMethod
class SessionPostProcessor:
    def __init__(self, base_recordings_dir: str = "recordings"):
        self.base_recordings_dir = Path(base_recordings_dir)
        self.processing_history: List[Dict] = []
    def discover_sessions(self) -> List[str]:
        sessions = []
        if not self.base_recordings_dir.exists():
            print(
                f"[WARNING] Recordings directory not found: {self.base_recordings_dir}"
            )
            return sessions
        for item in self.base_recordings_dir.iterdir():
            if item.is_dir():
                video_files = self._find_video_files(item)
                if video_files:
                    sessions.append(item.name)
        sessions.sort()
        return sessions
    def get_session_videos(self, session_id: str) -> List[str]:
        session_dir = self.base_recordings_dir / session_id
        return self._find_video_files(session_dir)
    def _find_video_files(self, directory: Path) -> List[str]:
        video_extensions = {".mp4", ".avi", ".mov", ".mkv", ".wmv"}
        video_files = []
        if directory.exists():
            for file_path in directory.rglob("*"):
                if file_path.is_file() and file_path.suffix.lower() in video_extensions:
                    video_files.append(str(file_path))
        return video_files
    def process_session(
        self, session_id: str, method: str = "mediapipe", **config_kwargs
    ) -> Dict[str, ProcessingResult]:
        print(f"[INFO] Starting hand segmentation processing for session: {session_id}")
        session_dir = self.base_recordings_dir / session_id
        if not session_dir.exists():
            print(f"[ERROR] Session directory not found: {session_dir}")
            return {}
        video_files = self.get_session_videos(session_id)
        if not video_files:
            print(f"[WARNING] No video files found in session: {session_id}")
            return {}
        print(f"[INFO] Found {len(video_files)} video files to process")
        engine = create_segmentation_engine(method=method, **config_kwargs)
        if not engine.initialize():
            print(f"[ERROR] Failed to initialize segmentation engine")
            return {}
        results = {}
        try:
            for video_path in video_files:
                print(f"[INFO] Processing video: {video_path}")
                video_name = Path(video_path).stem
                output_dir = session_dir / f"hand_segmentation_{video_name}"
                result = engine.process_video(str(video_path), str(output_dir))
                results[video_path] = result
                if result.success:
                    print(f"[INFO] Successfully processed: {video_path}")
                    print(
                        f"       Frames: {result.processed_frames}, Detections: {result.detected_hands_count}"
                    )
                    print(f"       Time: {result.processing_time:.2f}s")
                else:
                    print(f"[ERROR] Failed to process: {video_path}")
                    print(f"        Error: {result.error_message}")
            self._save_session_summary(session_id, method, results)
        finally:
            engine.cleanup()
        print(f"[INFO] Completed processing session: {session_id}")
        return results
    def process_video_file(
        self,
        video_path: str,
        output_directory: Optional[str] = None,
        method: str = "mediapipe",
        **config_kwargs,
    ) -> ProcessingResult:
        video_path = Path(video_path)
        if not video_path.exists():
            result = ProcessingResult(
                input_video_path=str(video_path),
                output_directory="",
                error_message=f"Video file not found: {video_path}",
            )
            return result
        if output_directory is None:
            output_directory = (
                video_path.parent / f"hand_segmentation_{video_path.stem}"
            )
        engine = create_segmentation_engine(method=method, **config_kwargs)
        if not engine.initialize():
            result = ProcessingResult(
                input_video_path=str(video_path),
                output_directory=str(output_directory),
                error_message="Failed to initialize segmentation engine",
            )
            return result
        try:
            result = engine.process_video(str(video_path), str(output_directory))
            self.processing_history.append(
                {
                    "timestamp": datetime.now().isoformat(),
                    "video_path": str(video_path),
                    "output_directory": str(output_directory),
                    "method": method,
                    "success": result.success,
                    "processing_time": result.processing_time,
                    "detected_hands": result.detected_hands_count,
                }
            )
            return result
        finally:
            engine.cleanup()
    def _save_session_summary(
        self, session_id: str, method: str, results: Dict[str, ProcessingResult]
    ):
        session_dir = self.base_recordings_dir / session_id
        summary_path = session_dir / f"hand_segmentation_summary_{method}.json"
        summary = {
            "session_id": session_id,
            "processing_method": method,
            "processed_at": datetime.now().isoformat(),
            "total_videos": len(results),
            "successful_videos": sum(1 for r in results.values() if r.success),
            "failed_videos": sum(1 for r in results.values() if not r.success),
            "total_processing_time": sum(r.processing_time for r in results.values()),
            "total_detections": sum(r.detected_hands_count for r in results.values()),
            "videos": {},
        }
        for video_path, result in results.items():
            summary["videos"][video_path] = {
                "success": result.success,
                "processed_frames": result.processed_frames,
                "detected_hands": result.detected_hands_count,
                "processing_time": result.processing_time,
                "output_directory": result.output_directory,
                "error_message": result.error_message,
            }
        with open(summary_path, "w") as f:
            json.dump(summary, f, indent=2)
        print(f"[INFO] Session summary saved: {summary_path}")
    def get_processing_status(self, session_id: str) -> Dict[str, bool]:
        session_dir = self.base_recordings_dir / session_id
        video_files = self.get_session_videos(session_id)
        status = {}
        for video_path in video_files:
            video_name = Path(video_path).stem
            output_dir = session_dir / f"hand_segmentation_{video_name}"
            metadata_file = output_dir / "processing_metadata.json"
            is_processed = False
            if metadata_file.exists():
                try:
                    with open(metadata_file, "r") as f:
                        metadata = json.load(f)
                        is_processed = metadata.get("success", False)
                except:
                    pass
            status[video_path] = is_processed
        return status
    def get_available_methods(self) -> List[str]:
        return [method.value for method in SegmentationMethod]
    def cleanup_session_outputs(self, session_id: str):
        session_dir = self.base_recordings_dir / session_id
        if not session_dir.exists():
            return
        for item in session_dir.iterdir():
            if item.is_dir() and item.name.startswith("hand_segmentation_"):
                print(f"[INFO] Removing segmentation output: {item}")
                import shutil
                shutil.rmtree(item)
        for file_path in session_dir.glob("hand_segmentation_summary_*.json"):
            print(f"[INFO] Removing summary file: {file_path}")
            file_path.unlink()
def create_session_post_processor(
    base_recordings_dir: str = "recordings",
) -> SessionPostProcessor:
    return SessionPostProcessor(base_recordings_dir)