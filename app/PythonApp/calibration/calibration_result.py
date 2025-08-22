import json
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Optional
import numpy as np
from ..utils.logging_config import get_logger
logger = get_logger(__name__)
class CalibrationResult:
    def __init__(self, device_id: str):
        self.device_id = device_id
        self.calibration_timestamp = None
        self.rgb_camera_matrix = None
        self.rgb_distortion_coeffs = None
        self.rgb_rms_error = None
        self.thermal_camera_matrix = None
        self.thermal_distortion_coeffs = None
        self.thermal_rms_error = None
        self.rotation_matrix = None
        self.translation_vector = None
        self.essential_matrix = None
        self.fundamental_matrix = None
        self.stereo_rms_error = None
        self.homography_matrix = None
        self.quality_assessment = {}
        self.pattern_type = "chessboard"
        self.pattern_size = 9, 6
        self.square_size = 25.0
        self.num_images_used = 0
        logger.debug(f"CalibrationResult initialized for device: {device_id}")
    def is_valid(self) -> bool:
        rgb_valid = (
            self.rgb_camera_matrix is not None
            and self.rgb_distortion_coeffs is not None
            and self.rgb_rms_error is not None
        )
        thermal_valid = (
            self.thermal_camera_matrix is not None
            and self.thermal_distortion_coeffs is not None
            and self.thermal_rms_error is not None
        )
        stereo_valid = (
            self.rotation_matrix is not None
            and self.translation_vector is not None
            and self.stereo_rms_error is not None
        )
        return rgb_valid and thermal_valid and stereo_valid
    def get_calibration_summary(self) -> Dict[str, Any]:
        summary = {
            "device_id": self.device_id,
            "timestamp": self.calibration_timestamp,
            "is_valid": self.is_valid(),
            "pattern_info": {
                "type": self.pattern_type,
                "size": self.pattern_size,
                "square_size": self.square_size,
                "num_images": self.num_images_used,
            },
            "rgb_camera": {
                "calibrated": self.rgb_camera_matrix is not None,
                "rms_error": self.rgb_rms_error,
            },
            "thermal_camera": {
                "calibrated": self.thermal_camera_matrix is not None,
                "rms_error": self.thermal_rms_error,
            },
            "stereo_calibration": {
                "available": self.rotation_matrix is not None,
                "rms_error": self.stereo_rms_error,
            },
            "overlay_ready": self.homography_matrix is not None,
            "quality_assessment": self.quality_assessment,
        }
        return summary
    def to_dict(self) -> Dict[str, Any]:
        def numpy_to_list(arr):
            return arr.tolist() if arr is not None else None
        data = {
            "device_id": self.device_id,
            "calibration_timestamp": self.calibration_timestamp,
            "pattern_type": self.pattern_type,
            "pattern_size": self.pattern_size,
            "square_size": self.square_size,
            "num_images_used": self.num_images_used,
            "rgb_camera_matrix": numpy_to_list(self.rgb_camera_matrix),
            "rgb_distortion_coeffs": numpy_to_list(self.rgb_distortion_coeffs),
            "rgb_rms_error": self.rgb_rms_error,
            "thermal_camera_matrix": numpy_to_list(self.thermal_camera_matrix),
            "thermal_distortion_coeffs": numpy_to_list(self.thermal_distortion_coeffs),
            "thermal_rms_error": self.thermal_rms_error,
            "rotation_matrix": numpy_to_list(self.rotation_matrix),
            "translation_vector": numpy_to_list(self.translation_vector),
            "essential_matrix": numpy_to_list(self.essential_matrix),
            "fundamental_matrix": numpy_to_list(self.fundamental_matrix),
            "stereo_rms_error": self.stereo_rms_error,
            "homography_matrix": numpy_to_list(self.homography_matrix),
            "quality_assessment": self.quality_assessment,
            "format_version": "1.0",
            "created_by": "Multi-Sensor Recording System v3.4",
        }
        return data
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "CalibrationResult":
        def list_to_numpy(lst):
            return np.array(lst) if lst is not None else None
        result = cls(data["device_id"])
        result.calibration_timestamp = data.get("calibration_timestamp")
        result.pattern_type = data.get("pattern_type", "chessboard")
        result.pattern_size = tuple(data.get("pattern_size", (9, 6)))
        result.square_size = data.get("square_size", 25.0)
        result.num_images_used = data.get("num_images_used", 0)
        result.rgb_camera_matrix = list_to_numpy(data.get("rgb_camera_matrix"))
        result.rgb_distortion_coeffs = list_to_numpy(data.get("rgb_distortion_coeffs"))
        result.rgb_rms_error = data.get("rgb_rms_error")
        result.thermal_camera_matrix = list_to_numpy(data.get("thermal_camera_matrix"))
        result.thermal_distortion_coeffs = list_to_numpy(
            data.get("thermal_distortion_coeffs")
        )
        result.thermal_rms_error = data.get("thermal_rms_error")
        result.rotation_matrix = list_to_numpy(data.get("rotation_matrix"))
        result.translation_vector = list_to_numpy(data.get("translation_vector"))
        result.essential_matrix = list_to_numpy(data.get("essential_matrix"))
        result.fundamental_matrix = list_to_numpy(data.get("fundamental_matrix"))
        result.stereo_rms_error = data.get("stereo_rms_error")
        result.homography_matrix = list_to_numpy(data.get("homography_matrix"))
        result.quality_assessment = data.get("quality_assessment", {})
        return result
    def save_to_file(self, filepath: str) -> bool:
        try:
            Path(filepath).parent.mkdir(parents=True, exist_ok=True)
            data = self.to_dict()
            with open(filepath, "w") as f:
                json.dump(data, f, indent=2)
            logger.debug(f"Calibration result saved to: {filepath}")
            return True
        except Exception as e:
            logger.error(f"Failed to save calibration result: {e}")
            return False
    @classmethod
    def load_from_file(cls, filepath: str) -> Optional["CalibrationResult"]:
        try:
            with open(filepath, "r") as f:
                data = json.load(f)
            result = cls.from_dict(data)
            logger.debug(f"Calibration result loaded from: {filepath}")
            return result
        except Exception as e:
            logger.error(f"Failed to load calibration result: {e}")
            return None
    def get_rgb_camera_info(self) -> Dict[str, Any]:
        if self.rgb_camera_matrix is None:
            return {"calibrated": False}
        fx = self.rgb_camera_matrix[0, 0]
        fy = self.rgb_camera_matrix[1, 1]
        cx = self.rgb_camera_matrix[0, 2]
        cy = self.rgb_camera_matrix[1, 2]
        return {
            "calibrated": True,
            "focal_length": {"fx": fx, "fy": fy},
            "principal_point": {"cx": cx, "cy": cy},
            "rms_error": self.rgb_rms_error,
            "distortion_coeffs": (
                self.rgb_distortion_coeffs.tolist()
                if self.rgb_distortion_coeffs is not None
                else None
            ),
        }
    def get_thermal_camera_info(self) -> Dict[str, Any]:
        if self.thermal_camera_matrix is None:
            return {"calibrated": False}
        fx = self.thermal_camera_matrix[0, 0]
        fy = self.thermal_camera_matrix[1, 1]
        cx = self.thermal_camera_matrix[0, 2]
        cy = self.thermal_camera_matrix[1, 2]
        return {
            "calibrated": True,
            "focal_length": {"fx": fx, "fy": fy},
            "principal_point": {"cx": cx, "cy": cy},
            "rms_error": self.thermal_rms_error,
            "distortion_coeffs": (
                self.thermal_distortion_coeffs.tolist()
                if self.thermal_distortion_coeffs is not None
                else None
            ),
        }
    def get_stereo_info(self) -> Dict[str, Any]:
        if self.rotation_matrix is None or self.translation_vector is None:
            return {"calibrated": False}
        baseline = np.linalg.norm(self.translation_vector)
        try:
            import cv2
            rvec, _ = cv2.Rodrigues(self.rotation_matrix)
            euler_angles = np.degrees(rvec.flatten())
        except (cv2.error, ValueError, TypeError) as e:
            logger.debug(f"Failed to compute euler angles: {e}")
            euler_angles = [0, 0, 0]
        return {
            "calibrated": True,
            "baseline_mm": baseline,
            "rotation_angles_deg": {
                "rx": euler_angles[0],
                "ry": euler_angles[1],
                "rz": euler_angles[2],
            },
            "translation_mm": {
                "tx": self.translation_vector[0],
                "ty": self.translation_vector[1],
                "tz": self.translation_vector[2],
            },
            "rms_error": self.stereo_rms_error,
        }
    def validate_integrity(self) -> Dict[str, Any]:
        validation = {"valid": True, "issues": [], "warnings": []}
        if self.rgb_camera_matrix is not None:
            if self.rgb_camera_matrix.shape != (3, 3):
                validation["valid"] = False
                validation["issues"].append("RGB camera matrix has incorrect shape")
            fx, fy = self.rgb_camera_matrix[0, 0], self.rgb_camera_matrix[1, 1]
            if fx <= 0 or fy <= 0:
                validation["valid"] = False
                validation["issues"].append("RGB camera has invalid focal lengths")
            elif fx < 100 or fy < 100:
                validation["warnings"].append(
                    "RGB camera focal lengths seem unusually low"
                )
        if self.thermal_camera_matrix is not None:
            if self.thermal_camera_matrix.shape != (3, 3):
                validation["valid"] = False
                validation["issues"].append("Thermal camera matrix has incorrect shape")
            fx, fy = self.thermal_camera_matrix[0, 0], self.thermal_camera_matrix[1, 1]
            if fx <= 0 or fy <= 0:
                validation["valid"] = False
                validation["issues"].append("Thermal camera has invalid focal lengths")
        if self.rgb_rms_error is not None and self.rgb_rms_error > 5.0:
            validation["warnings"].append("RGB camera RMS error is high (>5.0 pixels)")
        if self.thermal_rms_error is not None and self.thermal_rms_error > 5.0:
            validation["warnings"].append(
                "Thermal camera RMS error is high (>5.0 pixels)"
            )
        if self.stereo_rms_error is not None and self.stereo_rms_error > 5.0:
            validation["warnings"].append(
                "Stereo calibration RMS error is high (>5.0 pixels)"
            )
        if self.rotation_matrix is not None:
            if self.rotation_matrix.shape != (3, 3):
                validation["valid"] = False
                validation["issues"].append("Rotation matrix has incorrect shape")
            else:
                det = np.linalg.det(self.rotation_matrix)
                if abs(det - 1.0) > 0.1:
                    validation["warnings"].append(
                        "Rotation matrix determinant is not close to 1"
                    )
        if self.translation_vector is not None:
            if self.translation_vector.shape != (
                3,
                1,
            ) and self.translation_vector.shape != (3,):
                validation["valid"] = False
                validation["issues"].append("Translation vector has incorrect shape")
        return validation
    def __str__(self) -> str:
        summary = self.get_calibration_summary()
        return f"CalibrationResult(device={self.device_id}, valid={summary['is_valid']}, rgb_error={self.rgb_rms_error:.3f if self.rgb_rms_error else 'N/A'}, thermal_error={self.thermal_rms_error:.3f if self.thermal_rms_error else 'N/A'}, stereo_error={self.stereo_rms_error:.3f if self.stereo_rms_error else 'N/A'})"
    def __repr__(self) -> str:
        return self.__str__()
