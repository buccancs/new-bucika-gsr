import json
import os
import time
from typing import Dict, List, Optional, Tuple, Union
import cv2
import numpy as np
from ..utils.logging_config import get_logger
class CalibrationManager:
    def __init__(self):
        self.logger = get_logger(__name__)
        self.rgb_camera_matrix = None
        self.rgb_distortion_coeffs = None
        self.thermal_camera_matrix = None
        self.thermal_distortion_coeffs = None
        self.rotation_matrix = None
        self.translation_vector = None
        self.calibration_quality = None
        self.chessboard_size = 9, 6
        self.square_size = 25.0
        self.calibration_flags = cv2.CALIB_RATIONAL_MODEL
        self.criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.001)
    def capture_calibration_images(
        self, device_client=None, num_images: int = 20
    ) -> bool:
        self.logger.debug(f" Capturing {num_images} calibration image pairs")
        if device_client is None:
            self.logger.info(
                "[INFO] No device client provided - this method requires device integration"
            )
            self.logger.info(
                "[INFO] Use load_calibration_images_from_directory() for offline calibration"
            )
            return False
        calibration_images = []
        try:
            for i in range(num_images):
                self.logger.info(f" Capturing calibration image pair {i + 1}/{num_images}")
                command_data = {
                    "image_id": i,
                    "pattern_type": "chessboard",
                    "pattern_size": self.chessboard_size,
                }
                self.logger.info(f" Would send CAPTURE_CALIBRATION command: {command_data}")
                progress = (i + 1) / num_images * 100
                self.logger.info(f" Calibration capture progress: {progress:.1f}%")
            self.logger.info(f" Calibration capture framework ready for device integration")
            return True
        except Exception as e:
            self.logger.error(f" Error during calibration capture: {e}")
            return False
    def detect_calibration_pattern(
        self, image: np.ndarray, pattern_type: str = "chessboard"
    ) -> Tuple[bool, Optional[np.ndarray]]:
        self.logger.debug(f" Detecting {pattern_type} pattern in image")
        if pattern_type == "chessboard":
            grey = (
                cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
                if len(image.shape) == 3
                else image
            )
            ret, corners = cv2.findChessboardCorners(grey, self.chessboard_size, None)
            if ret:
                corners = cv2.cornerSubPix(
                    grey, corners, (11, 11), (-1, -1), self.criteria
                )
                return True, corners
            else:
                return False, None
        elif pattern_type == "circles":
            grey = (
                cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
                if len(image.shape) == 3
                else image
            )
            ret, centres = cv2.findCirclesGrid(grey, self.chessboard_size, None)
            if ret:
                return True, centres
            else:
                return False, None
        else:
            self.logger.error(f" Unsupported pattern type: {pattern_type}")
            return False, None
    def calibrate_single_camera(
        self,
        images: List[np.ndarray],
        image_points: List[np.ndarray],
        object_points: List[np.ndarray],
    ) -> Tuple[Optional[np.ndarray], Optional[np.ndarray], float]:
        self.logger.debug(" Performing single camera calibration")
        if not images or not image_points or not object_points:
            self.logger.error(" Empty input data for calibration")
            return None, None, float("inf")
        if len(images) != len(image_points) or len(images) != len(object_points):
            self.logger.error(" Mismatched input data lengths")
            return None, None, float("inf")
        try:
            image_size = images[0].shape[:2][::-1]
            ret, camera_matrix, dist_coeffs, rvecs, tvecs = cv2.calibrateCamera(
                object_points,
                image_points,
                image_size,
                None,
                None,
                flags=self.calibration_flags,
            )
            if ret:
                total_error = 0
                for i in range(len(object_points)):
                    projected_points, _ = cv2.projectPoints(
                        object_points[i], rvecs[i], tvecs[i], camera_matrix, dist_coeffs
                    )
                    error = cv2.norm(
                        image_points[i], projected_points, cv2.NORM_L2
                    ) / len(projected_points)
                    total_error += error
                mean_error = total_error / len(object_points)
                self.logger.info(
                    f"[DEBUG_LOG] Calibration completed with RMS error: {mean_error:.3f}"
                )
                return camera_matrix, dist_coeffs, mean_error
            else:
                self.logger.error(" Camera calibration failed")
                return None, None, float("inf")
        except Exception as e:
            self.logger.error(f" Exception during calibration: {e}")
            return None, None, float("inf")
    def calibrate_stereo_cameras(
        self,
        rgb_images: List[np.ndarray],
        thermal_images: List[np.ndarray],
        rgb_points: List[np.ndarray],
        thermal_points: List[np.ndarray],
        object_points: List[np.ndarray],
    ) -> Tuple[Optional[np.ndarray], Optional[np.ndarray], float]:
        self.logger.debug(" Performing stereo calibration")
        if self.rgb_camera_matrix is None or self.thermal_camera_matrix is None:
            self.logger.error(" Individual camera calibrations must be completed first")
            return None, None, float("inf")
        if not rgb_images or not thermal_images:
            self.logger.error(" Empty image data for stereo calibration")
            return None, None, float("inf")
        try:
            image_size = rgb_images[0].shape[:2][::-1]
            ret, _, _, _, _, R, T, E, F = cv2.stereoCalibrate(
                object_points,
                rgb_points,
                thermal_points,
                self.rgb_camera_matrix,
                self.rgb_distortion_coeffs,
                self.thermal_camera_matrix,
                self.thermal_distortion_coeffs,
                image_size,
                flags=cv2.CALIB_FIX_INTRINSIC,
            )
            if ret:
                self.rotation_matrix = R
                self.translation_vector = T
                self.logger.info(
                    f"[DEBUG_LOG] Stereo calibration completed with RMS error: {ret:.3f}"
                )
                return R, T, ret
            else:
                self.logger.error(" Stereo calibration failed")
                return None, None, float("inf")
        except Exception as e:
            self.logger.error(f" Exception during stereo calibration: {e}")
            return None, None, float("inf")
    def assess_calibration_quality(
        self,
        images: List[np.ndarray],
        image_points: List[np.ndarray],
        object_points: List[np.ndarray],
        camera_matrix: np.ndarray,
        dist_coeffs: np.ndarray,
        rvecs: List[np.ndarray],
        tvecs: List[np.ndarray],
    ) -> Dict:
        self.logger.debug(" Assessing calibration quality")
        quality_metrics = {
            "mean_reprojection_error": 0.0,
            "max_reprojection_error": 0.0,
            "std_reprojection_error": 0.0,
            "pattern_coverage": 0.0,
            "quality_score": "UNKNOWN",
            "recommendations": [],
        }
        try:
            errors = []
            for i in range(len(object_points)):
                projected_points, _ = cv2.projectPoints(
                    object_points[i], rvecs[i], tvecs[i], camera_matrix, dist_coeffs
                )
                error = cv2.norm(image_points[i], projected_points, cv2.NORM_L2) / len(
                    projected_points
                )
                errors.append(error)
            errors = np.array(errors)
            quality_metrics["mean_reprojection_error"] = float(np.mean(errors))
            quality_metrics["max_reprojection_error"] = float(np.max(errors))
            quality_metrics["std_reprojection_error"] = float(np.std(errors))
            if images and len(images) > 0:
                image_height, image_width = images[0].shape[:2]
                total_area = image_width * image_height
                all_points = np.vstack(image_points)
                min_x, min_y = np.min(all_points.reshape(-1, 2), axis=0)
                max_x, max_y = np.max(all_points.reshape(-1, 2), axis=0)
                coverage_area = (max_x - min_x) * (max_y - min_y)
                quality_metrics["pattern_coverage"] = min(
                    100.0, coverage_area / total_area * 100
                )
            mean_error = quality_metrics["mean_reprojection_error"]
            if mean_error < 0.5:
                quality_metrics["quality_score"] = "EXCELLENT"
            elif mean_error < 1.0:
                quality_metrics["quality_score"] = "GOOD"
            elif mean_error < 2.0:
                quality_metrics["quality_score"] = "ACCEPTABLE"
                quality_metrics["recommendations"].append(
                    "Consider recapturing some images for better accuracy"
                )
            else:
                quality_metrics["quality_score"] = "POOR"
                quality_metrics["recommendations"].append(
                    "Recapture calibration images"
                )
                quality_metrics["recommendations"].append(
                    "Ensure better lighting and pattern visibility"
                )
            if quality_metrics["pattern_coverage"] < 60:
                quality_metrics["recommendations"].append(
                    "Improve pattern coverage across image area"
                )
            if (
                quality_metrics["std_reprojection_error"]
                > quality_metrics["mean_reprojection_error"]
            ):
                quality_metrics["recommendations"].append(
                    "High error variation - some images may be poor quality"
                )
            self.logger.info(
                f"[DEBUG_LOG] Quality assessment complete: {quality_metrics['quality_score']} (error: {mean_error:.3f}px, coverage: {quality_metrics['pattern_coverage']:.1f}%)"
            )
            return quality_metrics
        except Exception as e:
            self.logger.error(f" Exception during quality assessment: {e}")
            return quality_metrics
    def save_calibration_data(self, filename: str) -> bool:
        self.logger.debug(f" Saving calibration data to {filename}")
        calibration_data = {
            "rgb_camera_matrix": (
                self.rgb_camera_matrix.tolist()
                if self.rgb_camera_matrix is not None
                else None
            ),
            "rgb_distortion_coeffs": (
                self.rgb_distortion_coeffs.tolist()
                if self.rgb_distortion_coeffs is not None
                else None
            ),
            "thermal_camera_matrix": (
                self.thermal_camera_matrix.tolist()
                if self.thermal_camera_matrix is not None
                else None
            ),
            "thermal_distortion_coeffs": (
                self.thermal_distortion_coeffs.tolist()
                if self.thermal_distortion_coeffs is not None
                else None
            ),
            "rotation_matrix": (
                self.rotation_matrix.tolist()
                if self.rotation_matrix is not None
                else None
            ),
            "translation_vector": (
                self.translation_vector.tolist()
                if self.translation_vector is not None
                else None
            ),
            "calibration_quality": self.calibration_quality,
            "timestamp": time.time(),
            "calibration_parameters": {
                "chessboard_size": self.chessboard_size,
                "square_size": self.square_size,
                "calibration_flags": int(self.calibration_flags),
            },
        }
        try:
            os.makedirs(os.path.dirname(filename), exist_ok=True)
            with open(filename, "w") as f:
                json.dump(calibration_data, f, indent=2)
            self.logger.debug(f" Calibration data saved successfully")
            return True
        except Exception as e:
            self.logger.error(f" Error saving calibration data: {e}")
            return False
    def load_calibration_data(self, filename: str) -> bool:
        self.logger.debug(f" Loading calibration data from {filename}")
        try:
            if not os.path.exists(filename):
                self.logger.error(f" Calibration file does not exist: {filename}")
                return False
            with open(filename, "r") as f:
                calibration_data = json.load(f)
            if calibration_data.get("rgb_camera_matrix"):
                self.rgb_camera_matrix = np.array(calibration_data["rgb_camera_matrix"])
            if calibration_data.get("rgb_distortion_coeffs"):
                self.rgb_distortion_coeffs = np.array(
                    calibration_data["rgb_distortion_coeffs"]
                )
            if calibration_data.get("thermal_camera_matrix"):
                self.thermal_camera_matrix = np.array(
                    calibration_data["thermal_camera_matrix"]
                )
            if calibration_data.get("thermal_distortion_coeffs"):
                self.thermal_distortion_coeffs = np.array(
                    calibration_data["thermal_distortion_coeffs"]
                )
            if calibration_data.get("rotation_matrix"):
                self.rotation_matrix = np.array(calibration_data["rotation_matrix"])
            if calibration_data.get("translation_vector"):
                self.translation_vector = np.array(
                    calibration_data["translation_vector"]
                )
            self.calibration_quality = calibration_data.get("calibration_quality")
            if "calibration_parameters" in calibration_data:
                params = calibration_data["calibration_parameters"]
                self.chessboard_size = tuple(
                    params.get("chessboard_size", self.chessboard_size)
                )
                self.square_size = params.get("square_size", self.square_size)
                self.calibration_flags = params.get(
                    "calibration_flags", self.calibration_flags
                )
            self.logger.debug(f" Calibration data loaded successfully")
            return True
        except Exception as e:
            self.logger.error(f" Error loading calibration data: {e}")
            return False
    def load_calibration_images_from_directory(
        self,
        directory_path: str,
        rgb_pattern: str = "*rgb*.jpg",
        thermal_pattern: str = "*thermal*.jpg",
    ) -> Tuple[List[np.ndarray], List[np.ndarray]]:
        import glob
        rgb_images = []
        thermal_images = []
        try:
            rgb_files = sorted(glob.glob(os.path.join(directory_path, rgb_pattern)))
            thermal_files = sorted(
                glob.glob(os.path.join(directory_path, thermal_pattern))
            )
            self.logger.info(
                f"[DEBUG_LOG] Found {len(rgb_files)} RGB images and {len(thermal_files)} thermal images"
            )
            for rgb_file in rgb_files:
                img = cv2.imread(rgb_file)
                if img is not None:
                    rgb_images.append(img)
                else:
                    self.logger.warning(f" Could not load RGB image: {rgb_file}")
            for thermal_file in thermal_files:
                img = cv2.imread(thermal_file)
                if img is not None:
                    thermal_images.append(img)
                else:
                    self.logger.warning(f" Could not load thermal image: {thermal_file}")
            return rgb_images, thermal_images
        except Exception as e:
            self.logger.error(f" Error loading calibration images: {e}")
            return [], []
    def perform_complete_calibration(
        self,
        rgb_images: List[np.ndarray],
        thermal_images: Optional[List[np.ndarray]] = None,
        pattern_type: str = "chessboard",
    ) -> Dict:
        self.logger.debug(" Starting complete calibration workflow")
        results = {
            "success": False,
            "rgb_calibration": None,
            "thermal_calibration": None,
            "stereo_calibration": None,
            "quality_metrics": {},
        }
        try:
            if not validate_calibration_images(rgb_images):
                return results
            object_points_3d = create_calibration_pattern_points(
                self.chessboard_size, self.square_size
            )
            rgb_image_points = []
            rgb_object_points = []
            valid_rgb_images = []
            for i, img in enumerate(rgb_images):
                success, corners = self.detect_calibration_pattern(img, pattern_type)
                if success:
                    rgb_image_points.append(corners)
                    rgb_object_points.append(object_points_3d)
                    valid_rgb_images.append(img)
                    self.logger.debug(f" RGB image {i + 1}: Pattern detected")
                else:
                    self.logger.warning(f" RGB image {i + 1}: Pattern not detected")
            if len(rgb_image_points) < 10:
                self.logger.error(f" Insufficient valid RGB images: {len(rgb_image_points)}")
                return results
            rgb_matrix, rgb_dist, rgb_error = self.calibrate_single_camera(
                valid_rgb_images, rgb_image_points, rgb_object_points
            )
            if rgb_matrix is not None:
                self.rgb_camera_matrix = rgb_matrix
                self.rgb_distortion_coeffs = rgb_dist
                results["rgb_calibration"] = {
                    "camera_matrix": rgb_matrix,
                    "distortion_coeffs": rgb_dist,
                    "rms_error": rgb_error,
                }
                self.logger.info(
                    f"[DEBUG_LOG] RGB camera calibration successful (RMS: {rgb_error:.3f})"
                )
                if thermal_images:
                    if not validate_calibration_images(thermal_images):
                        self.logger.info(
                            "[WARNING] Thermal image validation failed, skipping thermal calibration"
                        )
                    else:
                        thermal_image_points = []
                        thermal_object_points = []
                        valid_thermal_images = []
                        for i, img in enumerate(thermal_images):
                            success, corners = self.detect_calibration_pattern(
                                img, pattern_type
                            )
                            if success:
                                thermal_image_points.append(corners)
                                thermal_object_points.append(object_points_3d)
                                valid_thermal_images.append(img)
                                self.logger.info(
                                    f"[DEBUG_LOG] Thermal image {i + 1}: Pattern detected"
                                )
                            else:
                                self.logger.info(
                                    f"[WARNING] Thermal image {i + 1}: Pattern not detected"
                                )
                        if len(thermal_image_points) >= 10:
                            thermal_matrix, thermal_dist, thermal_error = (
                                self.calibrate_single_camera(
                                    valid_thermal_images,
                                    thermal_image_points,
                                    thermal_object_points,
                                )
                            )
                            if thermal_matrix is not None:
                                self.thermal_camera_matrix = thermal_matrix
                                self.thermal_distortion_coeffs = thermal_dist
                                results["thermal_calibration"] = {
                                    "camera_matrix": thermal_matrix,
                                    "distortion_coeffs": thermal_dist,
                                    "rms_error": thermal_error,
                                }
                                self.logger.info(
                                    f"[DEBUG_LOG] Thermal camera calibration successful (RMS: {thermal_error:.3f})"
                                )
                                min_pairs = min(
                                    len(rgb_image_points), len(thermal_image_points)
                                )
                                if min_pairs >= 10:
                                    R, T, stereo_error = self.calibrate_stereo_cameras(
                                        valid_rgb_images[:min_pairs],
                                        valid_thermal_images[:min_pairs],
                                        rgb_image_points[:min_pairs],
                                        thermal_image_points[:min_pairs],
                                        rgb_object_points[:min_pairs],
                                    )
                                    if R is not None and T is not None:
                                        results["stereo_calibration"] = {
                                            "rotation_matrix": R,
                                            "translation_vector": T,
                                            "rms_error": stereo_error,
                                        }
                                        self.logger.info(
                                            f"[DEBUG_LOG] Stereo calibration successful (RMS: {stereo_error:.3f})"
                                        )
                results["success"] = True
                self.logger.debug(" Complete calibration workflow finished successfully")
            return results
        except Exception as e:
            self.logger.error(f" Exception during calibration workflow: {e}")
            return results
    @property
    def pattern_size(self):
        return self.chessboard_size
    def detect_pattern(self, image, pattern_type="chessboard"):
        return self.detect_calibration_pattern(image, pattern_type)
    def save_calibration(self, device_id, filename):
        return self.save_calibration_data(filename)
