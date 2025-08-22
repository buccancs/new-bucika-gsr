from dataclasses import dataclass
from enum import Enum
from typing import Any, Dict, List, Optional, Tuple
import numpy as np
class SegmentationMethod(Enum):
    MEDIAPIPE = "mediapipe"
    COLOR_BASED = "color_based"
    CONTOUR_BASED = "contour_based"
@dataclass
class HandRegion:
    bbox: Tuple[int, int, int, int]
    mask: Optional[np.ndarray] = None
    landmarks: Optional[List[Tuple[float, float]]] = None
    confidence: float = 0.0
    hand_label: str = "Unknown"
@dataclass
class SegmentationConfig:
    method: SegmentationMethod = SegmentationMethod.MEDIAPIPE
    min_detection_confidence: float = 0.5
    min_tracking_confidence: float = 0.5
    max_num_hands: int = 2
    output_cropped: bool = True
    output_masks: bool = True
    crop_padding: int = 20
    target_resolution: Optional[Tuple[int, int]] = None
    skin_color_lower: Tuple[int, int, int] = (0, 20, 70)
    skin_color_upper: Tuple[int, int, int] = (20, 255, 255)
    contour_min_area: int = 1000
    contour_max_area: int = 50000
@dataclass
class ProcessingResult:
    input_video_path: str
    output_directory: str
    processed_frames: int = 0
    detected_hands_count: int = 0
    processing_time: float = 0.0
    output_files: Dict[str, str] = None
    success: bool = False
    error_message: Optional[str] = None
    def __post_init__(self):
        if self.output_files is None:
            self.output_files = {}
def create_bounding_box_from_landmarks(
    landmarks: List[Tuple[float, float]],
    frame_width: int,
    frame_height: int,
    padding: int = 20,
) -> Tuple[int, int, int, int]:
    if not landmarks:
        return 0, 0, 0, 0
    x_coords = [int(point[0] * frame_width) for point in landmarks]
    y_coords = [int(point[1] * frame_height) for point in landmarks]
    min_x, max_x = min(x_coords), max(x_coords)
    min_y, max_y = min(y_coords), max(y_coords)
    min_x = max(0, min_x - padding)
    min_y = max(0, min_y - padding)
    max_x = min(frame_width, max_x + padding)
    max_y = min(frame_height, max_y + padding)
    width = max_x - min_x
    height = max_y - min_y
    return min_x, min_y, width, height
def crop_frame_to_region(
    frame: np.ndarray, bbox: Tuple[int, int, int, int]
) -> np.ndarray:
    x, y, w, h = bbox
    return frame[y : y + h, x : x + w]
def resize_frame(frame: np.ndarray, target_size: Tuple[int, int]) -> np.ndarray:
    import cv2
    return cv2.resize(frame, target_size, interpolation=cv2.INTER_AREA)
def create_hand_mask_from_landmarks(
    landmarks: List[Tuple[float, float]], frame_shape: Tuple[int, int]
) -> np.ndarray:
    import cv2
    height, width = frame_shape[:2]
    mask = np.zeros((height, width), dtype=np.uint8)
    if not landmarks:
        return mask
    points = []
    for point in landmarks:
        x = int(point[0] * width)
        y = int(point[1] * height)
        points.append([x, y])
    points = np.array(points, dtype=np.int32)
    hull = cv2.convexHull(points)
    cv2.fillPoly(mask, [hull], 255)
    return mask
def save_processing_metadata(result: ProcessingResult, output_path: str):
    import json
    from datetime import datetime
    metadata = {
        "input_video": result.input_video_path,
        "output_directory": result.output_directory,
        "processed_frames": result.processed_frames,
        "detected_hands_count": result.detected_hands_count,
        "processing_time": result.processing_time,
        "output_files": result.output_files,
        "success": result.success,
        "error_message": result.error_message,
        "processed_at": datetime.now().isoformat(),
    }
    with open(output_path, "w") as f:
        json.dump(metadata, f, indent=2)