if __name__ == "__main__":
    logger.info("Testing CalibrationResult...")
    result = CalibrationResult("test_device")
    result.rgb_camera_matrix = np.array(
        [[800, 0, 320], [0, 800, 240], [0, 0, 1]], dtype=np.float32
    )
    result.rgb_distortion_coeffs = np.array(
        [0.1, -0.2, 0.001, 0.002, 0.1], dtype=np.float32
    )
    result.rgb_rms_error = 0.5
    result.thermal_camera_matrix = np.array(
        [[400, 0, 160], [0, 400, 120], [0, 0, 1]], dtype=np.float32
    )
    result.thermal_distortion_coeffs = np.array(
        [0.2, -0.3, 0.002, 0.003, 0.2], dtype=np.float32
    )
    result.thermal_rms_error = 0.8
    result.rotation_matrix = np.eye(3, dtype=np.float32)
    result.translation_vector = np.array([50, 0, 0], dtype=np.float32)
    result.stereo_rms_error = 1.2
    result.calibration_timestamp = datetime.now().isoformat()
    result.quality_assessment = {"quality_score": "GOOD"}
    logger.info(f"Is valid: {result.is_valid()}")
    logger.info(f"Summary: {result.get_calibration_summary()}")
    test_file = "test_calibration.json"
    if result.save_to_file(test_file):
        loaded_result = CalibrationResult.load_from_file(test_file)
        if loaded_result:
            logger.info(f"Loaded result: {loaded_result}")
            logger.info(f"Validation: {loaded_result.validate_integrity()}")
    logger.info("CalibrationResult test completed")