def create_calibration_pattern_points(
    pattern_size: Tuple[int, int], square_size: float
) -> np.ndarray:
    self.logger.info(
        f"[DEBUG_LOG] Creating calibration pattern points {pattern_size} with square size {square_size}mm"
    )
    pattern_points = np.zeros((pattern_size[0] * pattern_size[1], 3), np.float32)
    pattern_points[:, :2] = np.mgrid[
        0 : pattern_size[0], 0 : pattern_size[1]
    ].T.reshape(-1, 2)
    pattern_points *= square_size
    return pattern_points
def validate_calibration_images(images: List[np.ndarray], min_images: int = 10) -> bool:
    if len(images) < min_images:
        self.logger.error(f" Insufficient calibration images: {len(images)} < {min_images}")
        return False
    if not images:
        return False
    first_shape = images[0].shape
    for i, img in enumerate(images[1:], 1):
        if img.shape != first_shape:
            self.logger.info(
                f"[ERROR] Image shape mismatch at index {i}: {img.shape} != {first_shape}"
            )
            return False
    self.logger.debug(f" Calibration image validation passed: {len(images)} images")
    return True
def draw_calibration_pattern(
    image: np.ndarray,
    corners: np.ndarray,
    pattern_size: Tuple[int, int],
    pattern_found: bool = True,
) -> np.ndarray:
    result_image = image.copy()
    if pattern_found and corners is not None:
        cv2.drawChessboardCorners(result_image, pattern_size, corners, pattern_found)
    return result_image