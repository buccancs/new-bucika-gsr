from abc import ABC, abstractmethod
from typing import List, Optional, Tuple
import cv2
import numpy as np
from .utils import (
    HandRegion,
    SegmentationConfig,
    create_bounding_box_from_landmarks,
    create_hand_mask_from_landmarks,
)
class BaseHandSegmentation(ABC):
    def __init__(self, config: SegmentationConfig):
        self.config = config
        self.is_initialized = False
    @abstractmethod
    def initialize(self) -> bool:
        pass
    @abstractmethod
    def process_frame(self, frame: np.ndarray) -> List[HandRegion]:
        pass
    @abstractmethod
    def cleanup(self):
        pass
class MediaPipeHandSegmentation(BaseHandSegmentation):
    def __init__(self, config: SegmentationConfig):
        super().__init__(config)
        self.hands = None
        self.mp_hands = None
        self.mp_draw = None
    def initialize(self) -> bool:
        try:
            import mediapipe as mp
            self.mp_hands = mp.solutions.hands
            self.mp_draw = mp.solutions.drawing_utils
            self.hands = self.mp_hands.Hands(
                static_image_mode=False,
                max_num_hands=self.config.max_num_hands,
                min_detection_confidence=self.config.min_detection_confidence,
                min_tracking_confidence=self.config.min_tracking_confidence,
            )
            self.is_initialized = True
            return True
        except Exception as e:
            print(f"[ERROR] Failed to initialize MediaPipe hands: {e}")
            return False
    def process_frame(self, frame: np.ndarray) -> List[HandRegion]:
        if not self.is_initialized:
            return []
        hand_regions = []
        height, width = frame.shape[:2]
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = self.hands.process(rgb_frame)
        if results.multi_hand_landmarks:
            for idx, hand_landmarks in enumerate(results.multi_hand_landmarks):
                landmarks = []
                for landmark in hand_landmarks.landmark:
                    landmarks.append((landmark.x, landmark.y))
                bbox = create_bounding_box_from_landmarks(
                    landmarks, width, height, self.config.crop_padding
                )
                mask = None
                if self.config.output_masks:
                    mask = create_hand_mask_from_landmarks(landmarks, frame.shape)
                hand_label = "Unknown"
                if results.multi_handedness:
                    if idx < len(results.multi_handedness):
                        hand_label = (
                            results.multi_handedness[idx].classification[0].label
                        )
                hand_region = HandRegion(
                    bbox=bbox,
                    mask=mask,
                    landmarks=landmarks,
                    confidence=(
                        results.multi_handedness[idx].classification[0].score
                        if results.multi_handedness
                        and idx < len(results.multi_handedness)
                        else 1.0
                    ),
                    hand_label=hand_label,
                )
                hand_regions.append(hand_region)
        return hand_regions
    def cleanup(self):
        if self.hands:
            self.hands.close()
            self.hands = None
        self.is_initialized = False
class ColorBasedHandSegmentation(BaseHandSegmentation):
    def __init__(self, config: SegmentationConfig):
        super().__init__(config)
    def initialize(self) -> bool:
        self.is_initialized = True
        return True
    def process_frame(self, frame: np.ndarray) -> List[HandRegion]:
        if not self.is_initialized:
            return []
        hand_regions = []
        height, width = frame.shape[:2]
        hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
        lower = np.array(self.config.skin_color_lower)
        upper = np.array(self.config.skin_color_upper)
        skin_mask = cv2.inRange(hsv, lower, upper)
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
        skin_mask = cv2.morphologyEx(skin_mask, cv2.MORPH_OPEN, kernel)
        skin_mask = cv2.morphologyEx(skin_mask, cv2.MORPH_CLOSE, kernel)
        contours, _ = cv2.findContours(
            skin_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE
        )
        for contour in contours:
            area = cv2.contourArea(contour)
            if self.config.contour_min_area <= area <= self.config.contour_max_area:
                x, y, w, h = cv2.boundingRect(contour)
                x = max(0, x - self.config.crop_padding)
                y = max(0, y - self.config.crop_padding)
                w = min(width - x, w + 2 * self.config.crop_padding)
                h = min(height - y, h + 2 * self.config.crop_padding)
                bbox = x, y, w, h
                mask = None
                if self.config.output_masks:
                    mask = np.zeros((height, width), dtype=np.uint8)
                    cv2.fillPoly(mask, [contour], 255)
                confidence = min(1.0, area / self.config.contour_max_area)
                hand_region = HandRegion(
                    bbox=bbox,
                    mask=mask,
                    landmarks=None,
                    confidence=confidence,
                    hand_label="Unknown",
                )
                hand_regions.append(hand_region)
        hand_regions.sort(key=lambda x: x.confidence, reverse=True)
        return hand_regions[: self.config.max_num_hands]
    def cleanup(self):
        self.is_initialized = False
class ContourBasedHandSegmentation(BaseHandSegmentation):
    def __init__(self, config: SegmentationConfig):
        super().__init__(config)
    def initialize(self) -> bool:
        self.is_initialized = True
        return True
    def process_frame(self, frame: np.ndarray) -> List[HandRegion]:
        if not self.is_initialized:
            return []
        hand_regions = []
        height, width = frame.shape[:2]
        grey = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        blurred = cv2.GaussianBlur(grey, (5, 5), 0)
        thresh = cv2.adaptiveThreshold(
            blurred, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2
        )
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))
        thresh = cv2.morphologyEx(thresh, cv2.MORPH_CLOSE, kernel)
        thresh = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, kernel)
        contours, _ = cv2.findContours(
            thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE
        )
        for contour in contours:
            area = cv2.contourArea(contour)
            if self.config.contour_min_area <= area <= self.config.contour_max_area:
                perimeter = cv2.arcLength(contour, True)
                if perimeter == 0:
                    continue
                compactness = 4 * np.pi * area / (perimeter * perimeter)
                if 0.1 <= compactness <= 0.8:
                    x, y, w, h = cv2.boundingRect(contour)
                    x = max(0, x - self.config.crop_padding)
                    y = max(0, y - self.config.crop_padding)
                    w = min(width - x, w + 2 * self.config.crop_padding)
                    h = min(height - y, h + 2 * self.config.crop_padding)
                    bbox = x, y, w, h
                    mask = None
                    if self.config.output_masks:
                        mask = np.zeros((height, width), dtype=np.uint8)
                        cv2.fillPoly(mask, [contour], 255)
                    area_score = min(1.0, area / self.config.contour_max_area)
                    shape_score = compactness
                    confidence = (area_score + shape_score) / 2.0
                    hand_region = HandRegion(
                        bbox=bbox,
                        mask=mask,
                        landmarks=None,
                        confidence=confidence,
                        hand_label="Unknown",
                    )
                    hand_regions.append(hand_region)
        hand_regions.sort(key=lambda x: x.confidence, reverse=True)
        return hand_regions[: self.config.max_num_hands]
    def cleanup(self):
        self.is_initialized = False