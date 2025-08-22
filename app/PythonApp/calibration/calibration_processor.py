from typing import List, Optional, Tuple
import cv2
import numpy as np
from ..utils.logging_config import get_logger
class CalibrationProcessor:
    def __init__(self):
        self.logger = get_logger(__name__)
        self.pattern_size = 9, 6
        self.square_size = 25.0
        self.corner_criteria = (
            cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER,
            30,
            0.001,
        )
        self.calibration_flags = (
            cv2.CALIB_RATIONAL_MODEL
            | cv2.CALIB_THIN_PRISM_MODEL
            | cv2.CALIB_TILTED_MODEL
        )
        self.logger.debug("CalibrationProcessor initialized")
    def detect_chessboard_corners(
        self, image: np.ndarray, pattern_size: Tuple[int, int]
    ) -> Tuple[bool, Optional[np.ndarray]]:
        if len(image.shape) == 3:
            grey = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        else:
            grey = image.copy()
        ret, corners = cv2.findChessboardCorners(
            grey,
            pattern_size,
            cv2.CALIB_CB_ADAPTIVE_THRESH + cv2.CALIB_CB_NORMALIZE_IMAGE,
        )
        if ret:
            corners = cv2.cornerSubPix(
                grey, corners, (11, 11), (-1, -1), self.corner_criteria
            )
            return True, corners
        else:
            return False, None
    def detect_circles_grid(
        self, image: np.ndarray, pattern_size: Tuple[int, int]
    ) -> Tuple[bool, Optional[np.ndarray]]:
        if len(image.shape) == 3:
            grey = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        else:
            grey = image.copy()
        ret, centres = cv2.findCirclesGrid(
            grey, pattern_size, cv2.CALIB_CB_SYMMETRIC_GRID
        )
        return ret, centres if ret else None
    def find_calibration_corners(self, image: np.ndarray) -> dict:
        try:
            success, corners = self.detect_chessboard_corners(image, self.pattern_size)
            if success:
                return {"success": True, "corners": corners}
            else:
                return {"success": False, "error": "No chessboard pattern detected"}
        except Exception as e:
            return {"success": False, "error": f"Corner detection failed: {str(e)}"}
    def detect_aruco_markers(
        self, image: np.ndarray, dictionary_id: int = cv2.aruco.DICT_6X6_250
    ) -> Tuple[bool, List[np.ndarray]]:
        if len(image.shape) == 3:
            grey = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        else:
            grey = image.copy()
        aruco_dict = cv2.aruco.Dictionary_get(dictionary_id)
        parameters = cv2.aruco.DetectorParameters_create()
        corners, ids, rejected = cv2.aruco.detectMarkers(
            grey, aruco_dict, parameters=parameters
        )
        if ids is not None and len(corners) > 0:
            return True, corners
        else:
            return False, []
    def create_object_points(
        self, pattern_size: Tuple[int, int], square_size: float
    ) -> np.ndarray:
        objp = np.zeros((pattern_size[0] * pattern_size[1], 3), np.float32)
        objp[:, :2] = np.mgrid[0 : pattern_size[0], 0 : pattern_size[1]].T.reshape(
            -1, 2
        )
        objp *= square_size
        return objp
    def calibrate_camera_intrinsics(
        self,
        object_points: List[np.ndarray],
        image_points: List[np.ndarray],
        image_size: Tuple[int, int],
    ) -> Tuple[float, np.ndarray, np.ndarray, List[np.ndarray], List[np.ndarray]]:
        ret, camera_matrix, dist_coeffs, rvecs, tvecs = cv2.calibrateCamera(
            object_points,
            image_points,
            image_size,
            None,
            None,
            flags=self.calibration_flags,
        )
        return ret, camera_matrix, dist_coeffs, rvecs, tvecs
    def calibrate_stereo_cameras(
        self,
        object_points: List[np.ndarray],
        image_points1: List[np.ndarray],
        image_points2: List[np.ndarray],
        camera_matrix1: np.ndarray,
        dist_coeffs1: np.ndarray,
        camera_matrix2: np.ndarray,
        dist_coeffs2: np.ndarray,
        image_size: Tuple[int, int],
    ) -> Tuple[float, np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
        ret, _, _, _, _, R, T, E, F = cv2.stereoCalibrate(
            object_points,
            image_points1,
            image_points2,
            camera_matrix1,
            dist_coeffs1,
            camera_matrix2,
            dist_coeffs2,
            image_size,
            flags=cv2.CALIB_FIX_INTRINSIC,
        )
        return ret, R, T, E, F
    def compute_homography(
        self, points1: np.ndarray, points2: np.ndarray
    ) -> Optional[np.ndarray]:
        if len(points1) < 4 or len(points2) < 4:
            self.logger.debug("Insufficient points for homography computation")
            return None
        try:
            if points1.shape[1] == 1:
                points1 = points1.reshape(-1, 2)
            if points2.shape[1] == 1:
                points2 = points2.reshape(-1, 2)
            H, mask = cv2.findHomography(points1, points2, cv2.RANSAC, 5.0)
            if H is not None:
                self.logger.debug(f"Homography computed with {np.sum(mask)} inliers")
                return H
            else:
                self.logger.debug("Homography computation failed")
                return None
        except Exception as e:
            self.logger.debug(f"Homography computation error: {e}")
            return None
    def compute_reprojection_error(
        self,
        object_points: List[np.ndarray],
        image_points: List[np.ndarray],
        camera_matrix: np.ndarray,
        dist_coeffs: np.ndarray,
        rvecs: List[np.ndarray],
        tvecs: List[np.ndarray],
    ) -> Tuple[float, List[float]]:
        total_error = 0
        per_image_errors = []
        for i in range(len(object_points)):
            projected_points, _ = cv2.projectPoints(
                object_points[i], rvecs[i], tvecs[i], camera_matrix, dist_coeffs
            )
            error = cv2.norm(image_points[i], projected_points, cv2.NORM_L2) / len(
                projected_points
            )
            per_image_errors.append(error)
            total_error += error
        mean_error = total_error / len(object_points)
        return mean_error, per_image_errors
    def undistort_image(
        self, image: np.ndarray, camera_matrix: np.ndarray, dist_coeffs: np.ndarray
    ) -> np.ndarray:
        h, w = image.shape[:2]
        new_camera_matrix, roi = cv2.getOptimalNewCameraMatrix(
            camera_matrix, dist_coeffs, (w, h), 1, (w, h)
        )
        undistorted = cv2.undistort(
            image, camera_matrix, dist_coeffs, None, new_camera_matrix
        )
        x, y, w, h = roi
        if w > 0 and h > 0:
            undistorted = undistorted[y : y + h, x : x + w]
        return undistorted
    def create_rectification_maps(
        self,
        camera_matrix1: np.ndarray,
        dist_coeffs1: np.ndarray,
        camera_matrix2: np.ndarray,
        dist_coeffs2: np.ndarray,
        image_size: Tuple[int, int],
        R: np.ndarray,
        T: np.ndarray,
    ) -> Tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
        R1, R2, P1, P2, Q, validPixROI1, validPixROI2 = cv2.stereoRectify(
            camera_matrix1, dist_coeffs1, camera_matrix2, dist_coeffs2, image_size, R, T
        )
        map1x, map1y = cv2.initUndistortRectifyMap(
            camera_matrix1, dist_coeffs1, R1, P1, image_size, cv2.CV_16SC2
        )
        map2x, map2y = cv2.initUndistortRectifyMap(
            camera_matrix2, dist_coeffs2, R2, P2, image_size, cv2.CV_16SC2
        )
        return map1x, map1y, map2x, map2y
    def apply_rectification(
        self,
        image1: np.ndarray,
        image2: np.ndarray,
        map1x: np.ndarray,
        map1y: np.ndarray,
        map2x: np.ndarray,
        map2y: np.ndarray,
    ) -> Tuple[np.ndarray, np.ndarray]:
        rectified1 = cv2.remap(image1, map1x, map1y, cv2.INTER_LINEAR)
        rectified2 = cv2.remap(image2, map2x, map2y, cv2.INTER_LINEAR)
        return rectified1, rectified2
    def validate_calibration_quality(
        self, reprojection_error: float, num_images: int, pattern_coverage: float = None
    ) -> dict:
        quality = {
            "reprojection_error": reprojection_error,
            "num_images": num_images,
            "quality_score": "UNKNOWN",
            "recommendations": [],
        }
        if reprojection_error < 0.3:
            error_quality = "EXCELLENT"
        elif reprojection_error < 0.5:
            error_quality = "VERY_GOOD"
        elif reprojection_error < 1.0:
            error_quality = "GOOD"
        elif reprojection_error < 2.0:
            error_quality = "ACCEPTABLE"
        else:
            error_quality = "POOR"
        if num_images < 10:
            quality["recommendations"].append(
                "Use more calibration images (recommended: 15-20)"
            )
        elif num_images < 15:
            quality["recommendations"].append(
                "Consider using more images for better accuracy"
            )
        if error_quality in ["EXCELLENT", "VERY_GOOD"] and num_images >= 10:
            quality["quality_score"] = "EXCELLENT"
        elif error_quality in ["GOOD"] and num_images >= 10:
            quality["quality_score"] = "GOOD"
        elif error_quality in ["ACCEPTABLE"] and num_images >= 8:
            quality["quality_score"] = "ACCEPTABLE"
        else:
            quality["quality_score"] = "POOR"
            quality["recommendations"].extend(
                [
                    "Recapture calibration images",
                    "Ensure good lighting conditions",
                    "Use a high-contrast calibration pattern",
                    "Vary pattern positions and orientations",
                ]
            )
        return quality
    def draw_chessboard_corners(
        self,
        image: np.ndarray,
        pattern_size: Tuple[int, int],
        corners: np.ndarray,
        pattern_found: bool,
    ) -> np.ndarray:
        result_image = image.copy()
        cv2.drawChessboardCorners(result_image, pattern_size, corners, pattern_found)
        return result_image
if __name__ == "__main__":
    logger = get_logger(__name__)
    logger.info("Testing CalibrationProcessor...")
    processor = CalibrationProcessor()
    pattern_size = 9, 6
    square_size = 25.0
    objp = processor.create_object_points(pattern_size, square_size)
    logger.info(f"Created object points: {objp.shape}")
    test_image = np.zeros((480, 640, 3), dtype=np.uint8)
    success, corners = processor.detect_chessboard_corners(test_image, pattern_size)
    logger.info(f"Chessboard detection: {success}")
    logger.info("CalibrationProcessor test completed")