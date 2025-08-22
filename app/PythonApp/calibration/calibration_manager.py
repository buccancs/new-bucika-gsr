import json
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
import cv2
import numpy as np
from ..utils.logging_config import get_logger
from .calibration_processor import CalibrationProcessor
from .calibration_result import CalibrationResult
logger = get_logger(__name__)

# Import the unified CalibrationManager from calibration.py to avoid duplication
from .calibration import CalibrationManager as UnifiedCalibrationManager

class CalibrationManager(UnifiedCalibrationManager):
    def __init__(self, output_dir: str = "calibration_data"):
        # Initialize the base unified calibration manager
        super().__init__()
        
        self.logger = get_logger(__name__)
        self.logger.info(f"Enhanced CalibrationManager initialized")
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.processor = CalibrationProcessor()
        self.current_session = None
        self.captured_images = {}
        self.captured_frames = []
        self.calibration_results = {}
        self.pattern_type = "chessboard"
        self.min_images = 10
        self.is_capturing = False
        self.capture_count = {}
        logger.info(
            f"CalibrationManager initialized with output directory: {self.output_dir}"
        )
        logger.debug(
            f"Pattern: {self.pattern_type}, Size: {self.chessboard_size}, Square: {self.square_size}mm"
        )
    def start_calibration_session(
        self, device_ids: List[str], session_name: str = None
    ) -> Dict[str, Any]:
        logger.info(f"Starting calibration session for devices: {device_ids}")
        if self.is_capturing:
            logger.error(
                "Attempted to start calibration session while another is in progress"
            )
            raise RuntimeError("Calibration session already in progress")
        timestamp = datetime.now()
        if session_name is None:
            session_name = f"calibration_{timestamp.strftime('%Y%m%d_%H%M%S')}"
        session_folder = self.output_dir / session_name
        session_folder.mkdir(parents=True, exist_ok=True)
        logger.debug(f"Created session folder: {session_folder}")
        self.current_session = {
            "session_name": session_name,
            "session_folder": str(session_folder),
            "device_ids": device_ids,
            "start_time": timestamp.isoformat(),
            "pattern_type": self.pattern_type,
            "pattern_size": self.chessboard_size,
            "square_size": self.square_size,
            "status": "active",
        }
        for device_id in device_ids:
            self.captured_images[device_id] = {"rgb": [], "thermal": []}
            self.capture_count[device_id] = 0
        session_file = session_folder / "session_info.json"
        with open(session_file, "w") as f:
            json.dump(self.current_session, f, indent=2)
        logger.debug(f" Calibration session started: {session_name}")
        return self.current_session
    def capture_calibration_frame(self, device_server) -> Dict[str, Any]:
        if not self.current_session:
            raise RuntimeError("No active calibration session")
        if self.is_capturing:
            return {"success": False, "message": "Capture already in progress"}
        self.is_capturing = True
        capture_results = {
            "success": True,
            "message": "Calibration frames captured successfully",
            "device_results": {},
            "total_frames": {},
        }
        try:
            capture_command = {
                "cmd": "capture_calibration",
                "session_id": self.current_session["session_name"],
                "pattern_type": self.pattern_type,
                "pattern_size": self.chessboard_size,
            }
            device_count = device_server.broadcast_command(capture_command)
            logger.info(
                f"[DEBUG_LOG] Sent calibration capture command to {device_count} devices"
            )
            for device_id in self.current_session["device_ids"]:
                success = self._simulate_image_capture(device_id)
                if success:
                    self.capture_count[device_id] += 1
                    capture_results["device_results"][device_id] = {
                        "success": True,
                        "message": "Frame captured successfully",
                    }
                else:
                    capture_results["device_results"][device_id] = {
                        "success": False,
                        "message": "Pattern not detected in frame",
                    }
                capture_results["total_frames"][device_id] = self.capture_count[
                    device_id
                ]
            return capture_results
        except Exception as e:
            capture_results["success"] = False
            capture_results["message"] = f"Capture failed: {str(e)}"
            return capture_results
        finally:
            self.is_capturing = False
    def _simulate_image_capture(self, device_id: str) -> bool:
        rgb_image = np.random.randint(0, 255, (480, 640, 3), dtype=np.uint8)
        thermal_image = np.random.randint(0, 255, (240, 320), dtype=np.uint8)
        self.captured_images[device_id]["rgb"].append(rgb_image)
        self.captured_images[device_id]["thermal"].append(thermal_image)
        session_folder = Path(self.current_session["session_folder"])
        device_folder = session_folder / device_id
        device_folder.mkdir(exist_ok=True)
        frame_num = self.capture_count[device_id]
        rgb_path = device_folder / f"rgb_{frame_num:03d}.png"
        thermal_path = device_folder / f"thermal_{frame_num:03d}.png"
        cv2.imwrite(str(rgb_path), rgb_image)
        cv2.imwrite(str(thermal_path), thermal_image)
        logger.debug(f" Simulated capture for {device_id}: frame {frame_num}")
        return True
    def can_compute_calibration(self, device_id: str = None) -> Dict[str, bool]:
        if not self.current_session:
            return {}
        devices_to_check = (
            [device_id] if device_id else self.current_session["device_ids"]
        )
        readiness = {}
        for dev_id in devices_to_check:
            frame_count = self.capture_count.get(dev_id, 0)
            readiness[dev_id] = frame_count >= self.min_images
        return readiness
    def compute_calibration(self, device_id: str = None) -> Dict[str, Any]:
        if not self.current_session:
            raise RuntimeError("No active calibration session")
        devices_to_calibrate = (
            [device_id] if device_id else self.current_session["device_ids"]
        )
        computation_results = {
            "success": True,
            "message": "Calibration computed successfully",
            "device_results": {},
        }
        for dev_id in devices_to_calibrate:
            try:
                logger.debug(f" Computing calibration for device {dev_id}")
                if not self.can_compute_calibration(dev_id)[dev_id]:
                    computation_results["device_results"][dev_id] = {
                        "success": False,
                        "message": f"Insufficient images: {self.capture_count[dev_id]}/{self.min_images}",
                    }
                    continue
                rgb_images = self.captured_images[dev_id]["rgb"]
                thermal_images = self.captured_images[dev_id]["thermal"]
                result = self._compute_device_calibration(
                    dev_id, rgb_images, thermal_images
                )
                if result.is_valid():
                    self.calibration_results[dev_id] = result
                    self._save_calibration_result(dev_id, result)
                    computation_results["device_results"][dev_id] = {
                        "success": True,
                        "message": "Calibration computed successfully",
                        "rgb_error": result.rgb_rms_error,
                        "thermal_error": result.thermal_rms_error,
                        "stereo_error": result.stereo_rms_error,
                        "quality_score": result.quality_assessment.get(
                            "quality_score", "UNKNOWN"
                        ),
                    }
                else:
                    computation_results["device_results"][dev_id] = {
                        "success": False,
                        "message": "Calibration computation failed",
                    }
            except Exception as e:
                computation_results["device_results"][dev_id] = {
                    "success": False,
                    "message": f"Calibration error: {str(e)}",
                }
                logger.debug(f" Calibration error for {dev_id}: {e}")
        all_successful = all(
            result.get("success", False)
            for result in computation_results["device_results"].values()
        )
        if not all_successful:
            computation_results["success"] = False
            computation_results["message"] = "Some device calibrations failed"
        return computation_results
    def _detect_calibration_patterns(
        self, rgb_images: List[np.ndarray], thermal_images: List[np.ndarray]
    ) -> Tuple[List, List, List]:
        object_points = self.processor.create_object_points(
            self.chessboard_size, self.square_size
        )
        rgb_image_points = []
        thermal_image_points = []
        valid_object_points = []
        for i, (rgb_img, thermal_img) in enumerate(zip(rgb_images, thermal_images)):
            rgb_success, rgb_corners = self.processor.detect_chessboard_corners(
                rgb_img, self.chessboard_size
            )
            thermal_success, thermal_corners = self.processor.detect_chessboard_corners(
                thermal_img, self.chessboard_size
            )
            if rgb_success and thermal_success:
                rgb_image_points.append(rgb_corners)
                thermal_image_points.append(thermal_corners)
                valid_object_points.append(object_points)
                logger.debug(f" Pattern detected in frame {i} for both cameras")
            else:
                logger.debug(f" Pattern detection failed in frame {i}")
        return valid_object_points, rgb_image_points, thermal_image_points
    def _calibrate_individual_cameras(
        self, valid_object_points: List, rgb_image_points: List,
        thermal_image_points: List, rgb_images: List[np.ndarray],
        thermal_images: List[np.ndarray], result: CalibrationResult
    ) -> Tuple[bool, bool]:
        rgb_image_size = rgb_images[0].shape[1], rgb_images[0].shape[0]
        rgb_ret, rgb_camera_matrix, rgb_dist_coeffs, _, _ = cv2.calibrateCamera(
            valid_object_points, rgb_image_points, rgb_image_size, None, None
        )
        rgb_calibrated = False
        if rgb_ret:
            result.rgb_camera_matrix = rgb_camera_matrix
            result.rgb_distortion_coeffs = rgb_dist_coeffs
            result.rgb_rms_error = rgb_ret
            logger.debug(f" RGB camera calibrated with RMS error: {rgb_ret:.3f}")
            rgb_calibrated = True
        thermal_image_size = thermal_images[0].shape[1], thermal_images[0].shape[0]
        thermal_ret, thermal_camera_matrix, thermal_dist_coeffs, _, _ = (
            cv2.calibrateCamera(
                valid_object_points, thermal_image_points, thermal_image_size, None, None
            )
        )
        thermal_calibrated = False
        if thermal_ret:
            result.thermal_camera_matrix = thermal_camera_matrix
            result.thermal_distortion_coeffs = thermal_dist_coeffs
            result.thermal_rms_error = thermal_ret
            logger.debug(f" Thermal camera calibrated with RMS error: {thermal_ret:.3f}")
            thermal_calibrated = True
        return rgb_calibrated, thermal_calibrated
    def _perform_stereo_calibration(
        self, valid_object_points: List, rgb_image_points: List,
        thermal_image_points: List, result: CalibrationResult, rgb_images: List[np.ndarray]
    ) -> None:
        rgb_image_size = rgb_images[0].shape[1], rgb_images[0].shape[0]
        stereo_ret, _, _, _, _, R, T, E, F = cv2.stereoCalibrate(
            valid_object_points, rgb_image_points, thermal_image_points,
            result.rgb_camera_matrix, result.rgb_distortion_coeffs,
            result.thermal_camera_matrix, result.thermal_distortion_coeffs,
            rgb_image_size, flags=cv2.CALIB_FIX_INTRINSIC,
        )
        if stereo_ret:
            result.rotation_matrix = R
            result.translation_vector = T
            result.essential_matrix = E
            result.fundamental_matrix = F
            result.stereo_rms_error = stereo_ret
            logger.debug(f" Stereo calibration completed with RMS error: {stereo_ret:.3f}")
            result.homography_matrix = self.processor.compute_homography(
                thermal_image_points[0], rgb_image_points[0]
            )
    def _compute_device_calibration(
        self,
        device_id: str,
        rgb_images: List[np.ndarray],
        thermal_images: List[np.ndarray],
    ) -> CalibrationResult:
        logger.debug(f" Computing calibration for device {device_id}")
        result = CalibrationResult(device_id)
        valid_object_points, rgb_image_points, thermal_image_points = (
            self._detect_calibration_patterns(rgb_images, thermal_images)
        )
        if len(valid_object_points) < self.min_images:
            logger.debug(f" Insufficient valid frames: {len(valid_object_points)}/{self.min_images}")
            return result
        rgb_calibrated, thermal_calibrated = self._calibrate_individual_cameras(
            valid_object_points, rgb_image_points, thermal_image_points,
            rgb_images, thermal_images, result
        )
        if rgb_calibrated and thermal_calibrated:
            self._perform_stereo_calibration(
                valid_object_points, rgb_image_points, thermal_image_points, result, rgb_images
            )
        result.quality_assessment = self._assess_calibration_quality(result)
        result.calibration_timestamp = datetime.now().isoformat()
        return result
    def _assess_calibration_quality(self, result: CalibrationResult) -> Dict[str, Any]:
        assessment = {"quality_score": "UNKNOWN", "recommendations": []}
        rgb_error = result.rgb_rms_error or float("inf")
        thermal_error = result.thermal_rms_error or float("inf")
        stereo_error = result.stereo_rms_error or float("inf")
        max_error = max(rgb_error, thermal_error, stereo_error)
        if max_error < 0.5:
            assessment["quality_score"] = "EXCELLENT"
        elif max_error < 1.0:
            assessment["quality_score"] = "GOOD"
        elif max_error < 2.0:
            assessment["quality_score"] = "ACCEPTABLE"
            assessment["recommendations"].append(
                "Consider recapturing some images for better accuracy"
            )
        else:
            assessment["quality_score"] = "POOR"
            assessment["recommendations"].extend(
                [
                    "Recapture calibration images",
                    "Ensure calibration pattern is clearly visible",
                    "Use better lighting conditions",
                ]
            )
        assessment["rgb_error"] = rgb_error
        assessment["thermal_error"] = thermal_error
        assessment["stereo_error"] = stereo_error
        return assessment
    def _save_calibration_result(self, device_id: str, result: CalibrationResult):
        session_folder = Path(self.current_session["session_folder"])
        calibration_file = session_folder / f"calibration_{device_id}.json"
        result.save_to_file(str(calibration_file))
        logger.debug(f" Calibration result saved: {calibration_file}")
    def get_calibration_result(self, device_id: str) -> Optional[CalibrationResult]:
        return self.calibration_results.get(device_id)
    def load_calibration_result(self, device_id: str, calibration_file: str) -> bool:
        try:
            result = CalibrationResult.load_from_file(calibration_file)
            if result:
                self.calibration_results[device_id] = result
                logger.debug(f" Calibration result loaded for {device_id}")
                return True
        except Exception as e:
            logger.debug(f" Failed to load calibration for {device_id}: {e}")
        return False
    def apply_thermal_overlay(
        self,
        device_id: str,
        rgb_image: np.ndarray,
        thermal_image: np.ndarray,
        alpha: float = 0.3,
    ) -> Optional[np.ndarray]:
        result = self.calibration_results.get(device_id)
        if not result or result.homography_matrix is None:
            return None
        try:
            thermal_warped = cv2.warpPerspective(
                thermal_image,
                result.homography_matrix,
                (rgb_image.shape[1], rgb_image.shape[0]),
            )
            thermal_colored = cv2.applyColorMap(thermal_warped, cv2.COLORMAP_JET)
            overlay = cv2.addWeighted(rgb_image, 1.0 - alpha, thermal_colored, alpha, 0)
            return overlay
        except Exception as e:
            logger.debug(f" Overlay error for {device_id}: {e}")
            return None
    def end_calibration_session(self) -> Dict[str, Any]:
        if not self.current_session:
            return {"success": False, "message": "No active session"}
        self.current_session["end_time"] = datetime.now().isoformat()
        self.current_session["status"] = "completed"
        self.current_session["total_captures"] = dict(self.capture_count)
        self.current_session["calibrated_devices"] = list(
            self.calibration_results.keys()
        )
        session_folder = Path(self.current_session["session_folder"])
        session_file = session_folder / "session_info.json"
        with open(session_file, "w") as f:
            json.dump(self.current_session, f, indent=2)
        session_summary = {
            "success": True,
            "session_name": self.current_session["session_name"],
            "total_captures": dict(self.capture_count),
            "calibrated_devices": list(self.calibration_results.keys()),
            "session_folder": str(session_folder),
        }
        self.current_session = None
        self.captured_images.clear()
        self.capture_count.clear()
        self.is_capturing = False
        logger.debug(f" Calibration session ended: {session_summary}")
        return session_summary
    def get_session_status(self) -> Dict[str, Any]:
        if not self.current_session:
            return {"active": False}
        return {
            "active": True,
            "session_name": self.current_session["session_name"],
            "device_ids": self.current_session["device_ids"],
            "capture_counts": dict(self.capture_count),
            "can_compute": self.can_compute_calibration(),
            "calibrated_devices": list(self.calibration_results.keys()),
            "is_capturing": self.is_capturing,
        }
    @property
    def pattern_size(self):
        return self.chessboard_size
    def detect_pattern(self, image, pattern_type="chessboard"):
        if pattern_type == "chessboard":
            success, corners = self.processor.detect_chessboard_corners(
                image, self.chessboard_size
            )
            return success, corners
        elif pattern_type == "circles":
            success, corners = self.processor.detect_circles_grid(
                image, self.chessboard_size
            )
            return success, corners
        else:
            return False, None
    def save_calibration(self, device_id, filename):
        result = self.calibration_results.get(device_id)
        if result:
            import json
            data = {
                "camera_matrix": result.camera_matrix.tolist(),
                "distortion_coefficients": result.distortion_coefficients.tolist(),
                "rms_error": result.rms_error,
                "image_size": result.image_size,
            }
            if result.homography_matrix is not None:
                data["homography_matrix"] = result.homography_matrix.tolist()
            with open(filename, "w") as f:
                json.dump(data, f, indent=2)
            return True
        return False
if __name__ == "__main__":
    logger.debug(" Testing CalibrationManager...")
    manager = CalibrationManager("test_calibration")
    session = manager.start_calibration_session(
        ["device_1", "device_2"], "test_session"
    )
    logger.info(f"Started session: {session['session_name']}")
    for i in range(12):
        results = manager.capture_calibration_frame(None)
        logger.info(f"Capture {i + 1}: {results['total_frames']}")
    calibration_results = manager.compute_calibration()
    logger.info(f"Calibration results: {calibration_results}")
    summary = manager.end_calibration_session()
    logger.info(f"Session ended: {summary}")
    logger.debug(" CalibrationManager test completed")